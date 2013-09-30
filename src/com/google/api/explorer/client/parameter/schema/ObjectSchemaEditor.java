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

import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.parameter.schema.SchemaForm.SchemaEditor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * {@link SchemaEditor} for object values. The keys/values of the object will
 * have their own editors which will provide the string value of this editor.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
class ObjectSchemaEditor extends Composite implements SchemaEditor {

  private static ObjectSchemaEditorUiBinder uiBinder = GWT.create(ObjectSchemaEditorUiBinder.class);

  interface ObjectSchemaEditorUiBinder extends UiBinder<Widget, ObjectSchemaEditor> {
  }

  private static final String ADD_PROPERTY = "-- add a property --";
  private static final String EMPTY_INITIAL_KEY = "";
  private static final boolean REQUIRED_PROPERTY = true;
  private static final boolean OPTIONAL_PROPERTY = false;


  private final SchemaForm schemaForm;
  private final Map<String, Schema> properties;
  private final ApiService service;
  private final List<String> availableKeys = Lists.newArrayList();
  private final String methodName;
  private final Schema additionalPropertiesType;
  private final boolean nullableValues;

  @VisibleForTesting
  final Map<String, SchemaEditor> editors = Maps.newHashMap();

  @VisibleForTesting
  final Set<AdditionalPropertyElement> additionalPropertyEditors = Sets.newHashSet();

  @UiField
  ListBox listBox;
  @UiField
  HTMLPanel panel;
  @UiField
  Label newItem;

  @UiHandler("newItem")
  void addNewAdditionalEditor(ClickEvent event) {
    addAdditionalPropertyEditor(EMPTY_INITIAL_KEY);
  }

  ObjectSchemaEditor(SchemaForm schemaForm,
      String methodName,
      ApiService service,
      Map<String, Schema> properties,
      @Nullable Schema additionalPropertiesType,
      boolean nullableValues) {

    initWidget(uiBinder.createAndBindUi(this));
    this.schemaForm = schemaForm;
    this.properties = Objects.firstNonNull(properties, Collections.<String, Schema>emptyMap());
    this.service = service;
    this.methodName = methodName;
    this.additionalPropertiesType = additionalPropertiesType;
    this.nullableValues = nullableValues;

    newItem.setVisible(additionalPropertiesType != null);
    listBox.setVisible(!this.properties.isEmpty());
  }

  @Override
  public Widget render(Schema ignored) {
    clear();
    return this;
  }

