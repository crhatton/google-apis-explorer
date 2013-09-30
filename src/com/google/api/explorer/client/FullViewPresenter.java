/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.context.ExplorerContext;
import com.google.api.explorer.client.history.HistoryItem;
import com.google.api.explorer.client.routing.URLManipulator;
import com.google.api.explorer.client.search.SearchManager.SearchReadyCallback;

/**
 * Presenter which handles events from a full view display.
 *
 */
public class FullViewPresenter implements SearchReadyCallback {
  private final URLManipulator urlManipulator;
  private final Display display;

  private ExplorerContext currentContext;

  /** Different types of navigation items under which navigation can be rooted. */
  enum NavigationItem {
    PREFERRED_SERVICES,
    REQUEST_HISTORY,
    ALL_VERSIONS,
    NONE,
  }

  /**
   * Create an instance.
   *
   * @param urlManipulator Used to modify the url fragment in response to user navigation.
   * @param display Display instance which this presenter controls.
   */
  public FullViewPresenter(URLManipulator urlManipulator, Display display) {
    this.urlManipulator = urlManipulator;
    this.display = display;
  }

  /**
   * Set the current context object for this presenter.
   */
  public void setContext(ExplorerContext context) {
    currentContext = context;
  }

  /**
   * Interface by which we talk to the display.
   */
  interface Display {
    /**
     * Used to disable the search loading indicator when search has finished downloading and
     * indexing documents.
     */
    void hideSearchLoadingIndicator();
  }

  /**
   * The user clicked a root navigation item on the display.
   */
  void clickNavigationItem(NavigationItem navItem) {
    switch(navItem) {
      case ALL_VERSIONS:
        urlManipulator.selectAllServices();
        break;
      case PREFERRED_SERVICES:
        urlManipulator.selectPreferredServices();
        break;
      case REQUEST_HISTORY:
        urlManipulator.showAllHistory();
        break;
      default:
        // This should never happen.
        throw new IllegalStateException("Invalid navigation item selection: " + navItem);
    }
  }

  /**
   * The user clicked on the product logo on the display.
   */
  public void handleClickLogo() {
    urlManipulator.selectPreferredServices();
  }

  /**
   * The user clicked on a specific service in order to navigate into it.
   */
  public void handleClickService(ServiceDefinition service) {
    urlManipulator.setVersion(service.getName(), service.getVersion());
  }

  /**
   * The user clicked on a specific history item to display.
   *
   * @param prefix URL prefix to use when appending the item number. Is indicative of where the
   *        history item was when clicked (e.g. search or list of all history).
   * @param item Specific history item which was clicked.
   */
  public void handleClickHistoryItem(String prefix, HistoryItem item) {
    urlManipulator.setHistoryItem(prefix, item.getKey());
  }

  /**
   * The user clicked on a specific method to display.
   *
   * @param prefix URL prefix to use when appending the method identifier. It is indicative of where
   *        the method item was when clicked (e.g. search or list of a service).
   * @param method Specific method item which was clicked.
   */
  public void handleClickMethod(String prefix, ApiMethod method) {
    urlManipulator.setMethod(prefix, method.getId());
  }

  /**
   * The user clicked the back button.
   */
  public void handleClickBack() {
    urlManipulator.setUrl(currentContext.getParentUrl());
  }

  /**
   * The user clicked the search button or otherwise indicated that they wish to perform a search.
   */
  public void handleSearch(String searchString) {
    urlManipulator.search(searchString);
  }

  @Override
  public void searchReady() {
    display.hideSearchLoadingIndicator();
  }
}
