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

package com.google.api.explorer.client.method;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;

import java.util.Map;

/**
 * Presenter to handle to logic of the method selection UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class MethodSelectorPresenter
    implements
    VersionSelectedEvent.Handler,
    ServiceLoadedEvent.Handler,
    MethodSelectedEvent.Handler,
    ServiceSelectedEvent.Handler {

  interface Display {
    void setMethods(Map<String, ApiMethod> methods);
    void selectMethod(String methodName);
    void setLoading(boolean loading);
  }

  private final EventBus eventBus;
  private final AppState appState;
  private final Scheduler scheduler;
  private final Display display;

  MethodSelectorPresenter(
      EventBus eventBus, AppState appState, Scheduler scheduler, Display display) {
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    this.eventBus = eventBus;
    this.appState = appState;
    this.scheduler = scheduler;
    this.display = display;
  }

  @Override
  public void onServiceLoaded(final ServiceLoadedEvent event) {
    display.setMethods(event.service.allMethods());

    // If only one method is available, automatically select it. Use a
    // ScheduledCommand because the EventBus gets confused when you fire an
    // event while it's processing an event.
    if (event.service.allMethods().size() == 1) {
      scheduler.scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          Map.Entry<String, ApiMethod> entry =
              Iterables.getOnlyElement(event.service.allMethods().entrySet());
          eventBus.fireEvent(
              new MethodSelectedEvent(entry.getKey(), entry.getValue(), event.initialParams));
        }
      });
    }
  }

  @Override
  public void onServiceSelected(ServiceSelectedEvent event) {
    display.setMethods(Maps.<String, ApiMethod>newHashMap());
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    display.setLoading(true);
  }

  @Override
  public void onMethodSelected(MethodSelectedEvent event) {
    display.selectMethod(event.methodIdentifier);
  }

  public void selectMethod(String methodName) {
    eventBus.fireEvent(
        new MethodSelectedEvent(methodName, appState.getCurrentService().method(methodName)));
  }
}
