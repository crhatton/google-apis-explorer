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

package com.google.api.explorer.client.history;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.event.RequestFinishedEvent;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.shared.EventBus;

/**
 * @author jasonhall@google.com (Jason Hall)
 */
public class HistoryPanelPresenter implements RequestFinishedEvent.Handler {

  /** Interface that History views must implement. */
  public interface Display {
    /** Clear the history view and return it to its original state. */
    void clear();

    /** Insert a {@link HistoryItem} at the top of the history stack. */
    void insertAtTop(HistoryItem item);

    /** @return the number of {@link HistoryItem}s currently being displayed. */
    public int getItemCount();

    /** Remove the item at the given index. */
    public void removeItem(int index);
  }

  static final int MAX_HISTORY = 30;

  private final AppState appState;
  private final Display display;

  private HistoryItem lastItem;

  public HistoryPanelPresenter(EventBus eventBus, AppState appState, Display display) {
    eventBus.addHandler(RequestFinishedEvent.TYPE, this);
    this.appState = appState;
    this.display = display;
  }

  @Override
  public void onRequestFinished(RequestFinishedEvent event) {
    // Building the "title" of the history item, which will identify the
    // service, version, and method called.
    String methodIdentifier =
        event.request.service.getName() + " » " + event.request.service.getVersion() + " » "
            + appState.getCurrentMethodIdentifier();

    HistoryItem item =
        new HistoryItem(methodIdentifier, event.timeMillis, event.request, event.response);

    String responseBody = event.response.body;
    
    if (JsonUtils.safeToEval(event.response.body)) {
      DynamicJso jso = JsonUtils.safeEval(event.response.body);
      if (jso.get("error") != null) {
        item.setErrorMessage(ErrorCase.forJsonString(responseBody).getErrorLabel());
      }
    }

    addItem(item);
  }

  private void addItem(HistoryItem newItem) {
    if (lastItem != null) {
      lastItem.collapse();
    }
    lastItem = newItem;
    display.insertAtTop(lastItem);

    if (display.getItemCount() > MAX_HISTORY) {
      display.removeItem(MAX_HISTORY);
    }
  }

  void clear() {
    display.clear();
    lastItem = null;
  }
}
