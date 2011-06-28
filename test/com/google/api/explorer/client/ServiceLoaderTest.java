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

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gwt.core.client.testing.StubScheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link ServiceLoader}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceLoaderTest extends TestCase {

  private EventBus eventBus;
  private StubScheduler scheduler;
  private MockGoogleApi googleApi;
  private ServiceLoader loader;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    scheduler = new StubScheduler();
    googleApi = new MockGoogleApi();
    loader = new ServiceLoader(eventBus, scheduler, googleApi);
  }

  /**
   * When a {@link VersionSelectedEvent} fires, a {@link ServiceLoadedEvent}
   * fires with the loaded service.
   */
  public void testVersionSelected() {
    googleApi.service = EasyMock.createControl().createMock(ApiService.class);

    ServiceLoadedEvent.Handler serviceLoadedHandler =
        EasyMock.createControl().createMock(ServiceLoadedEvent.Handler.class);
    eventBus.addHandler(ServiceLoadedEvent.TYPE, serviceLoadedHandler);
    serviceLoadedHandler.onServiceLoaded(new ServiceLoadedEvent(googleApi.service));
    EasyMock.replay(serviceLoadedHandler);

    eventBus.fireEvent(new VersionSelectedEvent("service", "version"));
    EasyMock.verify(serviceLoadedHandler);
  }

  /**
   * When a {@link VersionSelectedEvent} fires and the service/version it
   * contains is already in the cache, a deferred command will result in a
   * {@link ServiceLoadedEvent} without calling to discovery.
   */
  public void testVersionSelected_cached() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);

    // Populate the cache with the service we'll request later.
    loader.cache.put("service", "version", service);

    ServiceLoadedEvent.Handler serviceLoadedHandler =
        EasyMock.createControl().createMock(ServiceLoadedEvent.Handler.class);
    eventBus.addHandler(ServiceLoadedEvent.TYPE, serviceLoadedHandler);
    serviceLoadedHandler.onServiceLoaded(new ServiceLoadedEvent(service));
    EasyMock.replay(serviceLoadedHandler);

    eventBus.fireEvent(new VersionSelectedEvent("service", "version"));

    // This results in a scheduled command being added to the queue. Execute
    // this command to fire the ServiceLoadedEvent.
    assertEquals(1, scheduler.getScheduledCommands().size());
    scheduler.getScheduledCommands().get(0).execute();
    EasyMock.verify(serviceLoadedHandler);
  }

  /**
   * When a {@link VersionSelectedEvent} fires with a method specified, a
   * {@link ServiceLoadedEvent} and a {@link MethodSelectedEvent} fire.
   */
  public void testVersionSelected_withMethod() {
    googleApi.service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(googleApi.service.allMethods()).andReturn(
        ImmutableMap.<String, ApiMethod>of("method.one", method1, "method.two", method2));
    EasyMock.expect(googleApi.service.method("method.one")).andReturn(method1);

    MethodSelectedEvent.Handler methodSelectedHandler =
        EasyMock.createControl().createMock(MethodSelectedEvent.Handler.class);
    eventBus.addHandler(MethodSelectedEvent.TYPE, methodSelectedHandler);
    methodSelectedHandler.onMethodSelected(new MethodSelectedEvent("method.one", method1));
    EasyMock.replay(googleApi.service, methodSelectedHandler);

    eventBus.fireEvent(new VersionSelectedEvent(
        "service", "version", "method.one", ImmutableMultimap.<String, String>of()));
    EasyMock.verify(googleApi.service, methodSelectedHandler);
  }

  /**
   * Mock implementation of {@link ApiServiceFactory} which allows its returned service
   * to be set.
   */
  private static class MockGoogleApi extends ApiServiceFactory {
    private ApiService service;

    public MockGoogleApi() {
      super();
    }

    /**
     * Mocks out real API requests by immediately calling the callback with the
     * pre-defined service.
     */
    @Override
    public void create(
        String serviceName, String version, AsyncCallback<ApiService> callback) {
      callback.onSuccess(service);
    }
  }
}
