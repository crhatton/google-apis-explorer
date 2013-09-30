/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.api.explorer.client.base.mock;

import com.google.api.explorer.client.base.ApiDirectory.Icons;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition.Label;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Schema;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of {@link ApiService} used for GWT tests.
 *
 */
public class MockApiService implements ApiService {

  public final Map<ApiMethod, Schema> schemaForMethod = Maps.newHashMap();

  @Override
  public Map<String, ApiMethod> allMethods() {
    return null;
  }

  @Override
  public String basePath() {
    return null;
  }

  @Override
  public CallStyle callStyle() {
    return CallStyle.REST;
  }

  @Override
  public Map<String, AuthInformation> getAuth() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Map<String, Schema> getParameters() {
    return null;
  }

  @Override
  public Map<String, Schema> getSchemas() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public ApiMethod method(String methodIdentifier) {
    return null;
  }

  @Override
  public Schema requestSchema(ApiMethod method) {
    return schemaForMethod.get(method);
  }

  @Override
  public Schema responseSchema(ApiMethod method) {
    return null;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public Icons getIcons() {
    return null;
  }

  @Override
  public String getDocumentationLink() {
    return null;
  }

  @Override
  public Set<Label> getLabels() {
    return null;
  }

  @Override
  public String displayTitle() {
    return null;
  }

  @Override
  public ApiMethod resolveMethod(String oldMethodIdentifier) {
    return null;
  }

  @Override
  public Collection<ApiMethod> usagesOfKind(String kind) {
    return null;
  }
}
