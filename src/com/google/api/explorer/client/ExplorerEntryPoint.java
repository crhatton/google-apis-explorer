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

import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.analytics.AnalyticsManager.AnalyticsEvent;
import com.google.api.explorer.client.analytics.AnalyticsManagerImpl;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.ExplorerConfig;
import com.google.api.explorer.client.base.ServiceLoader;
import com.google.api.explorer.client.embedded.EmbeddedResources;
import com.google.api.explorer.client.history.HistoryCache;
import com.google.api.explorer.client.history.JsonPrettifier;
import com.google.api.explorer.client.routing.HistoryWrapper;
import com.google.api.explorer.client.routing.HistoryWrapperImpl;
import com.google.api.explorer.client.routing.URLManipulator;
import com.google.api.explorer.client.routing.handler.HistoryManager;
import com.google.api.explorer.client.search.DirectoryIndexingStrategy;
import com.google.api.explorer.client.search.DiscoveryFullTextIndexingStrategy;
import com.google.api.explorer.client.search.HistoryItemIndexingStrategy;
import com.google.api.explorer.client.search.KeywordCompletionSuggestOracle;
import com.google.api.explorer.client.search.SearchManager;
import com.google.api.explorer.client.search.SearchResultIndex;
import com.google.common.collect.ImmutableList;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry Point for Explorer module.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ExplorerEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // Make sure that CSS gets applied.
    Resources.INSTANCE.style().ensureInjected();
    EmbeddedResources.INSTANCE.style().ensureInjected();

    // Set up static resources.
    JsonPrettifier.setResources(Resources.INSTANCE);

    // Set the API key and application name to use for calls from the Explorer.
    Config.setApiKey(ExplorerConfig.API_KEY);

    // If the URL specifies a base URL, use it.
    // If it specifies an API key, use it as well. If no key is specified (and
    // the base URL is), then unset the key -- use no API key. We only want to
    // use the specified key when the base URL is also set -- for requests to
    // the default backend (googleapis.com, defined in Config.java) we always
    // want to use the standard API key.
    String baseUrl = Window.Location.getParameter("base");
    String key = Window.Location.getParameter("key");
    if (baseUrl != null) {
      Config.setBaseUrl(baseUrl);
      Config.setApiKey(key == null ? "" : key);
    }

    // If the user supplied a trace parameter, keep track of it and append it to requests.
    String trace = Window.Location.getParameter("trace");
    if (trace != null) {
      Config.setTraceParameter(trace);
    }


    // Create and display the view.
    AnalyticsManager analytics = new AnalyticsManagerImpl();
    ViewAndHistory fullViewAndHistory = createFullView(analytics);
    RootLayoutPanel.get().add(fullViewAndHistory.fullView);

    // Notify analytics that we have loaded the explorer.
    analytics.trackEventWithValue(AnalyticsEvent.LOAD_EXPLORER, "Full");

    // Load the requested URL.
    fullViewAndHistory.historyManager.processUrl(History.getToken());
  }

  /**
   * Create the view that will be bound to the entire screen as well as the dependencies.
   */
  private ViewAndHistory createFullView(AnalyticsManager analytics) {
    // Dependencies for the UI
    AuthManager authManager = new AuthManager();
    HistoryCache historyCache = new HistoryCache();
    ServiceLoader serviceLoader = new ServiceLoader(ApiServiceFactory.INSTANCE);
    SearchResultIndex searchIndex = new SearchResultIndex();

    // Set up the keyword completion suggestion oracle.
    KeywordCompletionSuggestOracle searchKeywords = new KeywordCompletionSuggestOracle();
    searchIndex.setKeywordCallback(searchKeywords);

    // Set up the URL routing and responder.
    HistoryWrapper wrapper = new HistoryWrapperImpl();
    URLManipulator manipulator = new URLManipulator(wrapper);
    HistoryManager historyManager = new HistoryManager(wrapper,
        manipulator,
        serviceLoader,
        historyCache,
        analytics,
        searchIndex);

    // Construct the UI and add it to the page.
    FullView fullView = new FullView(manipulator, authManager, analytics, searchKeywords);
    historyManager.delegate = fullView;

    // If this in compiled GWT, set up the search capability. If it is hosted mode, search
    // capability is too slow and will be left disabled.
    if (GWT.isScript()) {
      SearchManager searchManager = new SearchManager(serviceLoader,
          searchIndex,
          new DiscoveryFullTextIndexingStrategy(),
          new DirectoryIndexingStrategy(),
          new HistoryItemIndexingStrategy(),
          ImmutableList.of(fullView, historyManager));
      serviceLoader.delegate = searchManager;
      historyCache.observer = searchManager;
    }

    return new ViewAndHistory(historyManager, fullView);
  }

  /**
   * Simple class to allow us to return the full view and the required history dependency.
   */
  private static class ViewAndHistory {
    public final HistoryManager historyManager;
    public final FullView fullView;

    /**
     * Create the instance binding the specified history manager and full view.
     */
    public ViewAndHistory(HistoryManager historyManager, FullView fullView) {
      this.historyManager = historyManager;
      this.fullView = fullView;
    }
  }
}