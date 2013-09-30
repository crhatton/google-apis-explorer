/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.base;

import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiService.CallStyle;
import com.google.api.explorer.client.base.rest.RestApiRequest;
import com.google.api.explorer.client.base.rest.RestApiService;
import com.google.api.explorer.client.base.rpc.RpcApiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.io.IOException;
import java.util.Set;

/**
 * Utility class to create {@link ApiService}s based on calls to the Discovery
 * API.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiServiceFactory {

  /** Needed so that this class can be sub-classed for tests. */
  protected ApiServiceFactory() {
  }

  /** Singleton instance of {@link ApiServiceFactory}. */
  public static final ApiServiceFactory INSTANCE = new ApiServiceFactory();

  /** Discovery API version to use. */
  public static final String DISCOVERY_VERSION = "v1";

  /**
   * Generates a {@link ApiService} based on the results of a Discovery API
   * request, using the provided Discovery API version.
   *
   * @param serviceName name of the API service for which to create a Service.
   * @param version version of the API to use.
   * @param callStyle variant of the service to create.
   * @param callback to execute when the {@link ApiService} has been created.
   */
  public void createService(final String serviceName, final String version,
      final CallStyle callStyle, final AsyncCallback<ApiService> callback) {

    RestApiRequest request =
        new RestApiRequest(createDiscoveryPath(serviceName, version, callStyle));

    // If a Discovery Auth token is set, use it.
    if (Config.getDiscoveryAuthToken() != null) {
      request.addHeader("Authorization", "OAuth " + Config.getDiscoveryAuthToken());
    }

    request.send(new AsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        // Determine if we got a 3XX or 4XX response and call failure if so.
        int responseClass = response.getStatus() / 100;
        if (responseClass > 3) {
          callback.onFailure(new IOException("Unsuccessful response code from server: "
              + response.getStatus()));
        } else if (callStyle == CallStyle.REST) {
          callback.onSuccess(RestApiService.Helper.fromString(response.getBodyAsString()));
        } else if (callStyle == CallStyle.RPC) {
          callback.onSuccess(RpcApiService.Helper.fromString(response.getBodyAsString()));
        }
      }

      @Override
      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }
    });
  }

  /**
   * Generate the proper request and parse the response that will load the directory document from
   * the Discovery service.
   *
   * @param callback Callback to notify of success or failure.
   */
  public void loadApiDirectory(final AsyncCallback<Set<ServiceDefinition>> callback) {
    RestApiRequest request = new RestApiRequest(Config.DIRECTORY_REQUEST_PATH);
    request.send(new AsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        ApiDirectory directory = ApiDirectory.Helper.fromString(response.getBodyAsString());
        callback.onSuccess(directory.getItems());
      }

      @Override
      public void onFailure(Throwable cause) {
        callback.onFailure(cause);
      }
    });
  }

  @VisibleForTesting
  public static final String createDiscoveryPath(
      String serviceName, String version, CallStyle callStyle) {

    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(serviceName), "Service name cannot be null or empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "Version cannot be null or empty");
    return "/discovery/" + DISCOVERY_VERSION + "/apis/" + serviceName + "/" + version + "/"
        + callStyle.discoveryPathFragment;
  }
}