  public void clear() {
    panel.clear();
    editors.clear();
    additionalPropertyEditors.clear();
    availableKeys.clear();
    availableKeys.addAll(properties.keySet());
    Collections.sort(availableKeys);

    // Iterate over properties in this object inspecting its annotations.
    // Annotations tell us whether the parameter is required, or immutable.
    for (Map.Entry<String, Schema> entry : properties.entrySet()) {
      boolean required = entry.getValue().requiredForMethod(methodName)
          || entry.getValue().isRequired();
      boolean immutable = !entry.getValue().mutableForMethod(methodName);

      if (required) {
        // Add all required fields for the selected method to the object form.
        onSelect(entry.getKey(), REQUIRED_PROPERTY);
      }
      // TODO(jasonhall): Check if the property is immutable and remove it from
      // availableKeys, when Discovery contains this information.
    }

    buildListBox();
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        onSelect(null, OPTIONAL_PROPERTY);
      }
    });
    listBox.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          onSelect(null, OPTIONAL_PROPERTY);
        }
      }
    });
  }

  @Override
  public JSONValue getJSONValue() {
    JSONObject obj = new JSONObject();

    for (Map.Entry<String, SchemaEditor> entry : allEditors()) {
      obj.put(entry.getKey(), entry.getValue().getJSONValue());
    }
    return obj;
  }

  @Override
  public void setJSONValue(JSONValue value) {
    JSONObject obj = value.isObject();
    if (obj == null) {
      // If this object came as a json blob, we might have to deserialize it
      JSONString str = value.isString();
      JSONValue parsed = null;
      try {
        parsed = JSONParser.parseStrict(str.stringValue());
      } catch (Exception e) {
        // There was an error parsing, just leave parsed as null
      }
      JSONObject parsedObject = parsed != null ? parsed.isObject() : null;
      if (parsedObject != null) {
        obj = parsed.isObject();
      }
    }

    if (obj != null) {
      // Clear the editor before we start adding the keys back in
      clear();

      // For each key that we are going to map we have to instantiate an
      // appropriate editor type. The {@link #onSelect(String)} function
      // instantiates the proper editor type for the key and binds the new
      // editor to our editor.
      for (String key : obj.keySet()) {
        if (properties.containsKey(key)) {
          SchemaEditor editor = onSelect(key, OPTIONAL_PROPERTY);
          editor.setJSONValue(obj.get(key));
        } else if (additionalPropertiesType != null){
          SchemaEditor editor = addAdditionalPropertyEditor(key);
          editor.setJSONValue(obj.get(key));
        } else {
          throw new JSONException("JSON object contains unknown key: " + key);
        }
      }
    } else {
      throw new JSONException("Invalid JSON object: " + value.toString());
    }
  }

  @Override
  public void prettyPrint(StringBuilder resultSoFar, int indentation) {
    if (resultSoFar.length() > 0) {
      resultSoFar.append("\n");
    }
    resultSoFar.append(Strings.repeat(INDENTATION, indentation)).append("{");
    boolean first = true;

    // Add the properties with fixed keys.
    for (Map.Entry<String, SchemaEditor> entry : allEditors()) {
      if (!first) {
        resultSoFar.append(",");
      }
      first = false;

      resultSoFar
          .append("\n")
          .append(Strings.repeat(INDENTATION, indentation + 1))
          .append("\"")
          .append(entry.getKey())
          .append("\": ");

      entry.getValue().prettyPrint(resultSoFar, indentation + 1);
    }

    resultSoFar.append("\n").append(Strings.repeat(INDENTATION, indentation)).append("}");
  }

  @VisibleForTesting
  SchemaEditor onSelect(String key, boolean isRequired) {
    // Selecting the first item in the list (a placeholder) has no effect.
    if (listBox.getSelectedIndex() == 0 && key == null) {
      return null;
    }

    // There may already be an editor for this key, if so just return it.
    if (editors.containsKey(key)) {
      return editors.get(key);
    }

    String selectedKey = key == null ? listBox.getValue(listBox.getSelectedIndex()) : key;
    Schema selectedProperty = properties.get(selectedKey);

    SchemaEditor editor = schemaForm.getSchemaEditorForSchema(service, selectedProperty,
        /* Descendants inherit nullability. */ nullableValues);

    boolean isRemovable = !isRequired && !selectedProperty.locked();
    final ObjectElement row =
        new ObjectElement(selectedKey, editor, selectedProperty, isRemovable, nullableValues);
    panel.add(row);
    editors.put(selectedKey, row);

    // Remove the selected key from the listbox.
    availableKeys.remove(selectedKey);
    for (int i = 1; i < listBox.getItemCount(); i++) {
      if (listBox.getItemText(i).equals(selectedKey)) {
        listBox.removeItem(i);
        break;
      }
    }

    // If there aren't any keys left, hide the listbox.
    if (availableKeys.isEmpty()) {
      listBox.setVisible(false);
    }

    // When a row is removed, re-add its key to the list of available keys.
    row.registerRemoveClickedHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        panel.remove(row);
        editors.remove(row.key);

        availableKeys.add(row.key);
        Collections.sort(availableKeys);

        buildListBox();
      }
    });

    return row;
  }

  private SchemaEditor addAdditionalPropertyEditor(String initialKeyValue) {
    SchemaEditor editor = schemaForm.getSchemaEditorForSchema(service, additionalPropertiesType,
        /* Descendants inherit nullability. */ nullableValues);
    final AdditionalPropertyElement row =
        new AdditionalPropertyElement(editor, additionalPropertiesType);

    // If the editor was created with an initial key, set it now.
    row.setKeyValue(Preconditions.checkNotNull(initialKeyValue));

    // Add our new components to the parent object and editor list.
    panel.add(row);
    additionalPropertyEditors.add(row);

    row.remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        panel.remove(row);
        additionalPropertyEditors.remove(row);
      }
    });

    return editor;
  }

  /**
   * Resets the listbox to contain all keys in availableKeys, and the
   * placeholder, and sets the listbox visible.
   */
  private void buildListBox() {
    listBox.clear();
    listBox.addItem(ADD_PROPERTY);

    // In some cases, all keys will be required
    if (!availableKeys.isEmpty()) {
      for (String key : availableKeys) {
        listBox.addItem(key);
      }
      listBox.setVisible(true);
    }
  }

  private Iterable<Map.Entry<String, SchemaEditor>> allEditors() {
    // Transform that map by extracting the editors.
    Iterable<Map.Entry<String, SchemaEditor>> keysToEditors =
        Iterables.transform(additionalPropertyEditors, AdditionalPropertyElement.normalizeEditor);

    // Concatenate with the named editors.
    return Iterables.concat(keysToEditors, editors.entrySet());
  }

  @Override
  public boolean isComposite() {
    return true;
  }
}
