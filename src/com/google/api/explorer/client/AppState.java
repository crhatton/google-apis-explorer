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

import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventBus;

/**
 * Stores app-wide state information, such as whether the view should be in a
 * locked state.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AppState implements ServiceLoadedEvent.Handler, MethodSelectedEvent.Handler {

  private ApiService currentService;
  private String currentMethodIdentifier;

  public AppState(EventBus eventBus) {
    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
  }

  @Override
  public void onServiceLoaded(ServiceLoadedEvent event) {
    this.currentService = event.service;
  }

  @Override
  public void onMethodSelected(MethodSelectedEvent event) {
    this.currentMethodIdentifier = event.methodIdentifier;
  }

  public ApiService getCurrentService() {
    return Preconditions.checkNotNull(currentService);
  }

  public String getCurrentMethodIdentifier() {
    return Preconditions.checkNotNull(currentMethodIdentifier);
  }
}
