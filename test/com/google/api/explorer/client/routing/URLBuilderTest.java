// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.routing;

import com.google.api.explorer.client.routing.UrlBuilder.RootNavigationItem;
import com.google.common.collect.ImmutableMultimap;

import junit.framework.TestCase;

/**
 * Tests for the {@link UrlBuilder} class.
 *
 */
public class URLBuilderTest extends TestCase {

  private UrlBuilder builder = new UrlBuilder();

  /** Test that all URL builder steps through the method work properly. */
  public void testMethodURL() {
    builder.addRootNavigationItem(RootNavigationItem.PREFERRED_SERVICES);
    assertEquals("p/", builder.toString());

    builder.addService("plus", "v1");
    assertEquals("p/plus/v1/", builder.toString());

    builder.addMethodName("a.method.name");
    assertEquals("p/plus/v1/a.method.name", builder.toString());

    builder.addQueryString("akey=avalue");
    assertEquals("p/plus/v1/a.method.name?akey=avalue", builder.toString());
  }

  /** Test selecting a history item. */
  public void testHistoryUrl() {
    builder.addRootNavigationItem(RootNavigationItem.REQUEST_HISTORY);
    assertEquals("h/", builder.toString());

    builder.addHistoryItemKey("1");
    assertEquals("h/1", builder.toString());
  }

  /** Test jumping from a search result directly to a method. */
  public void testSearchThroughMethod() {
    builder.addSearchRoot("queryword");
    assertEquals("search/queryword/", builder.toString());

    builder.addMethodFromSearch("plus", "v1", "a.method.name");
    assertEquals("search/queryword/m/plus/v1/a.method.name", builder.toString());
  }

  /** Tests that empty query params are allowed. */
  public void testEmptyQueryParams() {
    builder.addRootNavigationItem(RootNavigationItem.PREFERRED_SERVICES).addService("plus", "v1")
        .addMethodName("a.method.name").addQueryParams(ImmutableMultimap.<String, String>of());

    assertEquals("p/plus/v1/a.method.name?", builder.toString());
  }

  /** Test a subset of invalid transitions to make sure they are working properly. */
  public void testInvalidTransitions() {
    try {
      builder.addQueryString("akey=avalue");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("", builder.toString());
    }

    try {
      builder.addMethodFromSearch("plus", "v1", "a.method.name");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("", builder.toString());
    }

    try {
      builder.addService("plus", "v1");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("", builder.toString());
    }

    builder.addRootNavigationItem(RootNavigationItem.PREFERRED_SERVICES);
    assertEquals("p/", builder.toString());

    try {
      builder.addHistoryFromSearch("1");
      fail();
    } catch (IllegalStateException e) {
      assertEquals("p/", builder.toString());
    }
  }
}
