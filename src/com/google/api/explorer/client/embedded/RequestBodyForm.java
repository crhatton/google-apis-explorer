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

import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.Resources.Css;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.parameter.schema.SchemaForm;
import com.google.api.explorer.client.widgets.DescendantFocusPanel;
import com.google.api.explorer.client.widgets.FocusInHandler;
import com.google.api.explorer.client.widgets.FocusOutHandler;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * Form which shows a request body in an editor that can be used in both structured and freeform
 * mode, with special styling to collapse the editor when it doesn't have focus.
 *
 */
public class RequestBodyForm extends Composite {
  private static Css globalStyle = Resources.INSTANCE.style();

  @VisibleForTesting
  enum BodyEditor {
    SCHEMA,
    FREEFORM,
  }

  interface RequestBodyFormUiBinder extends UiBinder<Widget, RequestBodyForm> {
  }

  interface RequestBodyFormStyle extends CssResource {
    String hiddenControls();
  }

  private BodyEditor selectedEditor;

  @UiField public DescendantFocusPanel requestPanel;

  @UiField(provided = true) SchemaForm schemaForm;
  @UiField TextArea requestBody;
  @UiField Label editorSwitchError;
  @UiField RequestBodyFormStyle style;

  @UiField PushButton switchEditorMenu;
  @UiField PopupPanel menuPopup;
  @UiField PushButton showStructured;
  @UiField PushButton showFreeform;

  @UiHandler("switchEditorMenu")
  void discloseSwitchEditorMenu(ClickEvent event) {
    menuPopup.setPopupPositionAndShow(new PositionCallback() {
      @Override
      public void setPosition(int offsetWidth, int offsetHeight) {
        int left =
            switchEditorMenu.getAbsoluteLeft() + switchEditorMenu.getOffsetWidth() - offsetWidth;
        int top = switchEditorMenu.getAbsoluteTop() + switchEditorMenu.getOffsetHeight();
        menuPopup.setPopupPosition(left, top);
      }
    });
  }

  @UiHandler("showStructured")
  void selectStructuredEditor(ClickEvent event) {
    showEditor(BodyEditor.SCHEMA, /* Focus the editor after switching. */ true);
    menuPopup.hide();
  }

  @UiHandler("showFreeform")
  void selectFreeformEditor(ClickEvent event) {
    showEditor(BodyEditor.FREEFORM, /* Focus the editor after switching. */ true);
    menuPopup.hide();
  }

  /**
   * Create an empty request body form.
   */
  public RequestBodyForm() {
    schemaForm = new SchemaForm();

    initWidget(((RequestBodyFormUiBinder) GWT.create(RequestBodyFormUiBinder.class))
        .createAndBindUi(this));

    requestPanel.addFocusInHandler(new FocusInHandler() {
      @Override
      public void onFocusIn(Event event) {
        // Fade in the style for the controls.
        requestPanel.removeStyleName(style.hiddenControls());
      }
    });

    requestPanel.addFocusOutHandler(new FocusOutHandler() {
      @Override
      public void onFocusOut(Event event) {
        // Fade out the style for the controls
        requestPanel.addStyleName(style.hiddenControls());
      }
    });

    setupEditorDataBinding();

    // Remove the popup from the flow.
    menuPopup.show();
    menuPopup.hide();
  }

  /**
   * Bind the free form and schema based editors together so that when the user switches the data is
   * moved between editors.
   */
  private void setupEditorDataBinding() {
    // Add a handler to clear body error message when the user intends to edit
    // the text.
    requestBody.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        editorSwitchError.setVisible(false);
      }
    });

    requestBody.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        resizeTextArea();
      }
    });

    requestBody.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        resizeTextArea();
      }
    });

    requestBody.addKeyUpHandler(new KeyUpHandler() {

      @Override
      public void onKeyUp(KeyUpEvent event) {
        resizeTextArea();
      }
    });
  }

  /**
   * Returns the text associated with the contents of the editor.
   */
  public String getRequestBodyText() {
    return selectedEditor == BodyEditor.SCHEMA ? schemaForm.getStringValue() : requestBody
        .getText();
  }

  /**
   * Sets the text of the currently selected editor to the value provided.
   *
   * @param value Json or free-form text that should be used to populate the form.
   */
  public void setRequestBodyText(String value) {
    if (selectedEditor == BodyEditor.SCHEMA) {
      try {
        schemaForm.setJSONValue(JSONParser.parseStrict(value));
      } catch (Exception e) {
        showEditor(BodyEditor.FREEFORM, /* Do not focus on this content fill. */ false);
      }
    }

    // This may have been the original selection, or it may have been switched to by a json parsing
    // error or an error when assigning the json to the schema editor.
    if (selectedEditor == BodyEditor.FREEFORM) {
      requestBody.setText(value);
    }
  }

  /**
   * Initialize the body editors based on the schema provided, starting with an
   * empty request.
   *
   * @param service Service which is used to find schemas.
   * @param method Method for which this form is being displayed.
   * @param requestSchema Schema for which to enable the editor of {@code null}
   *        if none.
   * @param initialText Initial text which should populate the editor.
   */
  public void setContent(
      ApiService service, ApiMethod method, Schema requestSchema, String initialText) {

    selectedEditor = null;
    BodyEditor editorToShow;

    if (requestSchema != null) {
      editorToShow = BodyEditor.SCHEMA;
      schemaForm.setSchema(service, method, requestSchema);
    } else {
      editorToShow = BodyEditor.FREEFORM;
    }

    showEditor(editorToShow, /* Do not focus on the content event. */ false);

    String textToShow = Strings.emptyToNull(initialText) == null ? "{}" : initialText;
    setRequestBodyText(textToShow);
  }


  private void resizeTextArea() {
    int rows = requestBody.getVisibleLines();
    while (rows > 1) {
      requestBody.setVisibleLines(--rows);
    }

    while (requestBody.getElement().getScrollHeight()
        > requestBody.getElement().getClientHeight()) {
      requestBody.setVisibleLines(requestBody.getVisibleLines() + 1);
    }
  }


  /**
   * When the user switches tabs, the data is persistent.
   */
  @VisibleForTesting
  void showEditor(BodyEditor editorType, boolean isFocused) {
    String text = getRequestBodyText();

    switch (editorType) {
      case SCHEMA:
        // About to switch to guided view
        if (selectedEditor != BodyEditor.SCHEMA) {
          try {
            // If the user cleared the text, use an empty object
            if (text.isEmpty()) {
              text = "{}";
            }

            schemaForm.setJSONValue(JSONParser.parseStrict(text));
            schemaForm.setVisible(true);
            requestBody.setVisible(false);
            selectedEditor = BodyEditor.SCHEMA;
            showStructured.addStyleName(globalStyle.checked());
            showFreeform.removeStyleName(globalStyle.checked());
            requestPanel.setFocus(isFocused);
          } catch (JSONException e) {
            // If there was an error parsing the JSON abort the switch and show the cause message to
            // the user.
            editorSwitchError.setVisible(true);
            editorSwitchError.setText(e.getMessage());
          }
        }
        break;

      case FREEFORM:
        // About to switch to free form view
        schemaForm.setVisible(false);
        requestBody.setVisible(true);
        requestBody.setText(text);
        resizeTextArea();
        requestBody.setFocus(isFocused);
        selectedEditor = BodyEditor.FREEFORM;
        showStructured.removeStyleName(globalStyle.checked());
        showFreeform.addStyleName(globalStyle.checked());
        break;
    }
  }
}
