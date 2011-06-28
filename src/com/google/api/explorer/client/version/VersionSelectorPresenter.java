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

package com.google.api.explorer.client.version;

import com.google.api.explorer.client.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;

import java.util.List;

/**
 * Presenter to handle to logic of the version selectionUI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class VersionSelectorPresenter implements ServiceDefinitionsLoadedEvent.Handler,
    ServiceSelectedEvent.Handler, VersionSelectedEvent.Handler {

  interface Display {
    void setVersions(List<ServiceDefinition> versions);
    void selectVersion(String versionName);
  }

  private final EventBus eventBus;
  private final Scheduler scheduler;
  private final Display display;
  private HashBasedTable<String, String, ServiceDefinition> defs = HashBasedTable.create();
  private String serviceName;

  VersionSelectorPresenter(EventBus eventBus, Scheduler scheduler, Display display) {
    eventBus.addHandler(ServiceDefinitionsLoadedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    this.eventBus = eventBus;
    this.scheduler = scheduler;
    this.display = display;
  }

  @Override
  public void onServiceDefinitionsLoaded(ServiceDefinitionsLoadedEvent event) {
    defs.clear();
    for (ServiceDefinition def : event.definitions) {
      defs.put(def.getName(), def.getVersion(), def);
    }
  }

  @Override
  public void onServiceSelected(final ServiceSelectedEvent event) {
    this.serviceName = event.serviceName;
    Preconditions.checkArgument(defs.containsRow(event.serviceName));
    final List<ServiceDefinition> versions =
        Lists.newArrayList(defs.row(event.serviceName).values());
    display.setVersions(versions);

    // If only one version is available, automatically select it. Use a
    // ScheduledCommand because the EventBus gets confused when you fire an
    // event while it's processing an event.
    if (versions.size() == 1) {
      scheduler.scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          eventBus.fireEvent(
              new VersionSelectedEvent(event.serviceName, versions.get(0).getVersion()));
        }
      });
    }
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    this.serviceName = event.serviceName;
    Preconditions.checkArgument(defs.containsRow(event.serviceName));
    List<ServiceDefinition> versions = Lists.newArrayList(defs.row(event.serviceName).values());
    display.setVersions(versions);
    display.selectVersion(event.versionName);
  }

  public void selectVersion(String versionName) {
    Preconditions.checkState(serviceName != null);
    Preconditions.checkState(defs.contains(serviceName, versionName));
    eventBus.fireEvent(new VersionSelectedEvent(serviceName, versionName));
  }
}
