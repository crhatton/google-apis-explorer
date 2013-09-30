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
  /**
   * Class to store a key and value of a header.
   *
   */
  public static class HeaderValue {
    final String key;
    final String value;

    /**
     * Create a new header with the specified key and value.
     */
    public HeaderValue(String key, String value) {
      this.key = key;
      this.value = value;
    }

    /**
     * Returns the key.
     */
    public String getKey() {
      return key;
    }

    /**
     * Returns the value.
     */
    public String getValue() {
      return value;
    }
  }

  /** The response object */
  private DynamicJso object;

  /**
   * A {@link Map} of normalized lower case response header keys to tuples
   * containing the original key and the value.
   */
  private final Map<String, HeaderValue> headers;

  private ApiResponse(DynamicJso object) {
    this.object = object;
    this.headers = createHeadersMap(object);
  }

  /** Returns the value of the body element as a String. */
  public String getBodyAsString() {
    return object.getString("body");
  }

  /** Returns the status code of the response. */
  public int getStatus() {
    return object.getInteger("status");
  }

  /** Returns the text associated with the status code. */
  public String getStatusText() {
    return object.getString("statusText");
  }

  /**
   * Returns a map of normalized lower case header keys, associated with a tuple
   * containing the original key and the value.
   */
  public Map<String, HeaderValue> getHeaders() {
    return headers;
  }

  /** Instantiates a response from the JS object representation of a response. */
  public static ApiResponse fromData(JavaScriptObject data) {
    DynamicJso jso = data.cast();

    return new ApiResponse(jso);
  }

  /**
   * Inspects the headers object of the given JS object and constructs a
   * {@link Map} of its keys and values.
   */
  private static Map<String, HeaderValue> createHeadersMap(DynamicJso data) {
    DynamicJso headers = data.get("headers");
    JsArrayString keys = headers.keys();
    Map<String, HeaderValue> headersMap = Maps.newHashMapWithExpectedSize(keys.length());

    for (int i = 0; i < keys.length(); i++) {
      String key = keys.get(i);
      String value = "";
      switch (headers.typeofKey(key)) {
        case STRING:
          value = headers.getString(key);
          break;

        case BOOLEAN:
          value = String.valueOf(headers.getBoolean(key));
          break;

        case NUMBER:
          value = String.valueOf(headers.getInteger(key));
          break;

        case INTEGER:
          value = String.valueOf(headers.getDouble(key));
          break;
      }
      headersMap.put(key.toLowerCase(), new HeaderValue(key, value));

    }
    return headersMap;
  }
}
