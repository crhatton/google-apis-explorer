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

import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.http.TimeoutException;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequest;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequestBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Map;

/**
 * Base class for all ApiRequests, regardless of variant.
 *
 * @author jasonhall@google.com (Jason Hall)
 * @author moshenko@google.com (Jake Moshenko)
 */
public abstract class ApiRequest {
  /**
   * Reference to the underlying HTTP request being made, so that it can be
   * canceled. This will be null until the request is sent.
   */
  private CrossDomainRequest innerRequest;

  /**
   * Whether or not to pass the API key when making this request.
   */
  private boolean useApiKey = true;

  /**
   * Static holder class is needed to support JUnit-testing this class. Since
   * only the send() method requires the CrossDomainRequestBuilder, and it is
   * non-JUnit-compatible, all other methods can now be tested without GWT test
   * infrastructure.
   */
  private static class HttpRequestBuilderHolder {
    static final CrossDomainRequestBuilder REQUEST_BUILDER = new CrossDomainRequestBuilder();
  }

  /**
   * Set the timeout that all {@link ApiRequest}s should enforce. If the timeout
   * period ends before a response is received, a {@link TimeoutException} will
   * be raised.
   *
   * @param timeoutMillis length of time in milliseconds before a
   *        {@link TimeoutException} should be raised unless a response has been
   *        received.
   */
  public static void setTimeoutMillis(int timeoutMillis) {
    HttpRequestBuilderHolder.REQUEST_BUILDER.setTimeoutMillis(timeoutMillis);
  }

  /**
   * Send this request asynchronously.
   *
   * @param callback to execute when the response is received.
   *        {@link AsyncCallback#onSuccess(Object)} will be called when the
   *        response is received, or {@link AsyncCallback#onFailure(Throwable)}
   *        will be called if an error is encountered.
   */
  public void send(AsyncCallback<ApiResponse> callback) {
    setHeaders();
    maybeSetApiKeyParameter();
    maybeSetTraceParameter();
    this.innerRequest = HttpRequestBuilderHolder.REQUEST_BUILDER.makeRequest(this, callback);
  }

  /**
   * If this request has been sent using {@link #send(AsyncCallback)}, cancel
   * it. The callback will not be executed.
   */
  public void cancel() {
    if (innerRequest != null) {
      innerRequest.cancel();
    }
  }

  /**
   * Add the API key as a request parameter, if it has been set in
   * {@link Config#setApiKey(String)}).
   */
  @VisibleForTesting
  void maybeSetApiKeyParameter() {
    if (useApiKey && !Config.getApiKey().isEmpty()) {
      setApiKey(Config.getApiKey());
    }
  }

  /**
   * Add the trace parameter to requests when it has been specified by the user.
   */
  @VisibleForTesting
  void maybeSetTraceParameter() {
    String traceParameter = Config.getTraceParameter();
    if (Strings.emptyToNull(traceParameter) != null) {
      setTraceParameter(traceParameter);
    }
  }

  /**
   * Set whether or not to use the API key
   */
  public void setUseApiKey(boolean useApiKey) {
    this.useApiKey = useApiKey;
  }

  /**
   * Method that will set the default headers for the request.
   */
  @VisibleForTesting
  void setHeaders() {
    addHeader("X-JavaScript-User-Agent", ExplorerConfig.APP_NAME);
  }

  /**
   * Returns the URL path that will be requested when send is called.
   */
  public abstract String getRequestPath();

  /** Returns the HTTP method which will be used in this request. */
  public abstract HttpMethod getHttpMethod();

  /**
   * Return the HTTP Body sent by this request, or {@code null} if no body is
   * set.
   */
  public abstract String getRequestBody();

  /** Returns a key-value mapping of headers to set in this request. */
  public abstract Map<String, String> getHeaders();

  /** Returns the service that is called by this request. */
  public abstract ApiService getService();

  /** Returns the method that was invoked by this request. */
  public abstract ApiMethod getMethod();

  /** Returns a key-value mapping of parameter values specified by this request. */
  public abstract ListMultimap<String, String> getParamValues();

  /** Sets the API key that should be used for this request. */
  public abstract void setApiKey(String apiKey);

  /** Sets the trace parameter. */
  public abstract void setTraceParameter(String traceParameter);

  /** Fetches the API key currently set for this request*/
  public abstract String getApiKey();

  /** Adds a header to be sent with this request. */
  public abstract void addHeader(String headerName, String headerValue);
}
