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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
   * @param serviceName name of the API service for which to create a Service
   *        for.
   * @param version version of the API to use.
   * @param callback to execute when the {@link ApiService} has been created.
   */
  public void create(
      final String serviceName, final String version, final AsyncCallback<ApiService> callback) {
    ApiRequest request = new ApiRequest(createDiscoveryPath(serviceName, version));
    request.send(new AsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        callback.onSuccess(ApiService.Helper.fromString(response.body));
      }

      @Override
      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }
    });
  }

  @VisibleForTesting
  static final String createDiscoveryPath(String serviceName, String version) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(serviceName), "Service name cannot be null or empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "Version cannot be null or empty");
    return "/discovery/" + DISCOVERY_VERSION + "/apis/" + serviceName + "/" + version + "/rest";
  }
}
