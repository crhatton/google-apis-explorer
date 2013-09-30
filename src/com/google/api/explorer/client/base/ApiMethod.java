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

package com.google.api.explorer.client.base;

import java.util.List;
import java.util.Map;

/**
 * Represents a Google API method of a Service.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface ApiMethod {
  /** HTTP method used to call this method. */
  public enum HttpMethod {
    GET, POST, DELETE, PUT, PATCH, HEAD;
  }

  /** A short description of what this method does. */
  String getDescription();

  /**
   * Returns a {@link Map} of all {@link Schema}s of this method, keyed by
   * parameter name.
   */
  Map<String, Schema> getParameters();

  /**
   * {@link List} of parameter keys (corresponding to the keys in
   * {@link #getParameters()}) in the order in which they should be displayed.
   *
   * <p>
   * Only required parameters are included.
   * </p>
   */
  List<String> getParameterOrder();

  /**
   * {@link List} of auth scope keys (corresponding to keys in
   * {@link ApiService.AuthInformation#getScopes()}) that can be used by this
   * method.
   */
  List<String> getScopes();

  /** Path URL template of this method. */
  String getPath();

  /** HTTP Method that will be used when this method is executed. */
  HttpMethod getHttpMethod();

  /**
   * Returns a map containing one key, "$ref", which maps to the schema ID of
   * its request information, or {@code null} if no request body is required.
   */
  Map<String, String> getRequest();

  /**
   * Returns a map containing one key, "$ref", which maps to the schema ID of
   * its response information, or {@code null} if no response is returned.
   */
  Map<String, String> getResponse();

  /**
   * Returns a schema for the response object, JSON-RPC only.
   */
  Map<String, String> getReturns();

  /**
   * Returns the unique name of this method.
   */
  String getId();
}
