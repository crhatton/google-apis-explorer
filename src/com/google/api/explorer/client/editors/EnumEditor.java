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

package com.google.api.explorer.client.editors;

import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.editors.Validator.ValidationResult;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.List;

/**
 * Implementation of an {@link Editor} that suggests valid values to the user.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EnumEditor extends Editor {

  /** Values that will be suggested to the user. */
  final List<String> enumValues;

  /** Descriptions of the values that will be suggested to the user. */
  final List<String> enumDescriptions;
  EditorView view;

  EnumEditor(List<String> enumValues, List<String> enumDescriptions) {
    this.enumValues = enumValues;
    this.enumDescriptions = enumDescriptions;
    this.addValidator(new EnumValidator());
  }

  @Override
  public EditorView createAndSetView() {
    EnumEditorViewImpl viewImpl = new EnumEditorViewImpl(enumValues, enumDescriptions);
    setView(viewImpl);
    return viewImpl;
  }

  /**
   * Validates that the parameter value is one of the defined valid enum values.
   */
  public class EnumValidator implements Validator {
    @Override
    public ValidationResult isValid(List<String> values) {
      for (String value : values) {
        if (!value.isEmpty() && !enumValues.contains(value)) {
          return SimpleValidationResult.createError("Must be one of the specified values.");
        }
      }
      return SimpleValidationResult.STATUS_VALID;
    }
  }

  /**
   * {@link EditorView} implementation for displaying enum values using a
   * {@link SuggestBox}.
   */
  static class EnumEditorViewImpl extends FlowPanel implements EditorView {

    private SuggestBox suggestBox;
    private Label errorMessage;

    EnumEditorViewImpl(final List<String> enumValues, final List<String> enumDescriptions) {
      // Sets up a SuggestOracle that, when the textbox has focus, displays the
      // valid enum values and their descriptions.
      MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
      List<Suggestion> suggestions = Lists.newArrayList();
      for (int i = 0; i < enumValues.size(); i++) {
        suggestions.add(new EnumSuggestion(
            enumValues.get(i), enumDescriptions == null ? "" : enumDescriptions.get(i)));
      }
      oracle.setDefaultSuggestions(suggestions);
      this.suggestBox = new SuggestBox(oracle);
      suggestBox.getTextBox().addFocusHandler(new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event) {
          suggestBox.showSuggestionList();
        }
      });
      add(suggestBox);

      this.errorMessage = new Label("This parameter is invalid.");
      errorMessage.setVisible(false);
      add(errorMessage);
    }

    class EnumSuggestion implements Suggestion {
      private final String enumValue;
      private final String enumDisplay;

      EnumSuggestion(String enumValue, String enumDescription) {
        this.enumValue = enumValue;
        SafeHtmlBuilder newEnumDisplay = new SafeHtmlBuilder()
            .appendHtmlConstant("<b>").appendEscaped(enumValue).appendHtmlConstant("</b>");
        if (!enumDescription.isEmpty()) {
          newEnumDisplay.appendHtmlConstant(": ").appendEscaped(enumDescription);
        }
        enumDisplay = newEnumDisplay.toSafeHtml().asString();
      }

      @Override
      public String getDisplayString() {
        return enumDisplay;
      }

      @Override
      public String getReplacementString() {
        return enumValue;
      }
    }

    @Override
    public List<String> getValue() {
      return Lists.newArrayList(suggestBox.getValue());
    }

    @Override
    public void setValue(List<String> values) {
      suggestBox.setValue(Iterables.getOnlyElement(values));
    }

    @Override
    public void setEnabled(boolean enabled) {
      suggestBox.getTextBox().setEnabled(enabled);
    }

    @Override
    public void displayValidation(ValidationResult valid) {
      removeStyleName(Resources.INSTANCE.style().infoParameter());
      removeStyleName(Resources.INSTANCE.style().invalidParameter());

      switch(valid.getType()) {
        case VALID:
          errorMessage.setVisible(false);
          break;

        case INFO:
          addStyleName(Resources.INSTANCE.style().infoParameter());
          errorMessage.setVisible(true);
          errorMessage.setText(valid.getMessage());
          break;

        case ERROR:
          addStyleName(Resources.INSTANCE.style().invalidParameter());
          errorMessage.setVisible(true);
          errorMessage.setText(valid.getMessage());
          break;
      }
    }
  }
}
