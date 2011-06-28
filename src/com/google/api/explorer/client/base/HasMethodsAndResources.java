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

import com.google.api.explorer.client.base.ApiService.ApiResource;

import java.util.Map;

/**
 * Generic interface for anything that can contain {@link ApiMethod}s or
 * {@link ApiResource}s.
 *
 * <p>
 * This is the base interface for {@link ApiService} and
 * {@link ApiResource}.
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
interface HasMethodsAndResources {
  /**
   * Returns a map containing the {@link ApiMethod}s directly belonging to
   * this service or resource, keyed by the method's name. or {@code null} if
   * none are specified.
   *
   * <p>
   * Note that this is just *direct* methods, and that the key is not the
   * method's *unique identifier*.
   * </p>
   *
   * <p>
   * To get all the {@link ApiMethod}s belonging to a
   * {@link ApiService}, use {@link ApiService#allMethods()}.
   * </p>
   */
  Map<String, ApiMethod> getMethods();

  /**
   * Returns a map containing the {@link ApiResource}s directly belonging
   * to this service or resource, keyed by the resource's name, or {@code null}
   * if none are specified.
   */
  Map<String, ApiResource> getResources();
}
