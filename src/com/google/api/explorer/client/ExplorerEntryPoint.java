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

package com.google.api.explorer.client;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.DefaultAsyncCallback;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry Point for Explorer module.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ExplorerEntryPoint implements EntryPoint {

  /** API key assigned to the API Explorer to be used when making requests. */
  public static final String API_KEY = "AIzaSyBrBMkC4y1E3YSNU8veQ5oo1tZ5ijYAiaE";

  /** The name of this application. */
  public static final String APP_NAME = "Google APIs Explorer";

  /** Path to the Directory listing all APIs. */
  private static final String DIRECTORY_REQUEST_PATH =
      "/discovery/" + ApiServiceFactory.DISCOVERY_VERSION + "/apis";

  @Override
  public void onModuleLoad() {
    // Make sure that CSS gets applied.
    Resources.INSTANCE.style().ensureInjected();


    // Set the API key and application name to use for calls from the Explorer.
    Config.setApiKey(API_KEY);
    Config.setApplicationName(APP_NAME);

    // Dependencies for the UI
    final EventBus eventBus = new SimpleEventBus();
    AppState appState = new AppState(eventBus);
    AuthManager authManager = new AuthManager(eventBus, appState);

    // These listen for events on the event bus.
    HistoryManager historyManager =
        new HistoryManager(eventBus, appState, new HistoryWrapperImpl());
    ServiceLoader serviceLoader =
        new ServiceLoader(eventBus, Scheduler.get(), ApiServiceFactory.INSTANCE);
    AnalyticsManager analyticsManager = new AnalyticsManager(eventBus);

    // Construct the UI and add it to the page.
    RootLayoutPanel.get().add(new FullView(eventBus, appState, authManager));

    // Request the list of APIs to display from the Directory API.
    ApiRequest request = new ApiRequest(DIRECTORY_REQUEST_PATH);
    request.send(new DefaultAsyncCallback<ApiResponse>() {
      @Override
      public void onSuccess(ApiResponse response) {
        ApiDirectory directory = ApiDirectory.Helper.fromString(response.body);
        eventBus.fireEvent(new ServiceDefinitionsLoadedEvent(directory.getItems()));

        // Fire initial history event now that services are loaded.
        if (!History.getToken().isEmpty()) {
          History.fireCurrentHistoryState();
        }
      }
    });
  }
}
