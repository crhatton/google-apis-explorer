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
import com.google.api.explorer.client.base.ApiParameter.Type;
import com.google.api.explorer.client.base.http.TimeoutException;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequest;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequestBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a request to be made to call a method, with specified parameters.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiRequest {
  /**
   * Static holder class is needed to support JUnit-testing this class. Since
   * only the send() method requires the CrossDomainRequestBuilder, and it is
   * non-JUnit-compatible, all other methods can now be tested without GWT test
   * infrastructure.
   */
  private static class HttpRequestBuilderHolder {
    static final CrossDomainRequestBuilder REQUEST_BUILDER = new CrossDomainRequestBuilder();
  }

  /** Service that is called by this request. */
  public final ApiService service;

  /** Method that is called by this request. */
  public final ApiMethod method;

  /** The HTTP method which will be used in this request. */
  public final HttpMethod httpMethod;

  /**
   * Key-value mapping of parameter values specified by this request.
   *
   * <p>
   * Note that this map is a {@link Multimap}, meaning that multiple values can
   * be specified, and calling {@link ListMultimap#put(Object, Object)} will not
   * overwrite any existing values.
   * </p>
   *
   * <p>
   * If this request was constructed with an explicit request path (i.e., by
   * calling {@link #ApiRequest(String)}), then this map will be ignored.
   * </p>
   *
   * <p>
   * If this request was constructed with a {@link ApiService} and method
   * identifier (i.e., by calling {@link #ApiRequest(ApiService, String)}), then
   * this map may be pre-populated with default parameter values as defined by
   * the method's parameter's {@link ApiParameter#getDefault()}.
   * </p>
   */
  public final ListMultimap<String, String> paramValues = ArrayListMultimap.create(4, 1);

  /** Key-value mapping of headers to set in this request. */
  public final Map<String, String> headers = Maps.newHashMap();

  /** HTTP Body sent by this request, or {@code null} if no body is set. */
  public String body;

  /**
   * Request path as specified in the constructor (
   * {@link ApiRequest#ApiRequest(String)}), or {@code null} if this constructor
   * was not used, in which case the {@link #service} and {@link #method} will
   * be used to construct the request path.
   *
   * <p>
   * If the request path is explicitly set, the API key will be ignored.
   * </p>
   */
  public final String requestPath;

  /**
   * Whether or not requests should be validated in the client before being sent
   * to the server. By default, this is {@code true}.
   *
   * <p>
   * If this is {@code false}, validation will be skipped and the
   * {@link #validate()} will always succeed.
   * </p>
   */
  public boolean enableClientSideValidation = true;

  /**
   * Reference to the underlying HTTP request being made, so that it can be
   * canceled. This will be null until the request is sent.
   */
  private CrossDomainRequest innerRequest;

  /**
   * Whether or not to pass the API key when making this request. This is {@code
   * true} by default, unless the {@link #requestPath} is set.
   */
  public boolean useApiKey = true;

  /**
   * Constructs a {@link ApiRequest} that will make a request to the method with
   * the given identifier belonging to the given {@link ApiService}.
   *
   * @param service Service which will be called.
   * @param methodIdentifier Identifier of the method which will be called.
   */
  public ApiRequest(ApiService service, String methodIdentifier) {
    this.service = Preconditions.checkNotNull(service, "Service cannot be null");
    this.method = service.method(methodIdentifier);
    this.httpMethod = method.getHttpMethod();
    this.requestPath = null;
  }

  /**
   * Constructs a {@link ApiRequest} that will request the given path using the
   * HTTP GET method, relative to the base URL (i.e.,
   * {@link Config#getBaseUrl()}).
   *
   * <p>
   * Note that requests constructed in this way are not validated in the client,
   * and may be invalid requests. Additionally, any parameter values added to
   * {@link #paramValues} will be ignored.
   * </p>
   *
   * @param requestPath URL path to request.
   */
  public ApiRequest(String requestPath) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(requestPath), "Request URL cannot be null or empty");
    this.requestPath = requestPath;
    this.httpMethod = HttpMethod.GET;
    this.service = null;
    this.method = null;
    useApiKey = false;
  }

  /**
   * Performs validation of the parameter values given in {@link #paramValues}
   * against the requirements defined in the method's {@link ApiParameter} s.
   * This method is called immediately before sending a request when
   * {@link #send(AsyncCallback)} is called.
   *
   * <p>
   * If {@link #enableClientSideValidation} is {@code false}, or if the request
   * was constructed with an explicit request path (i.e., by using
   * {@link #ApiRequest(String)}, then this method will always succeed.
   * </p>
   */
  public void validate() {
    if (!enableClientSideValidation || method == null) {
      return;
    }

    Map<String, ApiParameter> paramSpecs = method.getParameters();

    for (Map.Entry<String, ApiParameter> entry : paramSpecs.entrySet()) {
      String paramName = entry.getKey();
      // Check that required parameters have a parameter value specified.
      if (entry.getValue().isRequired()) {
        Preconditions.checkArgument(
            paramValues.containsKey(paramName) && !paramValues.get(paramName).isEmpty(),
            "[" + paramName + "] is required, and must be given a value.");
      }
    }

    for (Map.Entry<String, Collection<String>> entry : paramValues.asMap().entrySet()) {
      String paramName = entry.getKey();
      // Skip validation of extraneous parameter values.
      if (!paramSpecs.containsKey(paramName)) {
        continue;
      }

      ApiParameter paramSpec = paramSpecs.get(paramName);

      // Check that only repeated parameters are given multiple values.
      Preconditions.checkArgument(paramSpec.isRepeated() || paramValues.get(paramName).size() == 1,
          "[" + paramName + "] is not a repeated parameter, and cannot be given multiple values.");

      String pattern = paramSpec.getPattern();
      List<String> enumValues = paramSpec.getEnumValues();
      Type type = paramSpec.getType();

      Collection<String> values = entry.getValue();
      for (String value : values) {
        // Check that the value matches the pattern
        Preconditions.checkArgument(pattern == null || value.matches(pattern),
            "[" + paramName + "] does not match the required pattern: " + pattern);

        // Check that the value is one of the defined enum values
        Preconditions.checkArgument(enumValues == null || enumValues.contains(value),
            "[" + paramName + "] is not one of the defined valid values: " + enumValues);

        if (type != null) {
          // Check that values are the correct type, and within defined bounds.
          switch (type) {
            case STRING:
              // All values are valid strings
              break;

            case INTEGER:
              BigInteger intVal;
              try {
                intVal = new BigInteger(value);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "[" + paramName + "] is not a valid integer value.");
              }

              String intMinimum = paramSpec.getMinimum();
              if (intMinimum != null) {
                BigInteger min = new BigInteger(intMinimum);
                Preconditions.checkArgument(intVal.compareTo(min) >= 0,
                    "[" + paramName + "] is less than the allowable minimum: " + intMinimum);
              }
              String intMaximum = paramSpec.getMaximum();
              if (intMaximum != null) {
                BigInteger max = new BigInteger(intMaximum);
                Preconditions.checkArgument(intVal.compareTo(max) <= 0,
                    "[" + paramName + "] is greater than the allowable maximum: " + intMaximum);
              }
              break;

            case DECIMAL:
              BigDecimal decVal;
              try {
                decVal = new BigDecimal(value);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "[" + paramName + "] is not a valid decimal value.");
              }
              break;

            case BOOLEAN:
              Preconditions.checkArgument(value.equals("true") || value.equals("false"),
                  "[" + paramName + "] is not a valid boolean value.");
              break;
          }
        }
      }
    }
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
    validate();
    setHeaders();
    maybeSetApiKeyParameter();
    this.innerRequest = HttpRequestBuilderHolder.REQUEST_BUILDER.makeRequest(this, callback);
  }

  /**
   * Add the API key as a request parameter, if it has been set in
   * {@link Config#setApiKey(String)}).
   */
  @VisibleForTesting
  void maybeSetApiKeyParameter() {
    if (useApiKey && !Config.getApiKey().isEmpty()) {
      paramValues.put("key", Config.getApiKey());
    }
  }

  private void setHeaders() {
    headers.put("X-JavaScript-User-Agent", Config.getUserAgent());
  }

  /**
   * Returns the URL path that will be requested when
   * {@link #send(AsyncCallback)} is called, including all given values added to
   * {@link #paramValues}.
   *
   * <p>
   * If this request was constructed given a request path (i.e., using
   * {@link #ApiRequest(String)}), that path will be returned by this method. If
   * an API key has been specified using {@link Config#setApiKey(String)}, that
   * key will be appended.
   * </p>
   */
  public String getRequestPath() {
    // If the request path was explicitly set, we prepend the base URL path to
    // the request path given.
    if (requestPath != null) {
      return requestPath;
    }

    // If the request path was not set, we assemble it from the base URL path,
    // and the method's path.
    String pathUrl = method.getPath();
    Set<String> unusedParamKeys = new HashSet<String>(paramValues.keySet());

    StringBuilder sb = new StringBuilder(service.getBasePath());

    boolean addSlash = false;
    for (String section : pathUrl.split("/")) {
      if (addSlash) {
        sb.append("/");
      } else {
        addSlash = true;
      }

      if (section.startsWith("{")) {
        String paramKey = section.substring(1, section.length() - 1);
        if (!paramValues.containsKey(paramKey)) {
          if (enableClientSideValidation) {
            throw new IllegalArgumentException(
                "Error generating path URL: Missing parameter value [" + paramKey + "]");
          }
        } else {
          // TODO(jasonhall): Investigate how to support repeated path
          // parameters, or throw an error here if it's not supported.
          sb.append(paramValues.get(paramKey).get(0));
        }
        unusedParamKeys.remove(paramKey);
      } else {
        sb.append(section);
      }
    }

    String delim = pathUrl.contains("?") ? "&" : "?";
    for (String key : unusedParamKeys) {
      for (String value : paramValues.get(key)) {
        sb
            .append(delim)
            .append(URL.encodePathSegment(key))
            .append("=")
            .append(URL.encodePathSegment(value));
        delim = "&";
      }
    }
    return sb.toString();
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

}
