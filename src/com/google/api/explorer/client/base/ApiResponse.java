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

import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.Map;

/**
 * Represents a response from a call to an API service.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiResponse {

  /** The body of the response. */
  public final String body;

  /** A {@link Map} of response header keys and values. */
  public final Map<String, String> headers;

  /** HTTP status code of the response. */
  public final int status;

  /** Text of the HTTP status. */
  public final String statusText;

  private ApiResponse(String body, Map<String, String> headers, int status, String statusText) {
    this.body = body;
    this.headers = headers;
    this.status = status;
    this.statusText = statusText;
  }

  /** Instantiates a response from the JS object representation of a response. */
  public static ApiResponse fromData(JavaScriptObject data) {
    DynamicJso jso = data.cast();

    return new ApiResponse(jso.getString("body"), createHeadersMap(jso), jso.getInteger("status"),
        jso.getString("statusText"));
  }

  /**
   * Inspects the headers object of the given JS object and constructs a
   * {@link Map} of its keys and values.
   */
  private static Map<String, String> createHeadersMap(DynamicJso data) {
    DynamicJso headers = data.get("headers");
    JsArrayString keys = headers.keys();
    Map<String, String> headersMap = Maps.newHashMapWithExpectedSize(keys.length());

    for (int i = 0; i < keys.length(); i++) {
      String key = keys.get(i);
      switch (headers.typeofKey(key)) {
        case STRING:
          headersMap.put(key, headers.getString(key));
          break;
        case BOOLEAN:
          headersMap.put(key, String.valueOf(headers.getBoolean(key)));
          break;
        case NUMBER:
          headersMap.put(key, String.valueOf(headers.getInteger(key)));
          break;
        case INTEGER:
          headersMap.put(key, String.valueOf(headers.getDouble(key)));
          break;
      }
    }
    return headersMap;
  }
}
