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

  /** Path URL template of this method. */
  String getPath();

  /** HTTP Method that will be used when this method is executed using REST. */
  HttpMethod getHttpMethod();

  /** A short description of what this method does. */
  String getDescription();

  /**
   * Returns a {@link Map} of all {@link ApiParameter}s of this method, keyed by
   * parameter name.
   */
  Map<String, ApiParameter> getParameters();

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
   * {@link List} of auth scope keys (corresponding to keys in {@link
   * ApiService.AuthInformation#getScopes()}) that can be used by this method.
   */
  List<String> getScopes();
}
