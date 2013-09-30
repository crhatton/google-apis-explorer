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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.AuthManager.AuthToken;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ExplorerConfig;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.rest.RestApiRequest;
import com.google.api.explorer.client.routing.UrlBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Presenter to handle to logic of the parameter form UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedParameterFormPresenter {

  /**
   * Display object that can be controlled by the presenter.
   */
  public interface Display {
    /**
     * Set the method that should be shown in the display.
     *
     * @param service Service which contains the method to be shown.
     * @param method Method object which should be shown.
     * @param sortedParams Parameters that should be shown in the parameter form, in their final
     *        order.
     * @param paramValues Pre-defined values for the parameters that should be pre-filled in the
     *        request.
     * @param bodyText Text that should be shown in the body editor of the form.
     */
    void setMethod(ApiService service, ApiMethod method, SortedMap<String, Schema> sortedParams,
        Multimap<String, String> paramValues, String bodyText);

    /**
     * Returns the values from the form which are filled in.
     */
    Multimap<String, String> getParameterValues();

    /**
     * Returns the text from the request body editor as a serialized string.
     */
    String getBodyText();

    /**
     * Set the executing status on the display, which should give the user some sort of visual
     * indication that the user should anticipate a result.
     */
    void setExecuting(boolean executing);
  }

  /**
   * Interface that should be invoked when the parameter form has completed a request.
   *
   */
  public interface RequestFinishedCallback {
    /**
     * Method which is invoked when a request is about to be executed.
     *
     * @param request Request object which is about to be executed.
     */
    public void starting(ApiRequest request);

    /**
     * Method which is invoked when a request has been completed.
     *
     * @param request Request object which was executed.
     * @param response Response object which was generated from the request result.
     * @param startTime Time at which the request was started.
     * @param endTime Time at which the request completed.
     */
    public void finished(ApiRequest request, ApiResponse response, long startTime, long endTime);
  }

  private final AuthManager authManager;
  private final Display display;
  private ApiMethod method;
  private ApiService service;
  private RequestFinishedCallback callback;

  public EmbeddedParameterFormPresenter(
      AuthManager authManager, Display display, RequestFinishedCallback callback) {

    this.authManager = authManager;
    this.display = display;
    this.callback = callback;
  }

  public void selectMethod(ApiService service, ApiMethod method, Multimap<String, String> params) {
    this.method = Preconditions.checkNotNull(method);
    this.service = Preconditions.checkNotNull(service);

    Map<String, Schema> parameters = method.getParameters();
    SortedMap<String, Schema> sortedParams;
    if (parameters != null) {
      sortedParams = ImmutableSortedMap.copyOf(
          parameters, new ParameterComparator(method.getParameterOrder()));
    } else {
      sortedParams = ImmutableSortedMap.of();
    }

    display.setMethod(service, method, sortedParams, params, getRequestBodyParam(params));
  }

  /**
   * Returns the request body specified by the "resource" key of the parameters block specified.
   */
  private String getRequestBodyParam(Multimap<String, String> params) {
    Collection<String> body = params.get(UrlBuilder.BODY_QUERY_PARAM_KEY);
    return body.isEmpty() ? null : Iterables.getLast(body);
  }

  public void submit() {
    Preconditions.checkState(method != null);
    final RestApiRequest req = new RestApiRequest(service, method);

    // If the user has declared a body, set it on the request.
    String body = display.getBodyText();
    if (!body.isEmpty()) {
      req.body = body;
      req.addHeader("Content-Type", "application/json");
    }

    Multimap<String, String> paramValues = display.getParameterValues();
    for (Map.Entry<String, String> entry : paramValues.entries()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      req.getParamValues().put(entry.getKey(), entry.getValue());
    }

    // Do not send the API key if the service is a public-only API.
    req.setUseApiKey(
        !ExplorerConfig.PUBLIC_ONLY_APIS.contains(service.getName()));

    // Set the auth header if we have a token.
    AuthToken oauth2Token = authManager.getToken(service);
    if (oauth2Token != null) {
      req.addHeader("Authorization", "Bearer " + oauth2Token.getAuthToken());
    }

    display.setExecuting(true);

    final long start = System.currentTimeMillis();
    req.send(new AsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        display.setExecuting(false);
        callback.finished(req, response, start, System.currentTimeMillis());
      }

      @Override
      public void onFailure(Throwable caught) {
        display.setExecuting(false);
        // TODO(jasonhall): Better error handling when request fails (i.e.,
        // cannot communicate at all).
        Window.alert("An error occured: " + caught.getMessage());
      }
    });

    // This has to be after the actual send so that the API key gets initialized properly.
    callback.starting(req);
  }

  /**
   * Comparator to sort parameter names. This checks the
   * {@link ApiMethod#getParameterOrder()} member for the explicit ordering,
   * then defaults to alphabetical order if neither are explicitly ordered.
   */
  @VisibleForTesting
  static class ParameterComparator implements Comparator<String> {
    private final List<String> parameterOrder;

    ParameterComparator(List<String> parameterOrder) {
      this.parameterOrder = parameterOrder == null ? ImmutableList.<String>of() : parameterOrder;
    }

    @Override
    public int compare(String o1, String o2) {
      int i1 = parameterOrder.indexOf(o1);
      int i2 = parameterOrder.indexOf(o2);

      if (i1 != -1) {
        if (i2 != -1) {
          // Both are explicitly ordered, use relative ordering position to
          // determine which goes first.
          return i1 - i2;
        } else {
          // Only o1 is explicitly ordered, it goes first.
          return -1;
        }
      }

      if (i2 != -1) {
        // Only o2 is explicitly ordered, it goes first.
        return 1;
      }

      // Neither are explicitly ordered. Compare alphabetically.
      return o1.compareTo(o2);
    }
  }

  /**
   * Generate a String description in the form of:
   * <ul>
   * <li>Description, if available</li>
   * <li>Open parenthesis, then lowercase type, e.g., "(string"</li>
   * <li>Minimum and maximum, if available and within bounds, in the form of one
   * of:
   * <ul>
   * <li>2 - 10</li>
   * <li>2+</li>
   * <li>max 10</li>
   * </ul>
   * </li>
   * <li>Close paranthesis</li>
   */
  public static String generateDescriptionString(Schema param) {
    StringBuilder sb = new StringBuilder();
    String description = param.getDescription();
    String minimum = param.getMinimum();
    String maximum = param.getMaximum();

    // Don't bother displaying "0-4294967295" and just display "0+"
    if (maximum != null && maximum.length() > 9) {
      maximum = null;
    }
    // Likewise, don't bother displaying "-4294867295-0" and just
    // display "max 0"
    if (minimum != null && minimum.length() > 10) {
      minimum = null;
    }

    if (description != null) {
      sb.append(description).append(' ');
    }
    sb.append('(').append(param.getType().name().toLowerCase());
    if (minimum != null || maximum != null) {
      sb.append(", ");
    }
    if (minimum != null) {
      if (maximum != null) {
        sb.append(minimum).append('-').append(maximum);
      } else {
        sb.append(minimum).append("+");
      }
    } else if (maximum != null) {
      sb.append("max ").append(maximum);
    }
    sb.append(')');
    return sb.toString();
  }
}
