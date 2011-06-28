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
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.DefaultAsyncCallback;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;

/**
 * Utility class to encapsulate logic of loading services.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceLoader implements VersionSelectedEvent.Handler {

  private final EventBus eventBus;
  private final Scheduler scheduler;
  private final ApiServiceFactory googleApi;

  @VisibleForTesting
  final HashBasedTable<String, String, ApiService> cache = HashBasedTable.create();

  public ServiceLoader(EventBus eventBus, Scheduler scheduler, ApiServiceFactory googleApi) {
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    this.eventBus = eventBus;
    this.scheduler = scheduler;
    this.googleApi = googleApi;
  }

  @Override
  public void onVersionSelected(final VersionSelectedEvent event) {
    if (event.serviceName != null && event.versionName != null) {
      if (cache.contains(event.serviceName, event.versionName)) {
        // This is done inside a deferred command because EventBus gets confused
        // when an event is fired while handling an event. This means that this
        // logic cannot be tested by JUnit tests, and will need to be tested by
        // a GWT test.
        scheduler.scheduleDeferred(new ScheduledCommand() {
          @Override
          public void execute() {
            // If this service/version has already been loaded from Discovery,
            // it will be cached. If it's found in the cache, fire a
            // ServiceLoadedEvent.
            ApiService service = cache.get(event.serviceName, event.versionName);
            fireEvents(event, service);
          }
        });
      } else {
        // If the service/version has not been cached, load it, cache it, and
        // send the ServiceLoadedEvent when it's done.
        googleApi.create(
            event.serviceName, event.versionName, new DefaultAsyncCallback<ApiService>() {
              public void onSuccess(ApiService service) {
                cache.put(event.serviceName, event.versionName, service);
                fireEvents(event, service);
              }
            });
      }
    }
  }

  /**
   * Fires a {@link ServiceLoadedEvent} given the service that was loaded (or
   * found in the cache, and may fire a {@link MethodSelectedEvent} if the given
   * {@link VersionSelectedEvent} specifies a method to select.
   */
  private void fireEvents(VersionSelectedEvent event, ApiService service) {
    eventBus.fireEvent(new ServiceLoadedEvent(service, event.params));

    // If the VersionSelectedEvent specified a method to pre-select, select it. The only time we
    // don't want to do this is when the service also only contains one method, since
    // MethodSelectorPresenter will auto-select that method on its own, and we don't want to bother
    // double-selecting it.
    if (event.methodName != null && service.allMethods().size() > 1) {
      eventBus.fireEvent(new MethodSelectedEvent(
          event.methodName, service.method(event.methodName), event.params));
    }
  }
}
