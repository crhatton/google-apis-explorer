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

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.auth.AuthView;
import com.google.api.explorer.client.event.RequestFinishedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Full view of the embedded version of the Explorer.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedView extends Composite
    implements ServiceLoadedEvent.Handler, RequestFinishedEvent.Handler {

  private static EmbeddedViewUiBinder uiBinder = GWT.create(EmbeddedViewUiBinder.class);

  interface EmbeddedViewUiBinder extends UiBinder<Widget, EmbeddedView> {
  }

  @UiField(provided = true) AuthView authView;
  @UiField(provided = true) EmbeddedParameterForm parameterForm;
  @UiField(provided = true) EmbeddedHistoryPanel historyPanel;

  public EmbeddedView(EventBus eventBus, AppState appState, AuthManager authManager) {
    this.authView = new AuthView(eventBus, authManager);
    this.parameterForm = new EmbeddedParameterForm(eventBus, appState, authManager);
    this.historyPanel = new EmbeddedHistoryPanel(eventBus, appState);
    initWidget(uiBinder.createAndBindUi(this));

    authView.setVisible(false);
    historyPanel.setVisible(false);

    eventBus.addHandler(RequestFinishedEvent.TYPE, this);
    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
  }

  @Override
  public void onRequestFinished(RequestFinishedEvent event) {
    historyPanel.setVisible(true);
  }

  @Override
  public void onServiceLoaded(ServiceLoadedEvent event) {
    authView.setVisible(true);
  }

}
