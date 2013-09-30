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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.CallStyle;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.Schema.Type;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.Display;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.ParameterComparator;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.RequestFinishedCallback;
import com.google.api.explorer.client.routing.UrlBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link EmbeddedParameterFormPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedParameterFormPresenterTest extends TestCase {

  private Display display;
  private EmbeddedParameterFormPresenter presenter;
  private ApiService service;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    display = EasyMock.createControl().createMock(Display.class);

    service = EasyMock.createMock(ApiService.class);
    EasyMock.expect(service.callStyle()).andReturn(CallStyle.REST);

    EasyMock.replay(service);
    presenter = new EmbeddedParameterFormPresenter(
        new AuthManager(), display, new RequestFinishedCallback() {
          @Override
          public void finished(
              ApiRequest request, ApiResponse response, long startTime, long endTime) {
            // Intentionally blank, we're not executing any requests
          }

          @Override
          public void starting(ApiRequest request) {
            // Intentionally blank
          }
        });
  }

  /**
   * When a MethodSelectedEvent fires, it results in a call to setMethod() with
   * that method.
   */
  public void testMethodSelected() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    Schema param = EasyMock.createControl().createMock(Schema.class);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("foo", param)).anyTimes();
    EasyMock.expect(method.getParameterOrder()).andReturn(null);
    EasyMock.expect(param.isRequired()).andReturn(false).anyTimes();

    display.setMethod(service, method, ImmutableSortedMap.of("foo", param),
        ImmutableMultimap.<String, String>of(), null);
    EasyMock.expectLastCall();

    EasyMock.replay(display, method, param);

    presenter.selectMethod(service, method, ImmutableMultimap.<String, String>of());

    EasyMock.verify(display, method, param);
  }

  /**
   * When a MethodSelectedEvent fires and specifies parameters to fill in, it
   * results in a call to setMethod() as well as setParameterValues().
   */
  public void testMethodSelected_withParams() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    Schema param = EasyMock.createControl().createMock(Schema.class);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("foo", param)).anyTimes();
    EasyMock.expect(method.getParameterOrder()).andReturn(null);
    EasyMock.expect(param.isRequired()).andReturn(false).anyTimes();

    Multimap<String, String> queryParams = ImmutableMultimap.of("foo", "bar");
    display.setMethod(service, method, ImmutableSortedMap.of("foo", param), queryParams, null);
    EasyMock.expectLastCall();

    EasyMock.replay(display, method, param);

    presenter.selectMethod(service, method, queryParams);

    EasyMock.verify(display, method, param);
  }

  /**
   * Tests parameter description string generation under a variety of
   * conditions.
   */
  public void testParameterDescription() {
    // Simple String parameter with description provided.
    assertParameterDescription("description (string)", "description", null, null, Type.STRING);

    // Parameter with no description provided.
    assertParameterDescription("(string)", null, null, null, Type.STRING);

    // Boolean parameter.
    assertParameterDescription("description (boolean)", "description", null, null, Type.BOOLEAN);

    // Number parameter.
    assertParameterDescription("description (number)", "description", null, null, Type.NUMBER);

    // Parameter with minimum and maximum.
    assertParameterDescription(
        "description (integer, 1-100)", "description", "1", "100", Type.INTEGER);

    // Parameter with only minimum.
    assertParameterDescription("description (integer, 1+)", "description", "1", null, Type.INTEGER);

    // Parameter with only maximum.
    assertParameterDescription(
        "description (integer, max 100)", "description", null, "100", Type.INTEGER);

    // Parameter with a large maximum.
    assertParameterDescription("description (integer, 1+)", "description", "1",
        String.valueOf(Integer.MAX_VALUE), Type.INTEGER);
  }

  private static void assertParameterDescription(
      String expected, String description, String minimum, String maximum, Type type) {
    Schema param = EasyMock.createControl().createMock(Schema.class);
    EasyMock.expect(param.getDescription()).andReturn(description);
    EasyMock.expect(param.getMinimum()).andReturn(minimum);
    EasyMock.expect(param.getMaximum()).andReturn(maximum);
    EasyMock.expect(param.getType()).andReturn(type);
    EasyMock.replay(param);

    assertEquals(expected, EmbeddedParameterFormPresenter.generateDescriptionString(param));

    EasyMock.verify(param);
  }

  /**
   * The algorithm in ParameterComparator assures that parameters will be
   * ordered as defined in the parameterOrder list, then alphabetically, even
   * when the parameter map returns the keys in an incorrect order.
   */
  public void testParameterComparator() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    Schema a = EasyMock.createControl().createMock(Schema.class);
    Schema b = EasyMock.createControl().createMock(Schema.class);
    Schema reqA = EasyMock.createControl().createMock(Schema.class);
    Schema reqB = EasyMock.createControl().createMock(Schema.class);
    // Using a SortedMap so that the keys can be guaranteed to be returned in a
    // known unsorted order.
    EasyMock.expect(method.getParameters()).andReturn(
        ImmutableSortedMap.of("b", b, "req-a", reqA, "a", a, "req-b", reqB));
    EasyMock.expect(method.getParameterOrder()).andReturn(ImmutableList.of("req-b", "req-a"));
    EasyMock.replay(method);

    assertEquals(ImmutableSortedSet.of("req-b", "req-a", "a", "b"), ImmutableSortedMap.copyOf(
        method.getParameters(), new ParameterComparator(method.getParameterOrder())).keySet());

    EasyMock.verify(method);
  }

  /**
   * Test that when a request body is specified as a query param that it gets mapped to the request
   * body editor.
   */
  public void testRequestBodyPredefinition() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    Schema param = EasyMock.createControl().createMock(Schema.class);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("foo", param)).anyTimes();
    EasyMock.expect(method.getParameterOrder()).andReturn(null);
    EasyMock.expect(param.isRequired()).andReturn(false).anyTimes();

    String requestBodyValue = "{\"key\": \"value\"}";
    Multimap<String, String> queryParams =
        ImmutableMultimap.of("foo", "bar", UrlBuilder.BODY_QUERY_PARAM_KEY, requestBodyValue);
    display.setMethod(
        service, method, ImmutableSortedMap.of("foo", param), queryParams, requestBodyValue);
    EasyMock.expectLastCall();

    EasyMock.replay(display, method, param);

    presenter.selectMethod(service, method, queryParams);

    EasyMock.verify(display, method, param);
  }
}
