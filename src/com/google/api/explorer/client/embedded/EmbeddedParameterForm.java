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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.editors.Editor;
import com.google.api.explorer.client.editors.EditorFactory;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.RequestFinishedCallback;
import com.google.api.explorer.client.parameter.schema.FieldsEditor;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Nullable;

/**
 * View of the parameter form UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedParameterForm extends Composite
    implements EmbeddedParameterFormPresenter.Display {

  interface EmbeddedParameterFormStyle extends CssResource {
    String selected();

    String parameterInput();
  }

  @UiField EmbeddedParameterFormStyle style;

  private static EmbeddedParameterFormUiBinder embeddedUiBinder =
      GWT.create(EmbeddedParameterFormUiBinder.class);

  @UiTemplate("EmbeddedParameterForm.ui.xml")
  interface EmbeddedParameterFormUiBinder extends UiBinder<Widget, EmbeddedParameterForm> {
  }

  @UiField public Label requiredDescriptionLabel;

  @UiField public FlexTable table;
  @UiField public Label emptyNotificationLabel;
  @UiField public FlowPanel requiredDescription;
  @UiField public Button submit;

  @UiField public PopupPanel fieldsPopupPanel;
  @UiField public HTMLPanel fieldsPlaceholder;
  @UiField public Button closeFields;

  protected final FlexCellFormatter cellFormatter;
  protected TextBox fieldsTextBox = new TextBox();
  protected FieldsEditor fieldsEditor;

  private final EmbeddedParameterFormPresenter presenter;
  private RequestBodyForm bodyForm;

  /**
   * Bi-directional mapping between parameter name <-> editor responsible for
   * providing that parameter's value.
   */
  protected BiMap<String, Editor> nameToEditor = HashBiMap.create();

  public EmbeddedParameterForm(AuthManager authManager, RequestFinishedCallback callback) {
    bodyForm = new RequestBodyForm();

    initWidget(embeddedUiBinder.createAndBindUi(this));

    this.presenter = new EmbeddedParameterFormPresenter(authManager, this, callback);
    cellFormatter = table.getFlexCellFormatter();

    fieldsPopupPanel.show();
    fieldsPopupPanel.hide();
  }

  public EmbeddedParameterFormPresenter getPresenter() {
    return presenter;
  }

  @UiHandler("submit")
  public void submit(ClickEvent event) {
    presenter.submit();
  }

  @UiHandler("closeFields")
  public void closeFields(ClickEvent event) {
    fieldsPopupPanel.hide();
    fieldsTextBox.setText(fieldsEditor.genFieldsString());
  }

  /** Sets the parameters displayed in the table. */
  @Override
  public void setMethod(ApiService service, ApiMethod method,
      SortedMap<String, Schema> sortedParams, Multimap<String, String> params, String bodyText) {

    Preconditions.checkNotNull(sortedParams);

    // Reset the state of the form.
    setVisible(true);
    requiredDescription.setVisible(false);
    nameToEditor.clear();

    // Reset the table's contents, clear it out.
    table.clear(true);
    while (table.getRowCount() > 0) {
      table.removeRow(table.getRowCount() - 1);
    }

    // Add an editor row for each parameter in the method.
    int row = 0;
    for (Map.Entry<String, Schema> entry : sortedParams.entrySet()) {
      String paramName = entry.getKey();
      Schema param = entry.getValue();
      addEditorRow(paramName, param, row++);
    }

    // Add a row for the fields parameter if there is a response object
    Schema responseSchema = service.responseSchema(method);
    if (responseSchema != null) {
      addEmbeddedFieldsRow(service, responseSchema, row++);
    }

    Schema requestSchema = service.requestSchema(method);
    bodyForm.setContent(service, method, requestSchema, bodyText);

    // (Maybe) add row for request body editor.
    boolean canHaveRequestBody = requestSchema != null;
    String requestParameterText = (method.getHttpMethod() == HttpMethod.PATCH) ? "Patch body"
        : "Request body";
    if (canHaveRequestBody) {
      addRequestBodyRow(row++, requestParameterText);
    }

    // Add a label informing the user that there are no parameters if there are none
    boolean parameterFormIsEmpty =
        sortedParams.isEmpty() && requestSchema == null && responseSchema == null;
    if (parameterFormIsEmpty) {
      addEmptyParametersNotificationRow(row++);
    }
    emptyNotificationLabel.setVisible(parameterFormIsEmpty);

    addExecuteRow(row++);

    // Fill in any pre-filled request parameters.
    setParameterValues(params);
  }

  /**
   * Adds a row to the table containing the parameter name, whether it is
   * required, and an {@link Editor} to provide a value.
   */
  private void addEditorRow(String paramName, Schema param, int row) {
    // First cell in row displays the parameter name and whether the parameter
    // is required.
    boolean required = param.isRequired();
    table.setText(row, 0, paramName);
    if (required) {
      requiredDescription.setVisible(true);
      cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().requiredParameter());
    }

    // Second cell in row displays the editor for the parameter value.
    Editor editor = EditorFactory.forParameter(param);
    nameToEditor.put(paramName, editor);

    Widget editorWidget = editor.createAndSetView().asWidget();
    editorWidget.addStyleName(style.parameterInput());
    table.setWidget(row, 1, editorWidget);

    // Third cell in row displays the description.
    table.setText(row, 2, EmbeddedParameterFormPresenter.generateDescriptionString(param));

    if (paramName.equals("alt")) {
      editor.setValue(ImmutableList.of("json"));
    }

    cellFormatter.addStyleName(row, 0,
        EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 1,
        EmbeddedResources.INSTANCE.style().parameterFormEditorCell());
    cellFormatter.addStyleName(row, 2,
        EmbeddedResources.INSTANCE.style().parameterFormDescriptionCell());
  }

  /**
   * Adds a row to the table to edit the partial fields mask.
   *
   * @param responseSchema Definition of the response object being described.
   * @param row Row index to begin adding rows to the parameter form table.
   */
  private void addEmbeddedFieldsRow(ApiService service, @Nullable Schema responseSchema, int row) {
    fieldsPlaceholder.clear();

    table.setText(row, 0, "fields");

    // Reset the fields textbox's value to empty and add it to the table (with
    // appropriate styling)
    fieldsTextBox.setText("");

    // All inputs must be wrapped in a container to simplify the CSS.
    Widget container = new SimplePanel(fieldsTextBox);
    container.addStyleName(style.parameterInput());
    table.setWidget(row, 1, container);

    // Start adding the next cell which will have the description of this param,
    // and potentially a link to open the fields editor.
    HTMLPanel panel = new HTMLPanel("");

    service.getParameters().get("fields").getDescription();
    panel.add(new Label(getFieldsDescription(service)));

    // If a response schema is provided, add a link to the fields editor and
    // tell the fields editor about this method's response schema.
    if (responseSchema != null && responseSchema.getProperties() != null) {
      Label openFieldsEditor = new InlineLabel("Use fields editor");
      openFieldsEditor.addStyleName(Resources.INSTANCE.style().clickable());
      openFieldsEditor.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          fieldsPopupPanel.show();
          fieldsPopupPanel.center();
        }
      });
      panel.add(openFieldsEditor);

      fieldsEditor = new FieldsEditor(service, /* This is the root, no field name req'd */"");
      fieldsEditor.setProperties(responseSchema.getProperties());
      fieldsPlaceholder.add(fieldsEditor);
    }

    // Add the description (and maybe fields editor link) to the table.
    table.setWidget(row, 2, panel);

    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 1,
        EmbeddedResources.INSTANCE.style().parameterFormEditorCell());
    cellFormatter.addStyleName(row, 2,
        EmbeddedResources.INSTANCE.style().parameterFormDescriptionCell());
  }

  /** Returns the description of the global "fields" parameter, if it exists. */
  protected String getFieldsDescription(ApiService service) {
    Map<String, Schema> parameters = service.getParameters();
    if (parameters == null) {
      return "";
    }

    Schema parameter = parameters.get("fields");
    if (parameter == null) {
      return "";
    }

    return Strings.nullToEmpty(parameter.getDescription());
  }

  private void addRequestBodyRow(int row, String parameterText) {
    table.setText(row, 0, parameterText);
    table.setWidget(row, 1, bodyForm);

    cellFormatter.setColSpan(row, 1, 2);
    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 1,
        EmbeddedResources.INSTANCE.style().parameterFormEditorCell());

  }

  private void addExecuteRow(int row) {
    table.setWidget(row, 0, requiredDescription);
    table.setWidget(row, 1, this.submit);
    table.setText(row, 2, "");
    requiredDescriptionLabel.addStyleName(EmbeddedResources.INSTANCE.style().requiredParameter());

    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 1,
        EmbeddedResources.INSTANCE.style().parameterFormEditorCell());
  }

  private void addEmptyParametersNotificationRow(int row) {
    table.setText(row, 0, "");
    table.setWidget(row, 1, emptyNotificationLabel);
    table.setText(row, 2, "");
  }

  /**
   * Enables/disables the "Execute" button.
   */
  @Override
  public void setExecuting(boolean executing) {
    submit.setEnabled(!executing);
  }

  /** Return a {@link Map} of parameter keys to values as specified by the user. */
  @Override
  public Multimap<String, String> getParameterValues() {
    Multimap<String, String> values = ArrayListMultimap.create();
    for (Map.Entry<String, Editor> entry : nameToEditor.entrySet()) {
      Editor editor = entry.getValue();
      editor.displayValidation();
      values.putAll(entry.getKey(), editor.getValue());
    }

    String fields = this.fieldsTextBox.getText();
    if (!fields.isEmpty()) {
      values.put("fields", fields);
    }
    return values;
  }

  private void setParameterValues(Multimap<String, String> paramValues) {
    if (paramValues != null && !paramValues.isEmpty()) {
      for (String key : paramValues.keySet()) {
        if (nameToEditor.containsKey(key)) {
          nameToEditor.get(key).setValue(Lists.newArrayList(paramValues.get(key)));
        }
      }
    }
  }

  @Override
  public String getBodyText() {
    return bodyForm.isAttached() ? bodyForm.getRequestBodyText() : "";
  }

  /**
   * Generate a String description in the form of:
   * <ul>
   * <li>Description, if available</li>
   * <li>Open parenthesis, then lowercase type, e.g., "(string"</li>
   * <li>Minimum and maximum, if available and within bounds, in the form of one of:
   * <ul>
   * <li>2 - 10</li>
   * <li>2+</li>
   * <li>max 10</li>
   * </ul>
   * </li>
   * <li>Close paranthesis</li>
   */
  public static String generateDescriptionString(Schema param) {
    StringBuilder sb = new StringBuilder();
    String description = param.getDescription();
    String minimum = param.getMinimum();
    String maximum = param.getMaximum();

    // Don't bother displaying "0-4294967295" and just display "0+"
    if (maximum != null && maximum.length() >= "4294967295".length()) {
      maximum = null;
    }
    // Likewise, don't bother displaying "-4294867295-0" and just
    // display "max 0"
    if (minimum != null && minimum.length() >= "-4294867295".length()) {
      minimum = null;
    }

    if (description != null) {
      sb.append(description).append(' ');
    }
    sb.append('(').append(param.getType().name().toLowerCase());
    if (minimum != null || maximum != null) {
      sb.append(", ");
    }
    if (minimum != null) {
      if (maximum != null) {
        sb.append(minimum).append('-').append(maximum);
      } else {
        sb.append(minimum).append("+");
      }
    } else if (maximum != null) {
      sb.append("max ").append(maximum);
    }
    sb.append(')');
    return sb.toString();
  }
}
