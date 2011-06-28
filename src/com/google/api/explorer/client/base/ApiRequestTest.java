/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.api.explorer.client.base;

import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiParameter.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.List;

/**
 * Tests validation of parameter values and creation of request path in
 * {@link ApiRequest}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiRequestTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Config.setApiKey("");
  }

  /** Validating a request with one non-required parameter passes. */
  public void testValidate_noParams() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.<String, ApiParameter>of());

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(method, service);

    ApiRequest request = new ApiRequest(service, "method");

    request.validate();

    EasyMock.verify(method, service);
  }

  /** Validating a request with a missing required parameter fails. */
  public void testValidate_missingRequired() {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(true);

    assertInvalid("[param] is required, and must be given a value.", param);
  }

  /**
   * Validating a request with multiple values for a non-repeated parameter
   * fails.
   */
  public void testValidate_invalidRepeatedParams() {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(false);
    EasyMock.expect(param.isRepeated()).andReturn(false);

    assertInvalid("[param] is not a repeated parameter, and cannot be given multiple values.",
        param, "val1", "val2");
  }

  /**
   * Validating a request with a param value that matches the required integer
   * type passes.
   */
  public void testValidate_validIntegerType() {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(false);
    EasyMock.expect(param.isRepeated()).andReturn(false);
    EasyMock.expect(param.getPattern()).andReturn(null);
    EasyMock.expect(param.getEnumValues()).andReturn(null);
    EasyMock.expect(param.getType()).andReturn(Type.INTEGER);
    EasyMock.expect(param.getMinimum()).andReturn(null);
    EasyMock.expect(param.getMaximum()).andReturn(null);

    assertValid(param, "12345678901234567890");
  }

  /**
   * Validating a request with a value that is a integer value below the
   * minimum.
   */
  public void testValidate_belowMinimum() {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(false);
    EasyMock.expect(param.isRepeated()).andReturn(false);
    EasyMock.expect(param.getPattern()).andReturn(null);
    EasyMock.expect(param.getEnumValues()).andReturn(null);
    EasyMock.expect(param.getType()).andReturn(Type.INTEGER);
    EasyMock.expect(param.getMinimum()).andReturn("-1");

    assertInvalid("[param] is less than the allowable minimum: -1", param, "-2");
  }

  /**
   * Validating a request with a value that is a integer value above the
   * maximum.
   */
  public void testValidate_aboveMaximum() {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(false);
    EasyMock.expect(param.isRepeated()).andReturn(false);
    EasyMock.expect(param.getPattern()).andReturn(null);
    EasyMock.expect(param.getEnumValues()).andReturn(null);
    EasyMock.expect(param.getType()).andReturn(Type.INTEGER);
    EasyMock.expect(param.getMinimum()).andReturn(null);
    EasyMock.expect(param.getMaximum()).andReturn("1");

    assertInvalid("[param] is greater than the allowable maximum: 1", param, "2");
  }

  /**
   * Checks that the given values will result in validation passing for the
   * given parameter.
   */
  private static void assertValid(ApiParameter param, String... values) {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("param", param));

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(param, method, service);

    ApiRequest request = new ApiRequest(service, "method");
    request.paramValues.putAll("param", Lists.newArrayList(values));

    request.validate();

    EasyMock.verify(param, method, service);
  }

  /**
   * Checks that the given values will result in validation failing for the
   * given parameter, with the expected error message.
   */
  private static void assertInvalid(
      String expectedErrorMessage, ApiParameter param, String... values) {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("param", param));

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(param, method, service);

    ApiRequest request = new ApiRequest(service, "method");
    request.paramValues.putAll("param", Lists.newArrayList(values));

    try {
      request.validate();
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals(expectedErrorMessage, e.getMessage());
    }

    EasyMock.verify(param, method, service);
  }

  /**
   * Tests lots of cases where validation may pass or fail, based on different
   * criteria and provided values.
   */
  public void testValidate() {
    // Testing provided required parameters
    assertValid(true, false, null, null, Type.STRING, "value");

    // Testing valid repeated parameters
    assertValid(false, true, null, null, Type.STRING, "val1", "val2");

    // Testing pattern parameters
    String pattern = "@.{2}";
    assertValid(false, false, pattern, null, Type.STRING, "@ab");
    assertInvalid("[param] does not match the required pattern: @.{2}",
        false, false, pattern, null, Type.STRING, "@abcd");

    // Testing enum value parameters.
    List<String> enumValues = ImmutableList.of("foo", "bar");
    assertValid(false, false, null, enumValues, Type.STRING, "foo");
    assertValid(false, false, null, enumValues, Type.STRING, "bar");
    assertInvalid("[param] is not one of the defined valid values: [foo, bar]",
        false, false, null, enumValues, Type.STRING, "bad");

    // Testing integer parameters.
    assertInvalid("[param] is not a valid integer value.",
        false, false, null, null, Type.INTEGER, "0.0");
    assertInvalid("[param] is not a valid integer value.",
        false, false, null, null, Type.INTEGER, "asdf");
    assertInvalid("[param] is not a valid integer value.",
        false, false, null, null, Type.INTEGER, "true");

    // Testing decimal parameters.
    assertValid(false, false, null, null, Type.DECIMAL, "123456789.123456789");
    assertValid(false, false, null, null, Type.DECIMAL, "1.111");
    assertInvalid("[param] is not a valid decimal value.",
        false, false, null, null, Type.DECIMAL, "stringValue");

    // Testing boolean parameters.
    assertValid(false, false, null, null, Type.BOOLEAN, "true");
    assertValid(false, false, null, null, Type.BOOLEAN, "false");
    assertInvalid("[param] is not a valid boolean value.",
        false, false, null, null, Type.BOOLEAN, "12345");
  }

  /**
   * Creates a mock ApiParameter based on the given conditions and checks that
   * the given values will result in validation passing.
   */
  private static void assertValid(boolean required, boolean repeated, String pattern,
      List<String> enumValues, Type type, String... values) {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(required);
    EasyMock.expect(param.isRepeated()).andReturn(repeated);
    EasyMock.expect(param.getPattern()).andReturn(pattern);
    EasyMock.expect(param.getEnumValues()).andReturn(enumValues);
    EasyMock.expect(param.getType()).andReturn(type);

    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("param", param));

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(param, method, service);

    ApiRequest request = new ApiRequest(service, "method");
    request.paramValues.putAll("param", Lists.newArrayList(values));

    request.validate();

    EasyMock.verify(param, method, service);
  }

  /**
   * Creates a mock ApiParameter based on the given conditions and checks that
   *  the given values will result in validation failing with the expected error
   * message.
   */
  private static void assertInvalid(String expectedErrorMessage, boolean required, boolean repeated,
      String pattern, List<String> enumValues, Type type, String... values) {
    ApiParameter param = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(param.isRequired()).andReturn(required);
    EasyMock.expect(param.isRepeated()).andReturn(repeated);
    EasyMock.expect(param.getPattern()).andReturn(pattern);
    EasyMock.expect(param.getEnumValues()).andReturn(enumValues);
    EasyMock.expect(param.getType()).andReturn(type);

    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(ImmutableMap.of("param", param));

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(param, method, service);

    ApiRequest request = new ApiRequest(service, "method");
    request.paramValues.putAll("param", Lists.newArrayList(values));

    try {
      request.validate();
      fail("Invalid value given for boolean value, validation passed.");
    } catch (IllegalArgumentException e) {
      assertEquals(expectedErrorMessage, e.getMessage());
    }

    EasyMock.verify(param, method, service);
  }

  /** When an API key is set, it is added as a parameter value. */
  public void testApiKey() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getParameters()).andReturn(null);

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(method, service);

    ApiRequest request = new ApiRequest(service, "method");

    Config.setApiKey("MY_API_KEY");
    request.maybeSetApiKeyParameter();
    assertEquals(Lists.newArrayList("MY_API_KEY"), request.paramValues.get("key"));
    EasyMock.verify();
  }

  /**
   * Tests expected request properties when given a request path and HTTP
   * method.
   */
  public void testCreateRequestWithPath() {
    ApiRequest request = new ApiRequest("/some/path");

    assertEquals("/some/path", request.getRequestPath());
    assertEquals(HttpMethod.GET, request.httpMethod);
    assertNull(request.method);
    assertNull(request.service);
    request.validate(); // Validation passes (and is in fact a no-op)

    // Setting the API key does not make it appear in the query
    Config.setApiKey("MY_API_KEY");
    assertEquals("/some/path", request.getRequestPath());
    // TODO(jasonhall): Test creating a request with query parameters.
  }

  /** Tests proper creation of the Discovery path and error cases. */
  public void testDiscoveryPath() {
    assertEquals("/discovery/v1/apis/service/version/rest",
        ApiServiceFactory.createDiscoveryPath("service", "version"));

    assertIllegalArgument(null, "version", "Service name cannot be null or empty");
    assertIllegalArgument("", "version", "Service name cannot be null or empty");
    assertIllegalArgument("service", null, "Version cannot be null or empty");
    assertIllegalArgument("service", "", "Version cannot be null or empty");
  }

  /**
   * Asserts that an IllegalArgumentException is raised with the expected error
   * message, given the service and version.
   */
  private void assertIllegalArgument(
      String serviceName, String version, String expectedErrorMessage) {
    try {
      ApiServiceFactory.createDiscoveryPath(serviceName, version);
      fail("Illegal argument given, passed precondition. Expected: " + expectedErrorMessage);
    } catch (IllegalArgumentException e) {
      assertEquals(expectedErrorMessage, e.getMessage());
    }
  }

  /**
   * Tests generation of the request path when it is not explicitly set on
   * construction.
   */
  public void testGetRequestPath() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getPath()).andReturn("/path/to/{pathParam}").times(3);

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);
    EasyMock.expect(service.getBasePath()).andReturn("/base").times(3);

    EasyMock.replay(method, service);

    ApiRequest request = new ApiRequest(service, "method");

    try {
      request.getRequestPath();
      fail("Required parameter missing, passed validation.");
    } catch (IllegalArgumentException e) {
      assertEquals(
          "Error generating path URL: Missing parameter value [pathParam]", e.getMessage());
    }

    // Test that the path is generated even when required parameters are not
    // specified, only when client-side validation is disabled.
    request.enableClientSideValidation = false;
    assertEquals("/base/path/to/", request.getRequestPath());
    request.enableClientSideValidation = true;

    // Setting the path param sets it in the path.
    request.paramValues.put("pathParam", "1234");
    assertEquals("/base/path/to/1234", request.getRequestPath());

    // TODO(jasonhall): Test adding a query parameter.

    EasyMock.verify(method, service);
  }
}
