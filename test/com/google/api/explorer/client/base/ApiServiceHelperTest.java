// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.base;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Simple tests for the code that handles the actual embedding of the embedded explorer.
 *
 */
public class ApiServiceHelperTest extends TestCase {

  /**
   * Test that for doc sets that have not yet been regenerated we can still look up the method name
   * by the old name.
   */
  public void testBackwardCompatibleNameResolution() {
    String fullMethodName = "service.collection.methodName";
    String searchMethodName = "collection.methodName";

    // This is the method we are searching for.
    ApiMethod mockMethod = EasyMock.createMock(ApiMethod.class);

    // This is the service that we generated.
    ApiService mockService = EasyMock.createMock(ApiService.class);
    EasyMock.expect(mockService.method(searchMethodName)).andReturn(null);
    EasyMock.expect(mockService.allMethods())
        .andReturn(ImmutableMap.of(fullMethodName, mockMethod));

    EasyMock.replay(mockService, mockMethod);

    ApiMethod foundMethod = ApiServiceHelper.resolveMethod(mockService, searchMethodName);
    assertEquals(mockMethod, foundMethod);

    EasyMock.verify(mockService, mockMethod);
  }

  /**
   * Test that for services that have been regenerated we can look up the method name using the new
   * naming convention.
   */
  public void testMethodResolution() {
    String fullMethodName = "service.collection.methodName";

    // This is the method we are searching for.
    ApiMethod mockMethod = EasyMock.createMock(ApiMethod.class);

    // This is the service that we generated.
    ApiService mockService = EasyMock.createMock(ApiService.class);
    EasyMock.expect(mockService.method(fullMethodName)).andReturn(mockMethod);

    EasyMock.replay(mockService, mockMethod);

    ApiMethod foundMethod = ApiServiceHelper.resolveMethod(mockService, fullMethodName);
    assertEquals(mockMethod, foundMethod);

    EasyMock.verify(mockService, mockMethod);
  }
}
