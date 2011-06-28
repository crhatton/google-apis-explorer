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
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link HistoryManager}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class HistoryManagerTest extends TestCase {

  private EventBus eventBus;
  private HistoryManager historyManager;
  private AppState appState;
  private MockHistoryWrapper history;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    appState = EasyMock.createControl().createMock(AppState.class);
    history = new MockHistoryWrapper();
    historyManager = new HistoryManager(eventBus, appState, history);
  }

  /** When a VersionSelectedEvent fires, the history token is updated. */
  @SuppressWarnings("unchecked")
  public void testVersionSelected() {
    eventBus.fireEvent(new VersionSelectedEvent("service", "version"));
    assertEquals("_s=service&_v=version", history.latestToken);
  }

  /**
   * When a VersionSelectedEvent fires with no service or version, the history
   * token is cleared.
   */
  @SuppressWarnings("unchecked")
  public void testVersionSelected_unselect() {
    eventBus.fireEvent(new VersionSelectedEvent(null, null));
    assertEquals("", history.latestToken);
  }

  /** When a MethodSelectedEvent fires, the history token is updated. */
  @SuppressWarnings("unchecked")
  public void testMethodSelected() {
    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.getName()).andReturn("service");
    EasyMock.expect(service.getVersion()).andReturn("version");
    EasyMock.expect(appState.getCurrentService()).andReturn(service).times(2);
    EasyMock.expect(appState.getCurrentMethodIdentifier()).andReturn("method.one");

    EasyMock.replay(service, appState);

    eventBus.fireEvent(new MethodSelectedEvent("method.one", method));
    assertEquals("_s=service&_v=version&_m=method.one", history.latestToken);
    EasyMock.verify(service, appState);
  }

  /** When a ValueChangeEvent is fired, a VersionSelectedEvent fires. */
  @SuppressWarnings("unchecked")
  public void testHistoryValueChange_serviceVersion() {
    eventBus.fireEvent(new VersionSelectedEvent("service", "version"));
    assertEquals("_s=service&_v=version", history.latestToken);
  }

  /**
   * When a ValueChangeEvent is fired and specifies a method, a
   * VersionSelectedEvent fires with that method.
   */
  @SuppressWarnings("unchecked")
  public void testHistoryValueChange_serviceVersionMethod() {
    VersionSelectedEvent.Handler handler =
        EasyMock.createControl().createMock(VersionSelectedEvent.Handler.class);
    eventBus.addHandler(VersionSelectedEvent.TYPE, handler);
    handler.onVersionSelected(new VersionSelectedEvent(
        "service", "version", "method.one", ImmutableMultimap.<String, String>of()));
    EasyMock.replay(handler);

    historyManager.onValueChange(
        new InstantiableValueChangeEvent("_s=service&_v=version&_m=method.one"));
    EasyMock.verify(handler);
  }

  /**
   * When a ValueChangeEvent is fired and specifies a method and parameters, a
   * VersionSelectedEvent fires with that method and those parameters.
   */
  @SuppressWarnings("unchecked")
  public void testHistoryValueChange_serviceVersionMethodParams() {
    VersionSelectedEvent.Handler handler =
        EasyMock.createControl().createMock(VersionSelectedEvent.Handler.class);
    eventBus.addHandler(VersionSelectedEvent.TYPE, handler);
    Multimap<String, String> params = ImmutableMultimap.of("q", "foo", "x", "bar");
    handler.onVersionSelected(new VersionSelectedEvent("service", "version", "method.one", params));
    EasyMock.replay(handler);

    historyManager.onValueChange(
        new InstantiableValueChangeEvent("_s=service&_v=version&_m=method.one&q=foo&x=bar"));
    EasyMock.verify(handler);
  }

  /**
   * Mock implementation of {@link HistoryWrapper} which simply fires token
   * change events when the token is updated.
   */
  private static class MockHistoryWrapper implements HistoryWrapper {
    private String latestToken;

    @Override
    public void addValueChangeHandler(ValueChangeHandler<String> handler) {
    }

    @Override
    public void newItem(String token) {
      this.latestToken = token;
    }
  }

  /** Subclass of {@link ValueChangeEvent} that exposes its constructor. */
  private static class InstantiableValueChangeEvent extends ValueChangeEvent<String> {
    public InstantiableValueChangeEvent(String value) {
      super(value);
    }
  }
}
