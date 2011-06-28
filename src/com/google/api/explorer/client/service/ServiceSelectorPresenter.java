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

package com.google.api.explorer.client.service;

import com.google.api.explorer.client.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.gwt.event.shared.EventBus;

import java.util.Set;

/**
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceSelectorPresenter
    implements ServiceDefinitionsLoadedEvent.Handler, VersionSelectedEvent.Handler {

  interface Display {
    void setServices(Set<ServiceDefinition> services);
    void selectService(String serviceName);
    void setLoading(boolean loading);
  }

  private final EventBus eventBus;
  private final Display display;

  public ServiceSelectorPresenter(EventBus eventBus, Display display) {
    eventBus.addHandler(ServiceDefinitionsLoadedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    this.display = display;
    this.eventBus = eventBus;
  }

  void selectService(String serviceName) {
    eventBus.fireEvent(new ServiceSelectedEvent(serviceName));
  }

  @Override
  public void onServiceDefinitionsLoaded(ServiceDefinitionsLoadedEvent event) {
    display.setLoading(false);
    display.setServices(event.definitions);
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    display.selectService(event.serviceName);
  }
}
