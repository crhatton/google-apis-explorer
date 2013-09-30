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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.mock.MockApiMethod;
import com.google.api.explorer.client.base.mock.MockApiService;
import com.google.api.explorer.client.base.rpc.CustomSchema;
import com.google.api.explorer.client.embedded.RequestBodyForm.BodyEditor;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import java.util.Map;

/**
 * Tests for the request body form editor.
 *
 */
public class RequestBodyFormGwtTest extends BaseGwtTest {

  RequestBodyForm form;
  MockApiService mockService;

  @Override
  public void gwtSetUp() {
    form = new RequestBodyForm();
    mockService = new MockApiService();
  }

  /** Test that the free-form to schema data binding works as expected. */
  public void testDataBinding() {
    Map<String, Schema> properties =
        ImmutableMap.of("oneProp", CustomSchema.lockedStringField(null));
    Schema simpleSchema = CustomSchema.objectSchema(properties, null, false);

    ApiMethod apiMethodWithSchema = new MockApiMethod();
    mockService.schemaForMethod.put(apiMethodWithSchema, simpleSchema);

    ApiMethod apiMethodWithoutSchema = new MockApiMethod();
    mockService.schemaForMethod.put(apiMethodWithoutSchema, null);

    // Set the first method and some data in the editors.
    form.setContent(mockService, apiMethodWithSchema, simpleSchema, "");
    String value = "{\n  \"oneProp\": \"oneValue\"\n}";
    JSONObject data = JSONParser.parseStrict(value).isObject();
    form.schemaForm.setJSONValue(data);

    // Ensure that the data gets copied over.
    assertEquals("", form.requestBody.getText());
    form.showEditor(BodyEditor.FREEFORM, false);
    assertEquals(value, form.requestBody.getText());
    assertEquals(value, form.getRequestBodyText());

    // Ensure that it gets copied back.
    form.schemaForm.setJSONValue(new JSONObject());
    form.showEditor(BodyEditor.SCHEMA, false);
    assertEquals(value, form.schemaForm.getStringValue());
    assertEquals(value, form.getRequestBodyText());

    // Select a new method without a schema and verify the body gets cleared.
    form.setContent(mockService, apiMethodWithSchema, null, "");
    assertEquals("{}", form.requestBody.getText());
    assertEquals("{}", form.getRequestBodyText());
  }
}
