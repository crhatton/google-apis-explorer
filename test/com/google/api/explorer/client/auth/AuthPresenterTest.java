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

package com.google.api.explorer.client.auth;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.auth.AuthPresenter.Display;
import com.google.api.explorer.client.auth.AuthPresenter.Display.State;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.event.AuthGrantedEvent;
import com.google.api.explorer.client.event.AuthRequestedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link AuthPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthPresenterTest extends TestCase {

  private EventBus eventBus;
  private Display display;
  private AuthManager authManager;
  private AuthPresenter presenter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    display = EasyMock.createControl().createMock(Display.class);
    authManager = EasyMock.createControl().createMock(AuthManager.class);
    presenter = new AuthPresenter(eventBus, authManager, display);
  }

  /**
   * When a ServiceLoadedEvent fires, the Display becomes visible and displays
   * the user as un-authenticated by default.
   */
  public void testServiceLoaded_onlyPublic() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2));
    EasyMock.expect(service.getName()).andReturn("service");
    display.setState(State.ONLY_PUBLIC);
    EasyMock.replay(display);

    eventBus.fireEvent(new ServiceLoadedEvent(service));
    EasyMock.verify(display);
  }

  /** When an AuthGrantedEvent fires, the Display displays as authenticated. */
  public void testAuthGranted() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.getName()).andReturn("buzz");
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2));
    EasyMock.expect(service.getName()).andReturn("service");
    display.setState(State.ONLY_PUBLIC);
    display.setState(State.PRIVATE);
    EasyMock.replay(display);

    eventBus.fireEvent(new ServiceLoadedEvent(service));
    eventBus.fireEvent(new AuthGrantedEvent(service, "fakefakefake"));
    EasyMock.verify(display);
  }

  /** Clicking the "Authenticate" link fires an AuthRequestedEvent. */
  public void testClickAuthLink() {
    AuthRequestedEvent.Handler handler =
        EasyMock.createControl().createMock(AuthRequestedEvent.Handler.class);
    eventBus.addHandler(AuthRequestedEvent.TYPE, handler);

    EasyMock.expect(display.getSelectedScope()).andReturn("scope");
    handler.onAuthRequested(new AuthRequestedEvent("scope"));
    EasyMock.replay(handler, display);

    presenter.clickAuthLink();
    EasyMock.verify(handler, display);
  }

  /**
   * When a ServiceSelectedEvent fires, the auth control goes to its ONLY_PUBLIC state.
   */
  public void testServiceSelected() {
    display.setState(State.ONLY_PUBLIC);
    EasyMock.replay(display);

    eventBus.fireEvent(new ServiceSelectedEvent("service"));
    EasyMock.verify(display);
  }
}
