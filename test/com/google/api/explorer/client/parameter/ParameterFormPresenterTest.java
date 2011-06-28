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

package com.google.api.explorer.client.parameter;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.ApiParameter.Type;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.parameter.ParameterFormPresenter.Display;
import com.google.api.explorer.client.parameter.ParameterFormPresenter.ParameterComparator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for {@link ParameterFormPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ParameterFormPresenterTest extends TestCase {

  private EventBus eventBus;
  private Display display;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    eventBus = new SimpleEventBus();
    display = EasyMock.createControl().createMock(Display.class);
    AppState appState = EasyMock.createControl().createMock(AppState.class);
    new ParameterFormPresenter(eventBus, appState, new AuthManager(eventBus, appState), display);
  }

  /**
   * When a ServiceSelectedEvent fires, it results in the form becoming
   * invisible.
   */
  public void testServiceSelected() {
    display.setVisible(false);
    EasyMock.replay(display);

    eventBus.fireEvent(new ServiceSelectedEvent("service"));
    EasyMock.verify(display);
  }

  /**
   * When a VersionSelectedEvent fires, it results in the form becoming
   * invisible.
   */
  public void testVersionSelected() {
    display.setVisible(false);
    EasyMock.replay(display);

    eventBus.fireEvent(new VersionSelectedEvent("service", "v1"));
    EasyMock.verify(display);
  }

  /**
   * When a MethodSelectedEvent fires, it results in a call to setMethod() with
   * that method.
   */
  public void testMethodSelected() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("foo", param)).anyTimes();
    EasyMock.expect(method.getParameterOrder()).andReturn(null);
    EasyMock.expect(param.isRequired()).andReturn(false).anyTimes();
    display.setMethod(method, ImmutableSortedMap.of("foo", param));
    EasyMock.replay(display, method, param);

    eventBus.fireEvent(new MethodSelectedEvent("method.identifier", method));
    EasyMock.verify(display, method, param);
  }

  /**
   * When a MethodSelectedEvent fires and specifies parameters to fill in, it
   * results in a call to setMethod() as well as setParameterValues().
   */
  public void testMethodSelected_withParams() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("foo", param)).anyTimes();
    EasyMock.expect(method.getParameterOrder()).andReturn(null);
    EasyMock.expect(param.isRequired()).andReturn(false).anyTimes();
    display.setMethod(method, ImmutableSortedMap.of("foo", param));
    display.setParameterValues(ImmutableMultimap.of("foo", "bar"));
    EasyMock.replay(display, method, param);

    eventBus.fireEvent(
        new MethodSelectedEvent("method.identifier", method, ImmutableMultimap.of("foo", "bar")));
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

    // Decimal parameter.
    assertParameterDescription("description (decimal)", "description", null, null, Type.DECIMAL);

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
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.getDescription()).andReturn(description);
    EasyMock.expect(param.getMinimum()).andReturn(minimum);
    EasyMock.expect(param.getMaximum()).andReturn(maximum);
    EasyMock.expect(param.getType()).andReturn(type);
    EasyMock.replay(param);

    assertEquals(expected, ParameterFormPresenter.generateDescriptionString(param));

    EasyMock.verify(param);
  }

  /**
   * The algorithm in ParameterComparator assures that parameters will be
   * ordered as defined in the parameterOrder list, then alphabetically, even
   * when the parameter map returns the keys in an incorrect order.
   */
  public void testParameterComparator() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    ApiParameter a = EasyMock.createControl().createMock(ApiParameter.class);
    ApiParameter b = EasyMock.createControl().createMock(ApiParameter.class);
    ApiParameter reqA = EasyMock.createControl().createMock(ApiParameter.class);
    ApiParameter reqB = EasyMock.createControl().createMock(ApiParameter.class);
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
}
