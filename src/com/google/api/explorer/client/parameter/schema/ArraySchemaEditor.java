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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * {@link SchemaEditor} for array values. The elements of the array will each
 * have their own editors which provide the string value of this editor.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
class ArraySchemaEditor extends Composite implements SchemaEditor {

  private static ArraySchemaEditorUiBinder uiBinder = GWT.create(ArraySchemaEditorUiBinder.class);

  interface ArraySchemaEditorUiBinder extends UiBinder<Widget, ArraySchemaEditor> {
  }

  private final SchemaForm schemaForm;
  private final Schema items;
  private final List<SchemaEditor> editors = Lists.newArrayList();
  private final ApiService service;

  @UiField HTMLPanel panel;
  @UiField Image newItem;
  @UiField PopupPanel addTooltip;

  ArraySchemaEditor(ApiService service, SchemaForm schemaForm, Schema items) {
    initWidget(uiBinder.createAndBindUi(this));
    this.schemaForm = schemaForm;
    this.items = items;
    this.service = service;

    // Initialize the popup panels.
    addTooltip.show();
    addTooltip.hide();
  }

  @UiHandler("newItem")
  void newItem(ClickEvent event) {
    addItem();
  }

  @UiHandler("newItem")
  void discloseAddTooltip(MouseOverEvent event) {
    EditorHelper.discloseLowerRight(addTooltip, newItem);
  }

  @UiHandler("newItem")
  void hideAddTooltip(MouseOutEvent event) {
    addTooltip.hide();
  }

  @Override
  public Widget render(Schema ignored) {
    return this;
  }

  private void addItem() {
    // Get the correct editor to show for the type of array element.
    final SchemaEditor editor = schemaForm.getSchemaEditorForSchema(service, items,
        /* This is an array element, so descendants should not be nullable. */ false);

    // Render the widget and make an ArrayElement widget out of it
    final ArrayElement el = new ArrayElement(editor, items);
    editors.add(el);

    el.registerRemoveClickedHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // When the element is removed in the UI, remove it from the list of
        // editors we care about.
        panel.remove(el);
        editors.remove(editor);
      }
    });
    panel.add(el);
  }

  @Override
  public JSONValue getJSONValue() {
    JSONArray arr = new JSONArray();
    for (int i = 0; i < editors.size(); i++) {
      arr.set(i, editors.get(i).getJSONValue());
    }
    return arr;
  }

  @Override
  public void setJSONValue(JSONValue value) {
    JSONArray arr = value.isArray();
    if (arr != null) {
      for (int i = 0; i < arr.size(); i++) {
        // We may have to create additional editors
        if (i >= editors.size()) {
          addItem();
        }

        SchemaEditor editor = editors.get(i);
        editor.setJSONValue(arr.get(i));
      }
    } else {
      throw new JSONException("Not a valid JSON array: " + value.toString());
    }
  }

  @Override
  public void prettyPrint(StringBuilder resultSoFar, int indentation) {
    resultSoFar.append("\n").append(Strings.repeat(INDENTATION, indentation)).append("[");
    boolean first = true;
    for (SchemaEditor editor : editors) {
      if (!first) {
        resultSoFar.append(",");
      }
      first = false;
      editor.prettyPrint(resultSoFar, indentation + 1);
    }
    resultSoFar.append("\n").append(Strings.repeat(INDENTATION, indentation)).append("]");
  }

  @Override
  public boolean isComposite() {
    return true;
  }
}
