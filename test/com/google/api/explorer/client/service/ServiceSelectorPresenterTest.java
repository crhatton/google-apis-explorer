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

import com.google.api.explorer.client.ApiDirectory;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.service.ServiceSelectorPresenter.Display;
import com.google.common.collect.ImmutableSet;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link ServiceSelectorPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceSelectorPresenterTest extends TestCase {

  private EventBus eventBus;
  private Display display;
  private ServiceSelectorPresenter presenter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    display = EasyMock.createControl().createMock(Display.class);
    presenter = new ServiceSelectorPresenter(eventBus, display);
  }

  /** When a ServiceMapLoadedEvent fires, it sets the service options. */
  public void testServiceMapLoaded() {
    ApiDirectory.ServiceDefinition service1v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    ApiDirectory.ServiceDefinition service1v2 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    ApiDirectory.ServiceDefinition service2v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);

    display.setLoading(false);
    display.setServices(ImmutableSet.of(service1v1, service1v2, service2v1));
    EasyMock.replay(display);

    eventBus.fireEvent(
        new ServiceDefinitionsLoadedEvent(ImmutableSet.of(service1v1, service1v2, service2v1)));
    EasyMock.verify(display);
  }

  /** When a VersionSelectedEvent fires, it sets the selected service name. */
  public void testSelectVersion() {
    display.selectService("service1");
    EasyMock.replay(display);

    eventBus.fireEvent(new VersionSelectedEvent("service1", "v2"));
    EasyMock.verify(display);
  }

  /** Selecting a service triggers a ServiceSelectedEvent. */
  public void selectService() {
    ServiceSelectedEvent.Handler handler =
        EasyMock.createControl().createMock(ServiceSelectedEvent.Handler.class);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, handler);

    handler.onServiceSelected(new ServiceSelectedEvent("service"));
    EasyMock.replay(handler);

    presenter.selectService("service");
    EasyMock.verify(handler);
  }
}
