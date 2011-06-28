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
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
 * View to display and manage {@link HistoryItem}s for requests that have been
 * made.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class HistoryPanel extends Composite implements HistoryPanelPresenter.Display {

  private static HistoryPanelUiBinder uiBinder = GWT.create(HistoryPanelUiBinder.class);

  interface HistoryPanelUiBinder extends UiBinder<Widget, HistoryPanel> {
  }

  @UiField Label emptyLabel;
  @UiField ScrollPanel scrollPanel;
  @UiField VerticalPanel panel;

  private static final Set<HistoryItem> ITEMS = new HashSet<HistoryItem>();

  public HistoryPanel(EventBus eventBus, AppState appState) {
    initWidget(uiBinder.createAndBindUi(this));
    new HistoryPanelPresenter(eventBus, appState, this);
    JsonPrettifier.appState = appState;
  }

  @Override
  public void clear() {
    emptyLabel.setVisible(true);
    for (HistoryItem item : ITEMS) {
      item.clear();
    }
    ITEMS.clear();
    panel.clear();
    panel.setVisible(false);
  }

  @Override
  public int getItemCount() {
    return panel.getWidgetCount();
  }

  @Override
  public void removeItem(int index) {
    panel.remove(index);
  }

  @Override
  public void insertAtTop(HistoryItem item) {
    panel.setVisible(true);
    emptyLabel.setVisible(false);
    panel.insert(item, 0);
    scrollPanel.scrollToLeft();
    scrollPanel.scrollToTop();
    ITEMS.add(item);
  }
}
