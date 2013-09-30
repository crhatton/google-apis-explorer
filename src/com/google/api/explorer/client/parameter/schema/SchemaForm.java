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

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Schema;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * UI for constructing a request body based on the schema of the expected
 * request.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class SchemaForm extends Composite {

  interface SchemaFormUiBinder extends UiBinder<Widget, SchemaForm> {
  }

  public @UiField HTMLPanel root;

  private SchemaEditor editor;
  private ApiMethod method;

  public SchemaForm(UiBinder<Widget, SchemaForm> uiBinder) {
    initWidget(uiBinder.createAndBindUi(this));
  }

  public SchemaForm() {
    this((SchemaFormUiBinder) GWT.create(SchemaFormUiBinder.class));
  }

  /**
   * Returns the JSON string value of the current state of the object shown in
   * this form.
   */
  public String getStringValue() {
    if (editor == null) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    editor.prettyPrint(builder, 0);
    return builder.toString();
  }

  /**
   * Returns the JSON format of the current state of the object.
   */
  public JSONObject getJSONObject() {
    return editor.getJSONValue().isObject();
  }

  /**
   * Set the value of the base editor to the JSON value recursively.
   *
   * @throws JSONException if the JSON can not be applied to the editor cleanly.
   */
  public void setJSONValue(JSONValue value) {
    // May not yet be initialized with a schema.
    if (editor != null) {
      editor.setJSONValue(value);
    }
  }

  /** Sets the {@link Schema} to be displayed in this form. */
  public void setSchema(ApiService service, ApiMethod method,  Schema schema) {
    this.method = method;
    this.editor = getSchemaEditorForSchema(service, schema,
        /* This is the root element and descendats should be nullable, if also patch. */ true);

    root.clear();
    root.add(editor.render(schema));
  }

  private boolean methodIsPatch() {
    return method.getHttpMethod().equals(HttpMethod.PATCH);
  }

  /** Returns the SchemaEditor for the given Property value. */
  SchemaEditor getSchemaEditorForSchema(
      ApiService service, Schema schema, boolean descendantsNullable) {

    Schema dereferenced = schema;

    if (schema.getRef() != null) {
      // Properties of this object are defined elsewhere.
      dereferenced = service.getSchemas().get(schema.getRef());
    }

    SchemaEditor editor;
    if (dereferenced.getType() != null) {
      switch (dereferenced.getType()) {
        case OBJECT:
          editor = new ObjectSchemaEditor(this,
              method.getId(),
              service,
              dereferenced.getProperties(),
              dereferenced.getAdditionalProperties(),
              methodIsPatch() && descendantsNullable);
          break;

        case ARRAY:
          editor = new ArraySchemaEditor(service, this, dereferenced.getItems());
          break;

        case BOOLEAN:
          editor = new BooleanSchemaEditor();
          break;

        case INTEGER:
        case NUMBER:
          editor = new NumberSchemaEditor();
          break;

        case ANY:
        case STRING:
        default:
          editor = new StringSchemaEditor();
      }
    } else {
      editor = new StringSchemaEditor();
    }

    return editor;
  }

  /** Base interface for all schema-based editors. */
  interface SchemaEditor {
    static final String INDENTATION = "  ";

    /** Returns a widget displaying the UI for the user to fill in. */
    Widget render(Schema property);

    /**
     * Returns the JSON value of the single property defined displayed by this
     * editor.
     */
    JSONValue getJSONValue();

    /**
     * Recursively bind the JSON provided to the editors.
     *
     * @throws JSONException if the JSON can not be applied to the editor cleanly.
     */
    void setJSONValue(JSONValue value);

    /**
     * Add results from our calculation on to the existing results.
     */
    void prettyPrint(StringBuilder resultSoFar, int indentation);

    /**
     * Returns whether or not this schema editor is composed of other schema editors.
     */
    boolean isComposite();
  }

  /** Editor for string values. */
  static class StringSchemaEditor implements SchemaEditor {
    private HasText hasText;

    @Override
    public Widget render(Schema property) {
      HTMLPanel panel = new HTMLPanel("");
      panel.getElement().getStyle().setDisplay(Display.INLINE);

      panel.add(new InlineLabel("\""));
      if (property.locked()) {
        InlineLabel label = new InlineLabel();
        panel.add(label);
        hasText = label;
      } else {
        TextArea editor = new TextArea();
        panel.add(editor);
        hasText = editor;
      }
      panel.add(new InlineLabel("\""));

      if (property.getDefault() != null) {
        hasText.setText(property.getDefault());
      }

      return panel;
    }

    @Override
    public JSONValue getJSONValue() {
      return new JSONString(hasText.getText());
    }

    @Override
    public void setJSONValue(JSONValue value) {
      JSONString stringVal = value.isString();
      if (stringVal != null) {
        hasText.setText(stringVal.stringValue());
      } else {
        throw new JSONException("Not a valid JSON string: " + value.toString());
      }
    }

    @Override
    public void prettyPrint(StringBuilder resultSoFar, int indentation) {
      resultSoFar.append(getJSONValue().toString());
    }

    @Override
    public boolean isComposite() {
      return false;
    }
  }

  /** Editor for numerical values. */
  static class NumberSchemaEditor implements SchemaEditor {
    private TextBox textbox;

    @Override
    public Widget render(Schema property) {
      textbox = new TextBox();

      if (property.getDefault() != null) {
        textbox.setValue(property.getDefault());
      }
      return textbox;
    }

    @Override
    public JSONValue getJSONValue() {
      // Try to parse the value as a number.
      double val;
      try {
        val = Double.valueOf(textbox.getValue());
      } catch (NumberFormatException nfe) {
        // If the value is not a number, pass it as a string.
        return new JSONString(textbox.getValue());
      }

      return new JSONNumber(val);
    }

    @Override
    public void setJSONValue(JSONValue value) {
      JSONNumber numberVal = value.isNumber();
      JSONString stringVal = value.isString();
      if (numberVal != null) {
        textbox.setValue(String.valueOf(numberVal.doubleValue()));
      } else if (stringVal != null){
        textbox.setValue(stringVal.stringValue());
      } else {
        throw new JSONException("Not a valid JSON number: " + value.toString());
      }
    }

    @Override
    public void prettyPrint(StringBuilder resultSoFar, int indentation) {
      resultSoFar.append(getJSONValue().toString());
    }

    @Override
    public boolean isComposite() {
      return false;
    }
  }

  /** Editor for boolean values. */
  static class BooleanSchemaEditor implements SchemaEditor {
    private SimpleCheckBox checkbox;

    @Override
    public Widget render(Schema property) {
      checkbox = new SimpleCheckBox();

      // Set it to true if that is the default.
      checkbox.setValue("true".equals(property.getDefault()));

      // If this property is locked, disable the checkbox
      if (property.locked()) {
        checkbox.setEnabled(false);
      }
      return checkbox;
    }

    @Override
    public JSONValue getJSONValue() {
      return JSONBoolean.getInstance(checkbox.getValue());
    }

    @Override
    public void setJSONValue(JSONValue value) {
      JSONBoolean boolVal = value.isBoolean();
      JSONString stringVal = value.isString();
      if (boolVal != null) {
        checkbox.setValue(boolVal.booleanValue());
      } else if (stringVal != null) {
        checkbox.setValue(Boolean.parseBoolean(stringVal.stringValue()));
      } else {
        throw new JSONException("Not a valid JSON boolean: " + value.toString());
      }
    }

    @Override
    public void prettyPrint(StringBuilder resultSoFar, int indentation) {
      resultSoFar.append(getJSONValue().toString());
    }

    @Override
    public boolean isComposite() {
      return false;
    }
  }
}
