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

package com.google.api.explorer.client.parameter.schema;

import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.mock.MockApiMethod;
import com.google.api.explorer.client.base.mock.MockApiService;
import com.google.api.explorer.client.base.rpc.CustomSchema;
import com.google.api.explorer.client.parameter.schema.SchemaForm.StringSchemaEditor;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.json.client.JSONParser;

import java.util.Collections;
import java.util.Map;

/**
 * Tests for the ParameterForm.
 *
 */
public class ObjectSchemaEditorGwtTest extends BaseGwtTest {

  private static final Map<String, Schema> EMPTY_PROPERTIES = Collections.emptyMap();

  SchemaForm schemaForm;

  @Override
  public void gwtSetUp() {
    schemaForm = new SchemaForm();
    schemaForm.setSchema(
        new MockApiService(), new MockApiMethod(), CustomSchema.objectSchema(null, null, false));
  }

  public void testLockedRequiredFields() {
    Schema lockedString = CustomSchema.lockedStringField(null);
    Schema optional = CustomSchema.objectSchema(EMPTY_PROPERTIES, null, false);

    Map<String, Schema> properties = ImmutableMap.of("locked", lockedString, "optional", optional);
    ObjectSchemaEditor objectEditor =
        new ObjectSchemaEditor(schemaForm, null, null, properties, null, false);

    // If we aren't setting a JSON value, we have to render it to trigger the clear().
    objectEditor.render((Schema) null);

    assertEquals(1, objectEditor.editors.size());
    assertEquals(ImmutableSet.of("locked"), objectEditor.editors.keySet());
    assertEquals(ObjectElement.class, objectEditor.editors.get("locked").getClass());
    assertEquals(StringSchemaEditor.class,
        ((ObjectElement) objectEditor.editors.get("locked")).innerEditor.getClass());

    // Ensure that the additionalProperties newItem link is not visible.
    assertEquals(true, objectEditor.listBox.isVisible());
    assertEquals(false, objectEditor.newItem.isVisible());
  }

  public void testRequredNotDuplicated() {
    Schema lockedString = CustomSchema.lockedStringField(null);
    Map<String, Schema> properties = ImmutableMap.of("prop1", lockedString);
    ObjectSchemaEditor objectEditor =
        new ObjectSchemaEditor(schemaForm, null, null, properties, null, false);

    // Initialize a value for this property
    objectEditor.setJSONValue(JSONParser.parseStrict("{\"prop1\": \"a value\"}"));

    assertEquals(1, objectEditor.editors.size());
    assertEquals(ImmutableSet.of("prop1"), objectEditor.editors.keySet());
    assertEquals(ObjectElement.class, objectEditor.editors.get("prop1").getClass());
    assertEquals(StringSchemaEditor.class,
        ((ObjectElement) objectEditor.editors.get("prop1")).innerEditor.getClass());
    assertEquals("\"a value\"", objectEditor.editors.get("prop1").getJSONValue().toString());
  }

  public void testAdditionalProperties() {
    Schema additionalProperties = CustomSchema.objectSchema(EMPTY_PROPERTIES, null, false);
    ObjectSchemaEditor objectEditor = new ObjectSchemaEditor(schemaForm,
        null,
        null,
        EMPTY_PROPERTIES,
        additionalProperties,
        false);
    objectEditor.render((Schema) null);

    assertEquals(0, objectEditor.editors.size());
    assertEquals(0, objectEditor.additionalPropertyEditors.size());
    assertEquals(false, objectEditor.listBox.isVisible());
    assertEquals(true, objectEditor.newItem.isVisible());

    objectEditor.addNewAdditionalEditor(new ClickEvent() {});

    assertEquals(0, objectEditor.editors.size());
    assertEquals(1, objectEditor.additionalPropertyEditors.size());
  }

  public void testSchemaAndAdditionalProperties() {
    Schema object1 = CustomSchema.objectSchema(EMPTY_PROPERTIES, null, false);
    Schema object2 = CustomSchema.objectSchema(EMPTY_PROPERTIES, null, false);

    Map<String, Schema> properties = ImmutableMap.of("obj1", object1, "obj2", object2);

    Schema additionalProperties = CustomSchema.objectSchema(EMPTY_PROPERTIES, null, false);
    ObjectSchemaEditor objectEditor = new ObjectSchemaEditor(schemaForm,
        null,
        null,
        properties,
        additionalProperties,
        false);
    objectEditor.render((Schema) null);

    assertEquals(0, objectEditor.editors.size());
    assertEquals(0, objectEditor.additionalPropertyEditors.size());
    assertEquals(true, objectEditor.listBox.isVisible());
    assertEquals(true, objectEditor.newItem.isVisible());

    objectEditor.onSelect("obj1", /* Not required. */ false);
    objectEditor.onSelect("obj2", /* Not required. */ false);
    objectEditor.addNewAdditionalEditor(new ClickEvent() {});

    assertEquals(2, objectEditor.editors.size());
    assertEquals(1, objectEditor.additionalPropertyEditors.size());
  }
}
