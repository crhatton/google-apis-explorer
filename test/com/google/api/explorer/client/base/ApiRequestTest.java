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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import java.util.Map;

/**
 */
public class ApiRequestTest extends TestCase {
  private class MockApiRequest extends ApiRequest {

    public final Map<String, String> addedHeaders = Maps.newHashMap();
    public String apiKey;

    @Override
    public void addHeader(String headerName, String headerValue) {
      addedHeaders.put(headerName, headerValue);
    }

    @Override
    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    @Override
    public Map<String, String> getHeaders() {
      return null;
    }

    @Override
    public HttpMethod getHttpMethod() {
      return null;
    }

    @Override
    public ListMultimap<String, String> getParamValues() {
      return null;
    }

    @Override
    public String getRequestBody() {
      return null;
    }

    @Override
    public String getRequestPath() {
      return null;
    }

    @Override
    public ApiService getService() {
      return null;
    }

    @Override
    public String getApiKey() {
      return apiKey;
    }

    @Override
    public void setTraceParameter(String traceParameter) {
    }

    @Override
    public ApiMethod getMethod() {
      return null;
    }
  }

  /** When an API key is set, it is added as a parameter value. */
  public void testApiKey() {
    MockApiRequest request = new MockApiRequest();
    Config.setApiKey("MY_API_KEY");
    request.maybeSetApiKeyParameter();
    assertEquals("MY_API_KEY", request.apiKey);
  }

  public void testUserAgent() {
    MockApiRequest request = new MockApiRequest();
    request.setHeaders();
    assertTrue(request.addedHeaders.containsKey("X-JavaScript-User-Agent"));
    assertTrue(request.addedHeaders.get("X-JavaScript-User-Agent").equals(ExplorerConfig.APP_NAME));
  }
}
