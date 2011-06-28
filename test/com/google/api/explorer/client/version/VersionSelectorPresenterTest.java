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

import com.google.api.explorer.client.ApiDirectory;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.version.VersionSelectorPresenter.Display;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gwt.core.client.testing.StubScheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link VersionSelectorPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class VersionSelectorPresenterTest extends TestCase {

  private EventBus eventBus;
  private StubScheduler scheduler;
  private Display display;
  private VersionSelectorPresenter presenter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    display = EasyMock.createControl().createMock(Display.class);
    scheduler = new StubScheduler();
    presenter = new VersionSelectorPresenter(eventBus, scheduler, display);
  }

  /**
   * When a ServiceSelectedEvent fires, it sets the options. It may also cause a
   * VersionSelectedEvent to fire if there is only one valid version.
   */
  public void testServiceSelected() {
    ApiDirectory.ServiceDefinition service1v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service1v1.getName()).andReturn("service1");
    EasyMock.expect(service1v1.getVersion()).andReturn("v1");
    ApiDirectory.ServiceDefinition service1v2 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service1v2.getName()).andReturn("service1");
    EasyMock.expect(service1v2.getVersion()).andReturn("v2");
    ApiDirectory.ServiceDefinition service2v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service2v1.getName()).andReturn("service2");
    EasyMock.expect(service2v1.getVersion()).andReturn("v1");

    display.setVersions(ImmutableList.of(service1v1, service1v2));
    EasyMock.replay(service1v1, service1v2, service2v1, display);

    // A ServiceMapLoaded event must happen first so there will be
    // services/versions to select
    eventBus.fireEvent(
        new ServiceDefinitionsLoadedEvent(ImmutableSet.of(service1v1, service1v2, service2v1)));

    eventBus.fireEvent(new ServiceSelectedEvent("service1"));
    EasyMock.verify(service1v1, service1v2, service2v1, display);
  }

  /**
   * When a ServiceSelectedEvent fires with a service containing one version, it
   * results in a call to setVersions(), and a deferred command to select that
   * version.
   */
  public void testServiceSelected_oneVersion() {
    ApiDirectory.ServiceDefinition service1v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service1v1.getName()).andReturn("service1");
    EasyMock.expect(service1v1.getVersion()).andReturn("v1").times(2);

    display.setVersions(ImmutableList.of(service1v1));
    EasyMock.expectLastCall().times(2);
    display.selectVersion("v1");
    EasyMock.replay(service1v1, display);

    // A ServiceMapLoaded event must happen first so there will be
    // services/versions to select
    eventBus.fireEvent(new ServiceDefinitionsLoadedEvent(ImmutableSet.of(service1v1)));

    // This results in a scheduled command being added to the queue. Execute
    // this command to call selectVersion()
    eventBus.fireEvent(new ServiceSelectedEvent("service1"));
    assertEquals(1, scheduler.getScheduledCommands().size());
    scheduler.getScheduledCommands().get(0).execute();
    EasyMock.verify(service1v1, display);
  }

  /**
   * When a VersionSelectedEvent fires from outside the selector, it sets the
   * version and options.
   */
  public void testVersionSelected() {
    ApiDirectory.ServiceDefinition service1v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service1v1.getName()).andReturn("service1");
    EasyMock.expect(service1v1.getVersion()).andReturn("v1");
    ApiDirectory.ServiceDefinition service1v2 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service1v2.getName()).andReturn("service1");
    EasyMock.expect(service1v2.getVersion()).andReturn("v2");
    ApiDirectory.ServiceDefinition service2v1 =
        EasyMock.createControl().createMock(ApiDirectory.ServiceDefinition.class);
    EasyMock.expect(service2v1.getName()).andReturn("service2");
    EasyMock.expect(service2v1.getVersion()).andReturn("v1");

    display.setVersions(ImmutableList.of(service1v1, service1v2));
    display.selectVersion("v1");
    EasyMock.replay(service1v1, service1v2, service2v1, display);

    // A ServiceMapLoaded event must happen first so there will be
    // services/versions to select
    eventBus.fireEvent(
        new ServiceDefinitionsLoadedEvent(ImmutableSet.of(service1v1, service1v2, service2v1)));

    eventBus.fireEvent(new VersionSelectedEvent("service1", "v1"));
    EasyMock.verify(service1v1, service1v2, service2v1, display);
  }

  /** Selecting a service triggers a ServiceSelectedEvent. */
  public void selectVersion() {
    VersionSelectedEvent.Handler handler =
        EasyMock.createControl().createMock(VersionSelectedEvent.Handler.class);
    eventBus.addHandler(VersionSelectedEvent.TYPE, handler);

    handler.onVersionSelected(new VersionSelectedEvent("service", "v1"));
    EasyMock.replay(handler);

    eventBus.fireEvent(new ServiceSelectedEvent("service"));
    presenter.selectVersion("v1");
    EasyMock.verify(handler);
  }
}
