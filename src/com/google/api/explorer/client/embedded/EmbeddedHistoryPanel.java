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

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.history.HistoryItem;
import com.google.api.explorer.client.history.HistoryPanelPresenter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * View to display and manage {@link HistoryItem}s for requests that have been
 * made.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedHistoryPanel extends Composite implements HistoryPanelPresenter.Display {

  private static EmbeddedHistoryPanelUiBinder uiBinder =
      GWT.create(EmbeddedHistoryPanelUiBinder.class);

  interface EmbeddedHistoryPanelUiBinder extends UiBinder<Widget, EmbeddedHistoryPanel> {
  }

  @UiField SimplePanel panel;

  public EmbeddedHistoryPanel(EventBus eventBus, AppState appState) {
    initWidget(uiBinder.createAndBindUi(this));
    new HistoryPanelPresenter(eventBus, appState, this);
  }

  @Override
  public void clear() {
    panel.clear();
    panel.setVisible(false);
  }

  @Override
  public int getItemCount() {
    return 1;
  }

  @Override
  public void removeItem(int index) {
    // No-op.
  }

  @Override
  public void insertAtTop(HistoryItem item) {
    panel.clear();
    panel.setVisible(true);
    panel.add(item);
  }
}
