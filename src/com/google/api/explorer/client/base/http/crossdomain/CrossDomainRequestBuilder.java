/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.api.explorer.client.base.http.crossdomain;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

/**
 * Builds requests to make to an cross-domain proxy iframe.
 *
 * <p>
 * This consists of adding a <script> tag to the page to load the JS library to
 * make cross-domain requests to the server.
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class CrossDomainRequestBuilder {

  private static final String JS_CLIENT_URL =
      "https://apis.google.com/js/client.js?onload=__apis_explorer_load_callback";

  private int timeoutMillis;
  private List<OutstandingRequest> outstandingRequests = Lists.newArrayList();

  private static native boolean isScriptLoaded() /*-{
    return !!$wnd.googleapis && !!$wnd.googleapis.newHttpRequest;
  }-*/;

  @VisibleForTesting
  protected static native void addLoadCallback(CrossDomainRequestBuilder builder) /*-{
    $wnd.__apis_explorer_load_callback = function() {
      builder.
        @com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequestBuilder::scriptFinishedLoading()();
    };
  }-*/;

  public void scriptFinishedLoading() {
    setBaseUrl();

    for (OutstandingRequest request : outstandingRequests) {
      doMakeRequest(request.request, request.xdr);
    }
  }

  public void setTimeoutMillis(int timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
  }

  public CrossDomainRequest makeRequest(
      final ApiRequest request, AsyncCallback<ApiResponse> callback) {
    final CrossDomainRequest xdr = new CrossDomainRequest(callback, timeoutMillis);

    /** Adds a script tag to the page to load the JS library used to make requests. */
    if (!isScriptLoaded()) {
      outstandingRequests.add(new OutstandingRequest(request, xdr));

      // If we are the only request waiting, it is our responsibility to load the library.
      if (outstandingRequests.size() == 1) {
        addLoadCallback(this);
        ScriptInjector.fromUrl(JS_CLIENT_URL)
            .setWindow(ScriptInjector.TOP_WINDOW)
            .setCallback(new Callback<Void, Exception>() {
              @Override
              public void onFailure(Exception e) {
                throw new RuntimeException(e);
              }

              @Override
              public void onSuccess(Void arg0) {
                // Intentionally blank, callback will be invoked automatically
              }
            }).inject();
      }
    } else {
      doMakeRequest(request, xdr);
    }
    return xdr;
  }

  private <T> void doMakeRequest(ApiRequest request, final CrossDomainRequest xdr) {
    JavaScriptObject jso = CrossDomainRequest.convertRequest(request);
    xdr.sendRequest(jso);
  }

  private static native void setBaseUrl() /*-{
    var proxy = @com.google.api.explorer.client.base.Config::baseUrl + '/static/proxy.html';
    $wnd.gapi.config.update('googleapis.config/proxy', proxy);
  }-*/;

  private static class OutstandingRequest {

    public final ApiRequest request;
    public final CrossDomainRequest xdr;

    public OutstandingRequest(ApiRequest request, CrossDomainRequest xdr) {
      this.request = request;
      this.xdr = xdr;
    }
  }
}
