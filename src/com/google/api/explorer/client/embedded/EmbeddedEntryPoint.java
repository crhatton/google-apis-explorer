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

import com.google.api.explorer.client.AnalyticsManager;
import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.ExplorerEntryPoint;
import com.google.api.explorer.client.HistoryManager;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.ServiceLoader;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.event.AuthGrantedEvent;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.gwt.oauth2.client.Auth;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
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

  static EventBus eventBus;
  static AppState appState;

  @Override
  public void onModuleLoad() {
    // Make sure that CSS gets applied.
    Resources.INSTANCE.style().ensureInjected();

    RootPanel root = RootPanel.get(EMBEDDING_ID);

    // Try to get the root div, and service/version/method to load. If any are
    // not present, throw an error and give up.
    if (root == null) {
      throw new IllegalStateException("Could not find a suitable drop-in div.");
    }
    final Element rootElement = root.getElement();
    String serviceName = rootElement.getAttribute(SERVICE_ATTR);
    String versionName = rootElement.getAttribute(VERSION_ATTR);
    final String methodName = rootElement.getAttribute(METHOD_ATTR);
    String token = rootElement.getAttribute(PARAMS_ATTR);
    final Multimap<String, String> params = HistoryManager.parseParams(token);
    if (serviceName.isEmpty() || versionName.isEmpty() || methodName.isEmpty()) {
      throw new IllegalStateException("Could not determine service, version and method to load.");
    }

    // Set the OAuth2 popup window URL if one is given in the embedding div.
    String authPopup = rootElement.getAttribute(AUTH_POPUP_ATTR);
    if (!authPopup.isEmpty()) {
      Auth.get().setOAuthWindowUrl(authPopup);
    }


    // Set the API key and application name to use for calls from the Explorer.
    Config.setApiKey(ExplorerEntryPoint.API_KEY);
    Config.setApplicationName(ExplorerEntryPoint.APP_NAME + " (embedded)");

    // Dependencies for the UI
    eventBus = new SimpleEventBus();
    appState = new AppState(eventBus);
    AuthManager authManager = new AuthManager(eventBus, appState);

    // These listen for events on the event bus.
    ServiceLoader serviceLoader =
        new ServiceLoader(eventBus, Scheduler.get(), ApiServiceFactory.INSTANCE);
    AnalyticsManager analyticsManager = new AnalyticsManager(eventBus);

    // Construct the UI and add it to the page.
    root.add(new EmbeddedView(eventBus, appState, authManager));

    // Set up a handler to respond to service-loaded events.
    eventBus.addHandler(ServiceLoadedEvent.TYPE, new ServiceLoadedEvent.Handler() {
      @Override
      public void onServiceLoaded(ServiceLoadedEvent event) {
        eventBus.fireEvent(
            new MethodSelectedEvent(methodName, event.service.method(methodName), params));

        String authToken = rootElement.getAttribute("data-oauth2token");
        if (!authToken.isEmpty()) {
          eventBus.fireEvent(new AuthGrantedEvent(event.service, authToken));
        }
      }
    });

    // Fire an event selecting the correct service/version. ServiceLoader will
    // see this event and load the service, which will fire an event to call the
    // above handler, selecting the method.
    eventBus.fireEvent(new VersionSelectedEvent(serviceName, versionName));

    exportUpdate();
  }

  /**
   * Exports a globally-scoped JS function named 'updateExplorer' that the
   * outer page can call to update the embedded UI.
   */
  private final native void exportUpdate() /*-{
    $wnd.updateExplorer = $entry(function(method, token) {
      @com.google.api.explorer.client.embedded.EmbeddedEntryPoint::update(*)(method, token || "");
    });
  }-*/;

  @SuppressWarnings("unused") // Used in JSNI
  private static void update(String methodName, String token) {
    ApiMethod method = appState.getCurrentService().method(methodName);
    Multimap<String, String> params = HistoryManager.parseParams(token);
    eventBus.fireEvent(new MethodSelectedEvent(methodName, method, params));
  }
}
