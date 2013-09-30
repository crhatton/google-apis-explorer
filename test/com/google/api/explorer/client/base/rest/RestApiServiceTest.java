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
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.rest.RestApiService.Helper.Factory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

/**
 * Tests for {@link RestApiService}, namely creation of the
 * service/resource/method tree from JSON as if retrieved from Discovery.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class RestApiServiceTest extends TestCase {

  protected final RestApiService service;

  public RestApiServiceTest() {
    this.service = decodeTestJson();
  }

  /**
   * Reads discovery-small.json and parses it into a {@link RestApiService}
   * that can be tested.
   */
  protected RestApiService decodeTestJson() {
    InputStream stream = getClass().getResourceAsStream("discovery-small.json");
    if (stream == null) {
      fail("discovery-small.json missing");
    }
    try {
      Reader reader = new InputStreamReader(stream, Charsets.UTF_8);
      String jsonString = CharStreams.toString(reader);
      stream.close();

      Factory factory = AutoBeanFactorySource.create(Factory.class);
      RestApiService service =
          AutoBeanCodex.decode(factory, RestApiService.class, jsonString).as();
      return service;
    } catch (IOException e) {
      fail("IOException: " + e.getMessage());
    }
    return null;
  }

  /** Tests that basic service information is parsed as expected. */
  public void testService() {
    assertEquals("moderator", service.getName());
    assertEquals("v1", service.getVersion());
    assertEquals("Moderator API", service.getDescription());
    assertEquals("/moderator/v1", service.getBasePath());
    assertEquals(1, service.getResources().keySet().size());
    assertEquals(1, service.getMethods().keySet().size());
  }

  /** Tests that a top-level method is parsed as expected. */
  public void testToplevelMethod() {
    ApiMethod get = service.getMethods().get("get");
    assertEquals("/get/{param}", get.getPath());
    assertEquals(HttpMethod.GET, get.getHttpMethod());

    Schema param = get.getParameters().get("param");
    assertFalse(param.isRequired());
    assertNull(param.getPattern());
  }

  /** Tests that a top-level resource and its method are parsed as expected. */
  public void testResource() {
    RestApiService.ApiResource series = service.getResources().get("series");
    assertEquals(1, series.getMethods().keySet().size());
    assertEquals(1, series.getResources().keySet().size());

    ApiMethod seriesGet = series.getMethods().get("get");
    assertEquals("/series/{seriesId}", seriesGet.getPath());
    assertEquals(HttpMethod.GET, seriesGet.getHttpMethod());

    Schema seriesId = seriesGet.getParameters().get("seriesId");

    // TODO(jasonhall): There is a bug with AutoBeans in JRE where booleans --
    // like isRequired() -- are always false, meaning this always passes
    // validation in JUnit tests. When this bug is fixed, uncomment this line.
    // assertTrue(seriesId.isRequired());
    assertEquals("[^/]+", seriesId.getPattern());
  }

  /** Tests that the nested resource and its method are parsed as expected. */
  public void testNestedResource() {
    RestApiService.ApiResource series = service.getResources().get("series");

    RestApiService.ApiResource my = series.getResources().get("my");
    assertNull(my.getResources());
    assertEquals(1, my.getMethods().keySet().size());

    ApiMethod myGet = my.getMethods().get("get");
    assertEquals("/series/my/{seriesId}", myGet.getPath());
    assertEquals(HttpMethod.GET, myGet.getHttpMethod());
    assertEquals(1, myGet.getParameters().keySet().size());

    Schema seriesId = myGet.getParameters().get("seriesId");
    assertFalse(seriesId.isRequired());
    assertEquals("(foo|bar)", seriesId.getPattern());
  }

  /**
   * Tests getting all methods in a service using
   * {@link RestApiService#allMethods()} vs. digging through the
   * resource/method maps.
   */
  public void testAllMethods() {
    Map<String, ApiMethod> methods = service.allMethods();
    assertEquals(service.getMethods().get("get"), methods.get("moderator.get"));
    assertEquals(service.getResources().get("series").getMethods().get("get"),
        methods.get("moderator.series.get"));
    assertEquals(
        service.getResources().get("series").getResources().get("my").getMethods().get("get"),
        methods.get("moderator.series.my.get"));
    assertEquals(3, methods.keySet().size());
  }

  /**
   * Tests getting a method using {@link RestApiService#method(String)} vs.
   * digging through the resource/method maps, and tests that providing an
   * identifier that resolves to no method returns {@code null}.
   */
  public void testGetMethodsByIdentifier() {
    assertEquals(service.getMethods().get("get"), service.method("moderator.get"));
    assertEquals(service.getResources().get("series").getMethods().get("get"),
        service.method("moderator.series.get"));
    assertEquals(
        service.getResources().get("series").getResources().get("my").getMethods().get("get"),
        service.method("moderator.series.my.get"));

    assertNull(service.method("moderator.nonexistent"));
    assertNull(service.method("moderator.series.foo.nonexistent"));
    assertNull(service.method("moderator.series.foo.bar.nonexistent"));
  }

  /**
   * Tests fetching the API methods which support a given kind.
   */
  public void testGetMethodsWithKind() {
    Collection<ApiMethod> methods = service.usagesOfKind("moderator#type");
    assertEquals(2, methods.size());
    for (ApiMethod method : methods) {
      if (!"moderator.get".equals(method.getId()) && !"moderator.series.my.get".equals(
          method.getId())) {
        fail();
      }
    }
  }
}
