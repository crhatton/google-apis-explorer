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
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.method.MethodSelectorPresenter.Display;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.client.testing.StubScheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link MethodSelectorPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class MethodSelectorPresenterTest extends TestCase {

  private EventBus eventBus;
  private AppState appState;
  private StubScheduler scheduler;
  private Display display;
  private MethodSelectorPresenter presenter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    appState = EasyMock.createControl().createMock(AppState.class);
    display = EasyMock.createControl().createMock(Display.class);
    scheduler = new StubScheduler();
    presenter = new MethodSelectorPresenter(eventBus, appState, scheduler, display);
  }

  /**
   * When a ServiceLoadedEvent fires, it results in a call to setMethods() with
   * a list of method names.
   */
  public void testServiceLoaded() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2)).times(2);
    display.setMethods(ImmutableMap.of("method.one", method1, "method.two", method2));
    EasyMock.replay(service, display);

    eventBus.fireEvent(new ServiceLoadedEvent(service));
    EasyMock.verify(service, display);
  }

  /**
   * When a ServiceLoadedEvent fires with a service containing one method, it
   * results in a call to setMethods(), and a deferred command to select that
   * method.
   */
  public void testServiceLoaded_oneMethod() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(ImmutableMap.of("method.one", method1))
        .times(3);
    display.setMethods(ImmutableMap.of("method.one", method1));
    display.selectMethod("method.one");
    EasyMock.replay(service, display);

    eventBus.fireEvent(new ServiceLoadedEvent(service));

    // This results in a scheduled command being added to the queue. Execute
    // this command to call selectMethod()
    assertEquals(1, scheduler.getScheduledCommands().size());
    scheduler.getScheduledCommands().get(0).execute();
    EasyMock.verify(service, display);
  }

  /**
   * When a ServiceSelectedEvent fires, it results in an call to setMethods()
   * with an empty list of method names.
   */
  public void testServiceSelected() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2)).times(2);
    display.setMethods(ImmutableMap.of("method.one", method1, "method.two", method2));
    display.setMethods(ImmutableMap.<String, ApiMethod>of());
    EasyMock.replay(service, display);

    eventBus.fireEvent(new ServiceSelectedEvent("service"));
    eventBus.fireEvent(new ServiceLoadedEvent(service));
    EasyMock.verify(service, display);
  }

  /**
   * When a VersionSelectedEvent fires, it results in a call to
   * setLoading(true).
   */
  public void testVersionSelected() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2)).times(2);
    display.setMethods(ImmutableMap.of("method.one", method1, "method.two", method2));
    display.setLoading(true);
    EasyMock.replay(service, display);

    eventBus.fireEvent(new VersionSelectedEvent("service", "v1"));
    eventBus.fireEvent(new ServiceLoadedEvent(service));
    EasyMock.verify(service, display);
  }

  /**
   * When a MethodSelectedEvent fires, it results in a call to selectMethod()
   * with the selected method identifier.
   */
  public void testMethodSelected() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2)).times(2);
    display.setMethods(ImmutableMap.of("method.one", method1, "method.two", method2));
    display.selectMethod("method.one");
    EasyMock.replay(service, display);

    eventBus.fireEvent(new ServiceLoadedEvent(service));
    eventBus.fireEvent(new MethodSelectedEvent("method.one", method1));
    EasyMock.verify(service, display);
  }

  /** Selecting a method fires a MethodSelectedEvent. */
  public void testSelectMethod() {
    MethodSelectedEvent.Handler handler =
        EasyMock.createControl().createMock(MethodSelectedEvent.Handler.class);
    eventBus.addHandler(MethodSelectedEvent.TYPE, handler);
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(appState.getCurrentService()).andReturn(service);
    EasyMock.expect(service.method("method.one")).andReturn(method);

    handler.onMethodSelected(new MethodSelectedEvent("method.one", method));
    EasyMock.replay(appState, service, handler);

    presenter.selectMethod("method.one");
    EasyMock.verify(appState, service, handler);
  }
}
