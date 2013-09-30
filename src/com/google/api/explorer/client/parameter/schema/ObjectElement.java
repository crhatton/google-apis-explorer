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

import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.parameter.schema.SchemaForm.SchemaEditor;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Encapsulates the key/value of one key/value pair in an
 * {@link ObjectSchemaEditor}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ObjectElement extends Composite implements SchemaEditor, HasRemovalHandlers {

  private static RowUiBinder uiBinder = GWT.create(RowUiBinder.class);

  interface RowUiBinder extends UiBinder<Widget, ObjectElement> {
  }

  @UiField Panel panel;
  @UiField InlineLabel label;
  @UiField InlineLabel nullText;
  @UiField HTMLPanel placeholder;
  @UiField Image remove;
  @UiField Image info;
  @UiField Label nullify;
  @UiField PopupPanel docsTooltip;
  @UiField PopupPanel patchTooltip;
  @UiField PopupPanel deleteTooltip;
  @UiField Label description;
  @UiField Label required;

  @VisibleForTesting
  final SchemaEditor innerEditor;

  final String key;
  private boolean isNull;
  private boolean isNullable;

  private final HandlerManager handlerManager = new HandlerManager(this);

  /**
   * Creates an instance.
   *
   * @param key Key in the object under which this object can be found.
   * @param editor Editor which should be shown for the value portion of the object element.
   * @param property Schema which is used to represent values of this object element.
   * @param isRemovable Whether or not this object can request to be removed from the object.
   * @param isNullable Whether or not the object can be explicitly set to null.
   */
  public ObjectElement(
      String key, SchemaEditor editor, Schema property, boolean isRemovable, boolean isNullable) {

    initWidget(uiBinder.createAndBindUi(this));
    this.key = key;
    this.innerEditor = editor;
    this.isNullable = isNullable;

    label.setText("\"" + key + "\"");

    info.setVisible(!isRemovable || property.getDescription() != null);
    required.setVisible(!isRemovable);
    remove.setVisible(isRemovable);
    description.setVisible(property.getDescription() != null);
    description.setText(property.getDescription());

    // Default to showing the editor.
    isNull = false;
    updateDisplay();

    placeholder.add(editor.render(property));

    // Initialize the tooltip popup.
    docsTooltip.show();
    docsTooltip.hide();

    // Initialize the patch tooltip popup.
    patchTooltip.show();
    patchTooltip.hide();

    // Initialize the delete tooltip popup.
    deleteTooltip.show();
    deleteTooltip.hide();
  }

  @UiHandler("nullText")
  void activateInnerEditor(ClickEvent event) {
    isNull = false;
    updateDisplay();
  }

  @UiHandler("nullify")
  void nullifyValue(ClickEvent event) {
    isNull = true;
    updateDisplay();
  }

  private void updateDisplay() {
    nullText.setVisible(isNull);
    nullify.setVisible(!isNull && isNullable);
    placeholder.setVisible(!isNull);

    // If the editor is not an Object or Array editor, it can contain inner
    // elements, and will have its own inner remove link to manage them. If
    // that's the case, move the remove link to after the editor.
    if (!innerEditor.isComposite() || isNull) {
      moveControlsAfterEditor();
    } else {
      moveEditorAfterControls();
    }
  }

  @UiHandler("info")
  void discloseInfo(ClickEvent event) {
    EditorHelper.discloseLowerRight(docsTooltip, info);
  }

  @UiHandler("info")
  void discloseInfoHover(MouseOverEvent event) {
    EditorHelper.discloseLowerRight(docsTooltip, info);
  }

  @UiHandler("info")
  void hideInfoHover(MouseOutEvent event) {
    docsTooltip.hide();
  }

  @UiHandler("nullify")
  void disclosePatchInfo(MouseOverEvent event) {
    EditorHelper.discloseLowerRight(patchTooltip, nullify);
  }

  @UiHandler("nullify")
  void disclosePatchInfo(MouseOutEvent event) {
    patchTooltip.hide();
  }

  @UiHandler("remove")
  void removeClicked(ClickEvent event) {
    // Fire the remove event.
    handlerManager.fireEvent(new ClickEvent(){});
  }

  @UiHandler("remove")
  void discloseRemoveHover(MouseOverEvent event) {
    EditorHelper.discloseLowerRight(deleteTooltip, remove);
  }

  @UiHandler("remove")
  void hideRemoveHover(MouseOutEvent event) {
    deleteTooltip.hide();
  }

  private void moveControlsAfterEditor() {
    // By removing these widgets and re-adding them we move them to the end of the object panel.
    panel.remove(info);
    panel.remove(nullify);
    panel.remove(remove);

    // The order that these objects are added determines their order next to the element, and should
    // match the order from the declarative layout.
    panel.add(info);
    panel.add(nullify);
    panel.add(remove);
  }

  private void moveEditorAfterControls() {
    // By removing these widgets and re-adding them we move them to the end of the object panel.
    panel.remove(placeholder);
    panel.add(placeholder);
  }

  @Override
  public Widget render(Schema property) {
    return this;
  }

  @Override
  public JSONValue getJSONValue() {
    return isNull ? JSONNull.getInstance() : innerEditor.getJSONValue();
  }

  @Override
  public void setJSONValue(JSONValue value) {
    if (value.isNull() != null) {
      isNull = true;
    } else {
      isNull = false;
      innerEditor.setJSONValue(value);
    }
    updateDisplay();
  }

  @Override
  public void prettyPrint(StringBuilder resultSoFar, int indentation) {
    if (isNull) {
      resultSoFar.append("null");
    } else {
      innerEditor.prettyPrint(resultSoFar, indentation);
    }
  }

  @Override
  public boolean isComposite() {
    return innerEditor.isComposite();
  }

  @Override
  public HandlerRegistration registerRemoveClickedHandler(ClickHandler handler) {
    return handlerManager.addHandler(ClickEvent.getType(), handler);
  }
}
