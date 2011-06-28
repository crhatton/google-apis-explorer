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
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link AppState}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AppStateTest extends TestCase {

  private EventBus eventBus;
  private AppState appState;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    appState = new AppState(eventBus);
  }

  /** When a ServiceLoadedEvent fires, the current service updates accordingly. */
  public void testGetCurrentService() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    eventBus.fireEvent(new ServiceLoadedEvent(service));
    assertEquals(service, appState.getCurrentService());

    ApiService service1 = EasyMock.createControl().createMock(ApiService.class);
    eventBus.fireEvent(new ServiceLoadedEvent(service1));
    assertEquals(service1, appState.getCurrentService());

  }

  /**
   * When a MethodSelectedEvent fires, the current method identifier updates
   * accordingly.
   */
  public void testGetCurrentMethodIdentifer() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    eventBus.fireEvent(new MethodSelectedEvent("method.one", method));
    assertEquals("method.one", appState.getCurrentMethodIdentifier());

    eventBus.fireEvent(new MethodSelectedEvent("method.two", method));
    assertEquals("method.two", appState.getCurrentMethodIdentifier());
  }
}
