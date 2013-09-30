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
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Encapsulates one element in an {@link ArraySchemaEditor}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ArrayElement extends Composite implements SchemaEditor, HasRemovalHandlers {

  private static ArrayElementUiBinder uiBinder = GWT.create(ArrayElementUiBinder.class);

  interface ArrayElementUiBinder extends UiBinder<Widget, ArrayElement> {
  }

  @UiField Image remove;
  @UiField HTMLPanel placeholder;
  @UiField PopupPanel deleteTooltip;

  private final HandlerManager handlerManager = new HandlerManager(this);
  private final SchemaEditor innerEditor;

  public ArrayElement(SchemaEditor innerEditor, Schema property) {
    initWidget(uiBinder.createAndBindUi(this));
    placeholder.add(innerEditor.render(property));
    this.innerEditor = innerEditor;

    // Initialize the popup panels.
    deleteTooltip.show();
    deleteTooltip.hide();
  }

  @UiHandler("remove")
  void fireRemoveEvent(ClickEvent event) {
    // Repackage the click event on the remove button as an event sent to the user supplied handler.
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

  @Override
  public HandlerRegistration registerRemoveClickedHandler(ClickHandler handler) {
    return handlerManager.addHandler(ClickEvent.getType(), handler);
  }

  @Override
  public Widget render(Schema property) {
    return this;
  }

  @Override
  public JSONValue getJSONValue() {
    return innerEditor.getJSONValue();
  }

  @Override
  public void setJSONValue(JSONValue value) {
    innerEditor.setJSONValue(value);
  }

  @Override
  public void prettyPrint(StringBuilder resultSoFar, int indentation) {
    innerEditor.prettyPrint(resultSoFar, indentation);
  }

  @Override
  public boolean isComposite() {
    return innerEditor.isComposite();
  }
}
