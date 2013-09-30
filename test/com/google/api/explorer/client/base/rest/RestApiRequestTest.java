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

package com.google.api.explorer.client.base.rest;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService.CallStyle;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.TestUrlEncoder;
import com.google.api.explorer.client.base.UrlEncoder;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests validation of parameter values and creation of request path in
 * {@link RestApiRequest}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class RestApiRequestTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Config.setApiKey("");
    RestApiRequest.urlEncoder = new TestUrlEncoder();
  }

  @Override
  protected void tearDown() {
    RestApiRequest.urlEncoder = UrlEncoder.DEFAULT;
  }

  /**
   * Tests expected request properties when given a request path and HTTP
   * method.
   */
  public void testCreateRequestWithPath() {
    RestApiRequest request = new RestApiRequest("/some/path");

    assertEquals("/some/path", request.getRequestPath());
    assertEquals(HttpMethod.GET, request.getHttpMethod());
    assertNull(request.method);
    assertNull(request.getService());

    // Setting the API key does not make it appear in the query
    Config.setApiKey("MY_API_KEY");
    assertEquals("/some/path", request.getRequestPath());
    // TODO(jasonhall): Test creating a request with query parameters.
  }

  /** Tests proper creation of the Discovery path and error cases. */
  public void testDiscoveryPath() {
    assertEquals("/discovery/v1/apis/service/version/rest",
        ApiServiceFactory.createDiscoveryPath("service", "version", CallStyle.REST));

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
      ApiServiceFactory.createDiscoveryPath(serviceName, version, CallStyle.REST);
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
    EasyMock.expect(method.getPath()).andReturn("/path/to/{pathParam}").times(4);

    RestApiService service = EasyMock.createControl().createMock(RestApiService.class);
    EasyMock.expect(service.basePath()).andReturn("/base").times(4);

    EasyMock.replay(method, service);

    RestApiRequest request = new RestApiRequest(service, method);

    // Test that the path is generated even when required parameters are not
    // specified.
    assertEquals("/base/path/to/", request.getRequestPath());

    // Setting the path param sets it in the path.
    request.getParamValues().put("pathParam", "1234");
    assertEquals("/base/path/to/1234", request.getRequestPath());

    request.getParamValues().replaceValues("pathParam", ImmutableList.of("12/34"));
    assertEquals("/base/path/to/12%2F34", request.getRequestPath());

    request.getParamValues().put("nonPathParam", "abc/de");
    assertEquals("/base/path/to/12%2F34?nonPathParam=abc%2Fde", request.getRequestPath());

    EasyMock.verify(method, service);
  }

  /**
   * Tests generation of the request path when the first parameter component is
   * at the beginning of the path.
   */
  public void testMultipleComponents() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getPath()).andReturn("{firstParam}/path/to/{secondParam}").times(3);

    RestApiService service = EasyMock.createControl().createMock(RestApiService.class);
    EasyMock.expect(service.basePath()).andReturn("/base").times(3);

    EasyMock.replay(method, service);

    RestApiRequest request = new RestApiRequest(service, method);

    // Test that the path is generated even when required parameters are not
    // specified.
    assertEquals("/base/path/to/", request.getRequestPath());

    // Setting the path param sets it in the path.
    request.getParamValues().put("firstParam", "1234");
    request.getParamValues().put("secondParam", "4567");
    assertEquals("/base/1234/path/to/4567", request.getRequestPath());

    request.getParamValues().put("nonPathParam", "abc/de");
    assertEquals("/base/1234/path/to/4567?nonPathParam=abc%2Fde", request.getRequestPath());

    EasyMock.verify(method, service);
  }

  /**
   * Tests generation of the request path when the entire path is a placeholder.
   */
  public void testLonelyPlaceholder() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getPath()).andReturn("{lonelyParam}").times(3);

    RestApiService service = EasyMock.createControl().createMock(RestApiService.class);
    EasyMock.expect(service.basePath()).andReturn("/base").times(3);

    EasyMock.replay(method, service);

    RestApiRequest request = new RestApiRequest(service, method);

    // Test that the path is generated even when required parameters are not
    // specified.
    assertEquals("/base", request.getRequestPath());

    // Setting the path param sets it in the path.
    request.getParamValues().put("lonelyParam", "1234");
    assertEquals("/base/1234", request.getRequestPath());

    request.getParamValues().put("nonPathParam", "abc/de");
    assertEquals("/base/1234?nonPathParam=abc%2Fde", request.getRequestPath());

    EasyMock.verify(method, service);
  }

  /**
   * Tests repeated parameters.
   */
  public void testRepeated() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getPath()).andReturn("/path/to{/repeatedParam*}").times(4);

    RestApiService service = EasyMock.createControl().createMock(RestApiService.class);
    EasyMock.expect(service.basePath()).andReturn("/base").times(4);

    EasyMock.replay(method, service);

    RestApiRequest request = new RestApiRequest(service, method);

    // Test that the path is generated even when required parameters are not
    // specified.
    assertEquals("/base/path/to/", request.getRequestPath());

    // Setting the path param sets it in the path.
    request.getParamValues().put("repeatedParam", "1234");
    assertEquals("/base/path/to/1234", request.getRequestPath());

    request.getParamValues().replaceValues("repeatedParam", ImmutableList.of("12/34", "567"));
    assertEquals("/base/path/to/12%2F34/567", request.getRequestPath());

    request.getParamValues().put("nonPathParam", "abc/de");
    assertEquals("/base/path/to/12%2F34/567?nonPathParam=abc%2Fde", request.getRequestPath());

    EasyMock.verify(method, service);
  }
}
