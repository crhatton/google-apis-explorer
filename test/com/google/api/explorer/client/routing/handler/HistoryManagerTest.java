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

package com.google.api.explorer.client.routing.handler;

import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.analytics.AnalyticsManager.AnalyticsEvent;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ServiceLoader;
import com.google.api.explorer.client.context.ExplorerContext;
import com.google.api.explorer.client.history.HistoryCache;
import com.google.api.explorer.client.history.HistoryItem;
import com.google.api.explorer.client.routing.HistoryWrapper;
import com.google.api.explorer.client.routing.URLManipulator;
import com.google.api.explorer.client.routing.UrlBuilder.RootNavigationItem;
import com.google.api.explorer.client.routing.handler.HistoryManager.HistoryManagerDelegate;
import com.google.api.explorer.client.search.SearchResult;
import com.google.api.explorer.client.search.SearchResultIndex;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.Callback;
import com.google.gwt.event.logical.shared.ValueChangeEvent;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.EasyMock;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link HistoryManager}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class HistoryManagerTest extends TestCase {

  private HistoryManager manager;

  private HistoryWrapper historyWrapper = EasyMock.createMock(HistoryWrapper.class);
  private URLManipulator urlManipulator = EasyMock.createMock(URLManipulator.class);
  private ServiceLoader serviceLoader = EasyMock.createMock(ServiceLoader.class);
  private HistoryCache historyCache = EasyMock.createMock(HistoryCache.class);
  private AnalyticsManager analyticsManager = EasyMock.createMock(AnalyticsManager.class);
  private SearchResultIndex resultIndex = EasyMock.createMock(SearchResultIndex.class);

  private HistoryManagerDelegate delegate = EasyMock.createMock(HistoryManagerDelegate.class);

  @Override
  protected void setUp() throws Exception {

    manager = new HistoryManager(historyWrapper,
        urlManipulator,
        serviceLoader,
        historyCache,
        analyticsManager,
        resultIndex);

    manager.delegate = delegate;
  }

  /** Test what happens when you navigate to the root url for the preferred service list. */
  public void testRootRouting() {
    Capture<Callback<Set<ServiceDefinition>, String>> cbCapture =
        new Capture<Callback<Set<ServiceDefinition>, String>>();
    serviceLoader.loadServiceDefinitions(EasyMock.capture(cbCapture));
    EasyMock.expectLastCall();

    Capture<ExplorerContext> contextCapture = new Capture<ExplorerContext>();
    delegate.setContext(EasyMock.capture(contextCapture));
    EasyMock.expectLastCall();

    Set<ServiceDefinition> items = Collections.emptySet();

    EasyMock.replay(delegate, serviceLoader);

    manager.processUrl("p/");

    assertTrue(cbCapture.hasCaptured());

    cbCapture.getValue().onSuccess(items);

    assertTrue(contextCapture.hasCaptured());
    ExplorerContext ctxt = contextCapture.getValue();

    assertTrue(ctxt.isEntryListVisible());
    assertEquals(Collections.emptySet(), ctxt.getServicesList());
    assertEquals(RootNavigationItem.PREFERRED_SERVICES, ctxt.getRootNavigationItem());

    EasyMock.verify(delegate, serviceLoader);
  }

  /** Test routing to a specific service. */
  public void testServiceRouting() {
    Capture<Callback<ApiService, String>> cbCapture = new Capture<Callback<ApiService, String>>();
    serviceLoader.loadService(EasyMock.eq("plus"), EasyMock.eq("v1"), EasyMock.capture(cbCapture));
    EasyMock.expectLastCall().times(2);

    Capture<ExplorerContext> contextCapture = new Capture<ExplorerContext>();
    delegate.setContext(EasyMock.capture(contextCapture));
    EasyMock.expectLastCall();

    ApiService service = EasyMock.createMock(ApiService.class);
    EasyMock.expect(service.displayTitle()).andReturn("Plus API");
    EasyMock.expect(service.getVersion()).andReturn("v1");

    EasyMock.replay(serviceLoader, delegate, service);

    manager.processUrl("s/plus/v1/");

    assertTrue(cbCapture.hasCaptured());

    // This call completes the title for breadcrumbs.
    cbCapture.getValue().onSuccess(service);

    // This call completes the service load to generate the context.
    cbCapture.getValue().onSuccess(service);

    assertTrue(contextCapture.hasCaptured());
    ExplorerContext ctxt = contextCapture.getValue();

    assertEquals(service, ctxt.getService());
    assertTrue(ctxt.isEntryListVisible());
    assertEquals(RootNavigationItem.ALL_VERSIONS, ctxt.getRootNavigationItem());

    EasyMock.verify(serviceLoader, delegate, service);
  }

  public void testMethodRouting() {
    Capture<Callback<ApiService, String>> cbCapture = new Capture<Callback<ApiService, String>>();
    serviceLoader.loadService(EasyMock.eq("plus"), EasyMock.eq("v1"), EasyMock.capture(cbCapture));
    EasyMock.expectLastCall().times(2);

    Capture<ExplorerContext> contextCapture = new Capture<ExplorerContext>();
    delegate.setContext(EasyMock.capture(contextCapture));
    EasyMock.expectLastCall();

    ApiService service = EasyMock.createMock(ApiService.class);
    ApiMethod method = EasyMock.createMock(ApiMethod.class);
    Map<String, ApiMethod> allMethods = ImmutableMap.of("plus.method.name", method);
    EasyMock.expect(service.allMethods()).andReturn(allMethods);
    EasyMock.expect(service.displayTitle()).andReturn("Plus API");
    EasyMock.expect(service.getVersion()).andReturn("v1");

    EasyMock.replay(serviceLoader, delegate, service);

    manager.processUrl("s/plus/v1/plus.method.name");

    assertTrue(cbCapture.hasCaptured());

    // This call fills in the breadcrumbs.
    cbCapture.getValue().onSuccess(service);

    // This call generated the context.
    cbCapture.getValue().onSuccess(service);

    assertTrue(contextCapture.hasCaptured());
    ExplorerContext ctxt = contextCapture.getValue();

    assertEquals(service, ctxt.getService());
    assertEquals(method, ctxt.getMethod());
    assertTrue(ctxt.isMethodFormVisible());
    assertFalse(ctxt.isEntryListVisible());
    assertEquals(RootNavigationItem.ALL_VERSIONS, ctxt.getRootNavigationItem());

    EasyMock.verify(serviceLoader, delegate, service);
  }

  /** Test routing when there is a search term, including premature routing. */
  public void testSearchRouting() {
    String searchTerm = "searchTerm";

    EasyMock.expect(resultIndex.search(searchTerm))
        .andReturn(Collections.<SearchResult>emptySet()).times(2);

    Capture<ExplorerContext> contextCapture = new Capture<ExplorerContext>();
    delegate.setContext(EasyMock.capture(contextCapture));
    EasyMock.expectLastCall().times(2);

    EasyMock.replay(resultIndex, delegate);

    manager.processUrl("search/" + searchTerm + "/");

    assertTrue(contextCapture.hasCaptured());
    ExplorerContext ctxt = contextCapture.getValue();

    assertTrue(Iterables.isEmpty(ctxt.getSearchResults()));
    assertTrue(ctxt.isSearchResultsVisible());
    assertEquals(RootNavigationItem.NONE, ctxt.getRootNavigationItem());

    // This will invoke the second calls of search and setContext.
    manager.searchReady();

    EasyMock.verify(resultIndex, delegate);
  }

  public void testNestedSearchRouting() {
    Capture<Callback<ApiService, String>> cbCapture = new Capture<Callback<ApiService, String>>();
    serviceLoader.loadService(EasyMock.eq("plus"), EasyMock.eq("v1"), EasyMock.capture(cbCapture));
    EasyMock.expectLastCall();

    EasyMock.replay(serviceLoader);

    manager.processUrl("search/searchterm/plus/v1/");

    assertTrue(cbCapture.hasCaptured());

    EasyMock.verify(serviceLoader);
  }


  public void testHistoryItemWithAnalytics() {
    String fragment = "h/1";
    analyticsManager.trackEvent(AnalyticsEvent.SHOW_HISTORY);
    EasyMock.expectLastCall();

    @SuppressWarnings("unchecked")
    ValueChangeEvent<String> event = EasyMock.createMock(ValueChangeEvent.class);
    EasyMock.expect(event.getValue()).andReturn(fragment);

    HistoryItem item = createEmptyHistoryItem();
    EasyMock.expect(historyCache.getHistoryItem("1")).andReturn(item);

    Capture<ExplorerContext> contextCapture = new Capture<ExplorerContext>();
    delegate.setContext(EasyMock.capture(contextCapture));
    EasyMock.expectLastCall();

    EasyMock.replay(event, analyticsManager, historyCache, delegate);

    manager.onValueChange(event);

    assertTrue(contextCapture.hasCaptured());
    ExplorerContext ctxt = contextCapture.getValue();

    assertEquals(item, Iterables.getOnlyElement(ctxt.getHistoryItems()));
    assertTrue(ctxt.isHistoryItemVisible());
    assertEquals(RootNavigationItem.REQUEST_HISTORY, ctxt.getRootNavigationItem());

    EasyMock.verify(event, analyticsManager, historyCache, delegate);
  }

  /** This is necessary because history item cannot be subclassed. */
  private HistoryItem createEmptyHistoryItem() {
    ApiRequest mockRequest = EasyMock.createMock(ApiRequest.class);
    ApiResponse mockResponse = EasyMock.createMock(ApiResponse.class);
    return new HistoryItem("1", mockRequest, mockResponse, 0, 0);
  }
}
