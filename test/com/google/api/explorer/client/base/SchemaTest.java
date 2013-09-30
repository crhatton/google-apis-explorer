// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.base;

import com.google.common.collect.ImmutableMap;
import com.google.web.bindery.autobean.shared.AutoBean;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Map;

/**
 * Tests for the schema class.
 *
 */
public class SchemaTest extends TestCase {

  /**
   * Create a chain of schema references and verify that the dereferencing works
   * properly.
   */
  @SuppressWarnings("unchecked")
  public void testReferenceFollowing() {
    Schema startingPoint = EasyMock.createMock(Schema.class);
    EasyMock.expect(startingPoint.getRef()).andReturn("Interstitial");

    AutoBean<Schema> bean = EasyMock.createMock(AutoBean.class);
    EasyMock.expect(bean.as()).andReturn(startingPoint);

    Schema interstitial = EasyMock.createMock(Schema.class);
    EasyMock.expect(interstitial.getRef()).andReturn("Concrete");

    Schema concrete = EasyMock.createMock(Schema.class);
    EasyMock.expect(concrete.getRef()).andReturn(null);
    EasyMock.expect(concrete.getId()).andReturn("Concrete");

    Map<String, Schema> allSchemas =
        ImmutableMap.of("Interstitial", interstitial, "Concrete", concrete);

    EasyMock.replay(startingPoint, interstitial, concrete, bean);

    Schema dereferenced = Schema.PropertyWrapper.followRefs(bean, allSchemas);

    assertEquals(dereferenced, concrete);
    assertEquals("Concrete", concrete.getId());

    EasyMock.verify(startingPoint, interstitial, concrete, bean);
  }

  /**
   * Test that when a named reference is not found we end up with null.
   */
  @SuppressWarnings("unchecked")
  public void testNotFound() {
    Schema startingPoint = EasyMock.createMock(Schema.class);
    EasyMock.expect(startingPoint.getRef()).andReturn("NotARealSchema");

    AutoBean<Schema> bean = EasyMock.createMock(AutoBean.class);
    EasyMock.expect(bean.as()).andReturn(startingPoint);

    Map<String, Schema> allSchemas = ImmutableMap.of();

    EasyMock.replay(startingPoint, bean);

    Schema dereferenced = Schema.PropertyWrapper.followRefs(bean, allSchemas);

    assertEquals(dereferenced, null);

    EasyMock.verify(startingPoint, bean);
  }
}
