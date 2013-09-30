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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.AnalyticsRequestFinishedCallback;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.analytics.AnalyticsManager.AnalyticsEvent;
import com.google.api.explorer.client.analytics.AnalyticsManagerImpl;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.ExplorerConfig;
import com.google.api.explorer.client.base.ServiceLoader;
import com.google.api.explorer.client.history.EmbeddedHistoryItemView;
import com.google.api.explorer.client.history.JsonPrettifier;
import com.google.api.explorer.client.routing.URLFragment;
import com.google.api.gwt.oauth2.client.Auth;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry Point for Embedded Explorer module.
 *
 * <p>
 * <b>Usage</b>: This assumes that the hosting page includes a <div> element
 * with data attributes specifying the service, version, and method to load (in
 * "data-service", "data-version" and "data-method" attributes, respectively).
 * If these are not given, an {@link IllegalStateException} is thrown.
 * </p>
 *
 * <p>
 * Additional method parameters can be specified with the "data-params"
 * attribute on the div, declaring the params in the form
 * "key1=val1&key2=val2&..."
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedEntryPoint implements EntryPoint {

  private static final String EMBEDDING_ID = "embedded-explorer";
  private static final String SERVICE_ATTR = "data-service";
  private static final String VERSION_ATTR = "data-version";
  private static final String METHOD_ATTR = "data-method";
  private static final String PARAMS_ATTR = "data-params";
  private static final String BASE_ATTR = "data-baseUrl";
  private static final String AUTH_POPUP_ATTR = "data-auth-popup";
  private static final boolean SHOW_AUTH = true;

  private RootPanel root;
  private ServiceLoader serviceLoader;
  private AuthManager authManager;
  private AnalyticsManager analytics;

  @Override
  public void onModuleLoad() {
    // Make sure that CSS gets injected.
    Resources.INSTANCE.style().ensureInjected();
    EmbeddedResources.INSTANCE.style().ensureInjected();

    root = RootPanel.get(EMBEDDING_ID);

    // Try to get the root div, and service/version/method to load. If any are
    // not present, throw an error and give up.
    if (root == null) {
      throw new IllegalStateException("Could not find a suitable drop-in div.");
    }

    final Element rootElement = root.getElement();
    String serviceName = rootElement.getAttribute(SERVICE_ATTR);
    String versionName = rootElement.getAttribute(VERSION_ATTR);
    String methodName = rootElement.getAttribute(METHOD_ATTR);
    String token = rootElement.getAttribute(PARAMS_ATTR);
    final Multimap<String, String> params = URLFragment.parseParams(token);
    if (serviceName.isEmpty() || versionName.isEmpty() || methodName.isEmpty()) {
      throw new IllegalStateException("Could not determine service, version and method to load.");
    }

    // Set the OAuth2 popup window URL if one is given in the embedding div.
    String authPopup = rootElement.getAttribute(AUTH_POPUP_ATTR);
    if (!authPopup.isEmpty()) {
      Auth.get().setOAuthWindowUrl(authPopup);
    }

    // Set the base URL if one is given in the embedding div.
    String base = rootElement.getAttribute(BASE_ATTR);
    if (base != null && !base.isEmpty()) {
      Config.setBaseUrl(base);
    }

    // Set up static resources.
    JsonPrettifier.setResources(Resources.INSTANCE);

    // Set the API key and application name to use for calls from the Explorer.
    Config.setApiKey(ExplorerConfig.API_KEY);

    // Dependencies for the UI
    authManager = new AuthManager();
    serviceLoader = new ServiceLoader(ApiServiceFactory.INSTANCE);
    analytics = new AnalyticsManagerImpl();

    analytics.trackEventWithValue(AnalyticsEvent.LOAD_EXPLORER, "Embedded");

    loadServiceMethod(serviceName, versionName, methodName, params);

    exportUpdate(this);
  }

  private void loadServiceMethod(String serviceName, String versionName, final String methodName,
      final Multimap<String, String> params) {
    serviceLoader.loadService(serviceName, versionName, new Callback<ApiService, String>() {
      @Override
      public void onSuccess(ApiService service) {
        ShowHistoryCallback callback = new ShowHistoryCallback(analytics);

        EmbeddedView view = new EmbeddedView(authManager,
            service,
            service.resolveMethod(methodName),
            params,
            callback,
            SHOW_AUTH,
            analytics);

        callback.localView = view;

        // Construct the UI and add it to the page.
        root.clear();
        root.add(view);
      }

      @Override
      public void onFailure(String reason) {
        analytics.trackEvent(AnalyticsEvent.LOAD_DISCOVERY_FAILURE);
      }
    });
  }

  private static class ShowHistoryCallback extends AnalyticsRequestFinishedCallback {
    public EmbeddedView localView;

    private ShowHistoryCallback(AnalyticsManager analytics) {
      super(analytics);
    }

    @Override
    public void finished(ApiRequest request, ApiResponse response, long startTime, long endTime) {
      super.finished(request, response, startTime, endTime);
      EmbeddedHistoryItemView historyItem = new EmbeddedHistoryItemView(request);
      historyItem.complete(response, endTime - startTime, JsonPrettifier.EXTERNAL_LINK_FACTORY);
      localView.showHistoryItem(historyItem);
    }
  }

  /**
   * Exports a globally-scoped JS function named 'updateExplorer' that the
   * outer page can call to update the embedded UI.
   */
  private final native void exportUpdate(EmbeddedEntryPoint that) /*-{
    $wnd.updateExplorer = $entry(function(service, version, method, token) {
      that.
        @com.google.api.explorer.client.embedded.EmbeddedEntryPoint::update(*)
        (service, version, method, token || "");
    });
  }-*/;

  @SuppressWarnings("unused") // Used in JSNI
  private void update(String serviceName, String version, String methodName, String token) {
    loadServiceMethod(serviceName, version, methodName, URLFragment.parseParams(token));
  }
}
