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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

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

  private static final String JS_CLIENT_NAME = "ae_f85e3bd0744c7080861c3ae42085d071.js";
  private static final String JS_CLIENT_URL = "https://ssl.gstatic.com/gb/js/" + JS_CLIENT_NAME;

  private int timeoutMillis;

  private static native boolean scriptLoaded() /*-{
    return !!$wnd.googleapis && !!$wnd.googleapis.newHttpRequest;
  }-*/;

  public void setTimeoutMillis(int timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
  }

  public CrossDomainRequest makeRequest(
      final ApiRequest request, AsyncCallback<ApiResponse> callback) {
    final CrossDomainRequest xdr = new CrossDomainRequest(callback, timeoutMillis);

    /** Adds a script tag to the page to load the JS library used to make requests. */
    if (!scriptLoaded()) {
      ScriptElement script = Document.get().createScriptElement();
      script.setSrc(JS_CLIENT_URL);
      Document.get().getElementsByTagName("head").getItem(0).appendChild(script);

      new Timer() {
        @Override
        public void run() {
          if (scriptLoaded()) {
            doMakeRequest(request, xdr);
            cancel();
          }
        }
      }.scheduleRepeating(100);
    } else {
      doMakeRequest(request, xdr);
    }
    return xdr;
  }

  private <T> void doMakeRequest(ApiRequest request, final CrossDomainRequest xdr) {
    JavaScriptObject jso = CrossDomainRequest.convertRequest(request);
    xdr.sendRequest(jso);
  }

}
