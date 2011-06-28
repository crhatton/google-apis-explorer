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

package com.google.api.explorer.client.parameter;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.ExplorerConfig;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.RequestFinishedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Presenter to handle to logic of the parameter form UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ParameterFormPresenter implements MethodSelectedEvent.Handler,
    ServiceSelectedEvent.Handler, VersionSelectedEvent.Handler {

  public interface Display {
    void setMethod(ApiMethod method, SortedMap<String, ApiParameter> sortedParams);

    Multimap<String, String> getParameterValues();

    void setParameterValues(Multimap<String, String> paramValues);

    void setVisible(boolean visible);

    String getBodyText();

    void setExecuting(boolean executing);
  }

  private final EventBus eventBus;
  private final AppState appState;
  private final AuthManager authManager;
  private final Display display;
  private ApiMethod method;

  public ParameterFormPresenter(
      EventBus eventBus, AppState appState, AuthManager authManager, Display display) {
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    this.eventBus = eventBus;
    this.appState = appState;
    this.authManager = authManager;
    this.display = display;
  }

  public void onMethodSelected(final MethodSelectedEvent event) {
    this.method = event.method;

    Map<String, ApiParameter> parameters = event.method.getParameters();
    SortedMap<String, ApiParameter> sortedParams;
    if (parameters != null) {
      sortedParams = ImmutableSortedMap.copyOf(
          parameters, new ParameterComparator(event.method.getParameterOrder()));
    } else {
      sortedParams = ImmutableSortedMap.of();
    }
    display.setMethod(event.method, sortedParams);

    if (!event.params.isEmpty()) {
      display.setParameterValues(event.params);
    }
  }

  @Override
  public void onServiceSelected(ServiceSelectedEvent event) {
    display.setVisible(false);
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    display.setVisible(false);
  }

  public void submit() {
    Preconditions.checkState(method != null);
    final ApiRequest req =
        new ApiRequest(appState.getCurrentService(), appState.getCurrentMethodIdentifier());

    // Disable client-side validation so that errors resulting from invalid
    // parameters can be displayed in the response.
    req.enableClientSideValidation = false;

    // If the user has declared a body, set it on the request.
    String body = display.getBodyText();
    if (!body.isEmpty()) {
      req.body = body;
      req.headers.put("Content-Type", "application/json");
    }

    Multimap<String, String> paramValues = display.getParameterValues();
    for (Map.Entry<String, String> entry : paramValues.entries()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      req.paramValues.put(entry.getKey(), entry.getValue());
    }

    // Enable pretty-printing of the response.
    req.paramValues.put("pp", "1");

    // Do not send the API key if the service is a public-only API.
    req.useApiKey =
        !ExplorerConfig.PUBLIC_ONLY_APIS.contains(appState.getCurrentService().getName());

    // Set the auth header if we have a token.
    String oauth2Token = authManager.getToken();
    if (oauth2Token != null) {
      req.headers.put("Authorization", "OAuth " + oauth2Token);
    }

    display.setExecuting(true);
    final long start = System.currentTimeMillis();
    req.send(new AsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        display.setExecuting(false);
        ParameterFormPresenter.this.eventBus.fireEvent(
            new RequestFinishedEvent(req, response, System.currentTimeMillis() - start));
      }

      @Override
      public void onFailure(Throwable caught) {
        display.setExecuting(false);
        // TODO(jasonhall): Better error handling when request fails (i.e.,
        // cannot communicate at all).
        Window.alert("An error occured: " + caught.getMessage());
      }
    });
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
  public static String generateDescriptionString(ApiParameter param) {
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
