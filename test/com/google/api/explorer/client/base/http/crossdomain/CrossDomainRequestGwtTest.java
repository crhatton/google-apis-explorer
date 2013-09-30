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

package com.google.api.explorer.client.base.http.crossdomain;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

import java.util.Map;

/**
 * Tests for the {@link CrossDomainRequest} class.
 *
 */
public class CrossDomainRequestGwtTest extends BaseGwtTest {

  private String path = "/discovery/v1/apis";
  private String body = "{}";
  private Map<String, String> headers = ImmutableMap.of("Content-Type", "application/json");
  private HttpMethod method = HttpMethod.POST;


  public void testRequestParameters() {
    ApiRequest req = generateRequest(path, body, headers, method);

    DynamicJso obj = (DynamicJso) CrossDomainRequest.convertRequest(req);

    assertEquals(path, obj.getString("url"));
    assertEquals(body, obj.getString("body"));
    assertEquals(method.name(), obj.getString("httpMethod"));

    String ctHeaderVal = ((DynamicJso) obj.get("headers")).getString("Content-Type");
    assertEquals("application/json", ctHeaderVal);
  }

  /**
   * Test whether setting the base path has an effect. It shouldn't.
   */
  public void testAlternateBase() {
    Config.setBaseUrl("https://www.googleapis.com/alternate/base/path");

    ApiRequest req = generateRequest(path, body, headers, method);

    DynamicJso obj = (DynamicJso) CrossDomainRequest.convertRequest(req);

    assertEquals(path, obj.getString("url"));
    assertEquals(body, obj.getString("body"));
    assertEquals(method.name(), obj.getString("httpMethod"));

    String ctHeaderVal = ((DynamicJso) obj.get("headers")).getString("Content-Type");
    assertEquals("application/json", ctHeaderVal);
  }

  private ApiRequest generateRequest(final String path, final String body,
      final Map<String, String> headers, final HttpMethod method) {

    return new ApiRequest() {

      @Override
      public String getRequestBody() {
        return body;
      }

      @Override
      public String getRequestPath() {
        return path;
      }

      @Override
      public Map<String, String> getHeaders() {
        return headers;
      }

      @Override
      public HttpMethod getHttpMethod() {
        return method;
      }

      @Override
      public void addHeader(String headerName, String headerValue) {
      }

      @Override
      public String getApiKey() {
        return null;
      }

      @Override
      public ListMultimap<String, String> getParamValues() {
        return null;
      }

      @Override
      public ApiService getService() {
        return null;
      }

      @Override
      public void setApiKey(String apiKey) {
      }

      @Override
      public void setTraceParameter(String traceParameter) {
      }

      @Override
      public ApiMethod getMethod() {
        return null;
      }
    };
  }
}
