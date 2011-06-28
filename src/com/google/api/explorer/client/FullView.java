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

import com.google.api.explorer.client.auth.AuthView;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.history.HistoryPanel;
import com.google.api.explorer.client.method.MethodSelector;
import com.google.api.explorer.client.parameter.ParameterForm;
import com.google.api.explorer.client.service.ServiceSelector;
import com.google.api.explorer.client.version.VersionSelector;
import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * View of the whole app.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class FullView extends Composite
    implements
    ServiceLoadedEvent.Handler,
    ServiceSelectedEvent.Handler,
    MethodSelectedEvent.Handler,
    ServiceDefinitionsLoadedEvent.Handler,
    VersionSelectedEvent.Handler {

  // Height of the method selector pane when not hidden. When it is hidden its
  // height will be set to 0. This matches the heigh specified in the .ui.xml
  // template.
  private static final int SELECTOR_PANEL_SIZE = 306;

  private static FullViewUiBinder uiBinder = GWT.create(FullViewUiBinder.class);

  interface FullViewUiBinder extends UiBinder<Widget, FullView> {
  }

  @UiField DockLayoutPanel dockLayoutPanel;
  @UiField Image logo;
  @UiField HTMLPanel selectorPanel;
  @UiField(provided = true) AuthView authView;
  @UiField TableCellElement serviceColumn;
  @UiField(provided = true) ServiceSelector serviceSelector;
  @UiField TableCellElement versionColumn;
  @UiField(provided = true) VersionSelector versionSelector;
  @UiField TableCellElement methodColumn;
  @UiField(provided = true) MethodSelector methodSelector;
  @UiField(provided = true) ParameterForm parameterForm;
  @UiField Label showHide;
  @UiField HTMLPanel footer;

  // Whether or not the method selection panel is currently hidden.
  private boolean hidden = false;

  // Duration (in ms) of animation to show/hide the method selection panel.
  private static final int ANIMATION_DURATION = 200;

  /** {@link Animation} used to show the method selection panel. */
  private final Animation showMethodPanelAnimation = new Animation() {
    @Override
    protected void onUpdate(double progress) {
      dockLayoutPanel.setWidgetSize(selectorPanel, SELECTOR_PANEL_SIZE * progress);
      hidden = false;
    }
  };

  /** {@link Animation} used to hide the method selection panel. */
  private final Animation hideMethodPanelAnimation = new Animation() {
    @Override
    protected void onUpdate(double progress) {
      dockLayoutPanel.setWidgetSize(
          selectorPanel, SELECTOR_PANEL_SIZE - SELECTOR_PANEL_SIZE * progress);
      hidden = true;
    }
  };

  public FullView(EventBus eventBus, AppState appState, AuthManager authManager) {
    Scheduler scheduler = Scheduler.get();
    this.authView = new AuthView(eventBus, authManager);
    this.serviceSelector = new ServiceSelector(eventBus);
    this.versionSelector = new VersionSelector(eventBus, scheduler);
    this.methodSelector = new MethodSelector(eventBus, appState, scheduler);
    this.parameterForm = new ParameterForm(eventBus, appState, authManager);

    initWidget(uiBinder.createAndBindUi(this));

    dockLayoutPanel.add(new HistoryPanel(eventBus, appState));

    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
    eventBus.addHandler(ServiceDefinitionsLoadedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
  }

  /** Go back to the "home" state of the app when the logo is clicked. */
  @UiHandler("logo")
  void clickLogo(ClickEvent event) {
    Window.Location.assign("");
  }

  @UiHandler("showHide")
  void showHide(ClickEvent event) {
    if (hidden) {
      showMethodPanelAnimation.run(ANIMATION_DURATION);
    } else {
      hideMethodPanelAnimation.run(ANIMATION_DURATION);
    }
    hidden = !hidden;
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceDefinitionsLoaded(ServiceDefinitionsLoadedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(false);
    methodSelector.setVisible(false);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceSelected(ServiceSelectedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(false);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceLoaded(ServiceLoadedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(false);
  }

  @Override
  public void onMethodSelected(MethodSelectedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(true);
  }
}
