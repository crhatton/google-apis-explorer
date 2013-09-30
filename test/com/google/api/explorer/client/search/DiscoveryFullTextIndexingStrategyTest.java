// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.search;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.CallStyle;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.search.SearchResult.Kind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Collections;
import java.util.List;

/**
 * Test the discovery document indexing strategy.
 *
 */
public class DiscoveryFullTextIndexingStrategyTest extends TestCase {
  private IndexingStrategy<ApiService> discoveryStrategy = new DiscoveryFullTextIndexingStrategy();
  private ApiService mockService = EasyMock.createMock(ApiService.class);

  @Override
  public void setUp() {
    EasyMock.expect(mockService.getName()).andReturn("serviceName").anyTimes();
    EasyMock.expect(mockService.getVersion()).andReturn("v1.2").anyTimes();
    EasyMock.expect(mockService.callStyle()).andReturn(CallStyle.REST).anyTimes();
    EasyMock.expect(mockService.getDescription())
        .andReturn("serviceDescription description").anyTimes();
  }

  /** Test that the top level service gets processed correctly. */
  public void testServiceResult() {
    EasyMock.expect(mockService.allMethods())
        .andReturn(Collections.<String, ApiMethod>emptyMap()).anyTimes();
    EasyMock.replay(mockService);

    // Extract the only entry from the entry iterable.
    SearchEntry serviceEntry = Iterables.getOnlyElement(discoveryStrategy.index(mockService));

    EasyMock.verify(mockService);

    // Verify the entry stream.
    assertEquals(ImmutableSet.of("servicename", "v1.2", "servicedescription", "description"),
        serviceEntry.getKeywords());

    // Verify the search result.
    SearchResult apiResult = serviceEntry.getSearchResult();
    assertEquals(Kind.SERVICE, apiResult.getKind());
    assertEquals(mockService.getName(), apiResult.getService().getName());
    assertEquals(mockService.getVersion(), apiResult.getService().getVersion());
  }

  /** Test that method names and descriptions get indexed correctly. */
  public void testMethods() {
    ApiMethod mockMethod = EasyMock.createMock(ApiMethod.class);
    EasyMock.expect(mockMethod.getDescription()).andReturn("method description").anyTimes();
    EasyMock.expect(mockMethod.getId()).andReturn("collection.methodName").anyTimes();
    EasyMock.expect(mockMethod.getParameters()).andReturn(null).anyTimes();

    ApiMethod mockMethod2 = EasyMock.createMock(ApiMethod.class);
    EasyMock.expect(mockMethod2.getDescription()).andReturn("anotherMethod description").anyTimes();
    EasyMock.expect(mockMethod2.getId()).andReturn("collection.anotherMethod").anyTimes();
    EasyMock.expect(mockMethod2.getParameters()).andReturn(null).anyTimes();

    EasyMock.expect(mockService.allMethods()).andReturn(ImmutableMap.of(
        "collection.methodName", mockMethod, "collection.anotherMethod", mockMethod2));

    EasyMock.replay(mockService, mockMethod, mockMethod2);

    List<SearchEntry> entries = ImmutableList.copyOf(discoveryStrategy.index(mockService));

    EasyMock.verify(mockService, mockMethod, mockMethod2);

    assertEquals(3, entries.size());

    // Extract the entries.
    SearchEntry method1Entry = null;
    SearchEntry method2Entry = null;
    SearchEntry serviceEntry = null;
    for (SearchEntry entry : entries) {
      SearchResult result = entry.getSearchResult();

      if (result.getKind() == Kind.SERVICE) {
        serviceEntry = entry;
      } else if (result.getKind() == Kind.METHOD) {
        if (mockMethod.equals(result.getMethodBundle().getMethod())) {
          method1Entry = entry;
        } else if (mockMethod2.equals(result.getMethodBundle().getMethod())) {
          method2Entry = entry;
        } else {
          fail("Unexpected method result: " + result.getMethodBundle().getMethod());
        }
      } else {
        fail("Not an expected entry with kind: " + result.getKind());
      }
    }

    // Verify the entries.
    assertEquals(ImmutableSet.of("servicename", "v1.2", "servicedescription", "description"),
        serviceEntry.getKeywords());
    assertEquals(ImmutableSet.of("servicename",
        "v1.2",
        "collection",
        "collection.methodname",
        "method",
        "description",
        "methodname"), method1Entry.getKeywords());
    assertEquals(ImmutableSet.of("servicename",
        "v1.2",
        "collection",
        "collection.anothermethod",
        "anothermethod",
        "description"), method2Entry.getKeywords());

    // Verify the method search result.
    SearchResult methodResult = method1Entry.getSearchResult();
    assertEquals(Kind.METHOD, methodResult.getKind());
    assertEquals("serviceName", methodResult.getMethodBundle().getService().getName());
    assertEquals("v1.2", methodResult.getMethodBundle().getService().getVersion());
    assertEquals(mockMethod, methodResult.getMethodBundle().getMethod());
  }

  /** Test that method parameters, and method parameter descriptions get indexed. */
  public void testMethodParameters() {
    Schema parameter = EasyMock.createMock(Schema.class);
    EasyMock.expect(parameter.getDescription())
        .andReturn("parameterDescription description").anyTimes();

    Schema noDescription = EasyMock.createMock(Schema.class);
    EasyMock.expect(noDescription.getDescription()).andReturn(null).anyTimes();

    ApiMethod mockMethod = EasyMock.createMock(ApiMethod.class);
    EasyMock.expect(mockMethod.getDescription()).andReturn("method description").anyTimes();
    EasyMock.expect(mockMethod.getId()).andReturn("collection.methodName").anyTimes();
    EasyMock.expect(mockMethod.getParameters())
        .andReturn(ImmutableMap.of("paramName", parameter, "noDescription", noDescription))
        .anyTimes();

    EasyMock.expect(mockService.allMethods())
        .andReturn(ImmutableMap.of("collection.methodName", mockMethod));

    EasyMock.replay(mockService, mockMethod, parameter, noDescription);

    List<SearchEntry> entries = ImmutableList.copyOf(discoveryStrategy.index(mockService));

    EasyMock.verify(mockService, mockMethod, parameter, noDescription);

    // Verify the entries.
    assertEquals(2, entries.size());
    SearchEntry methodEntry = null;
    for (SearchEntry entry : entries) {
      if (entry.getSearchResult().getKind() == Kind.METHOD) {
        methodEntry = entry;
      } else if (entry.getSearchResult().getKind() == Kind.SERVICE) {
        // Intentionally blank, ignore the service entry.
      } else {
        fail("Not an expected entry: " + entry.getSearchResult().getKind());
      }
    }

    assertEquals(ImmutableSet.of("servicename",
        "v1.2",
        "collection",
        "collection.methodname",
        "method",
        "description",
        "methodname",
        "paramname",
        "nodescription",
        "parameterdescription"), methodEntry.getKeywords());
  }
}
