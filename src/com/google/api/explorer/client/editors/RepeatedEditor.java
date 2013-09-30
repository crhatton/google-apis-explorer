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
import com.google.api.explorer.client.editors.Validator.ValidationResult.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;

import java.util.List;

/**
 * Implementation of an {@link Editor} that accepts multiple values.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
class RepeatedEditor extends Editor {

  final Editor innerEditor;

  RepeatedEditor(Editor innerEditor) {
    this.innerEditor = innerEditor;
  }

  @Override
  ValidationResult isValid() {
    ValidationResult innerValid = innerEditor.isValid();
    if (innerValid.getType() != ValidationResult.Type.VALID) {
      return innerValid;
    } else {
      return super.isValid();
    }
  }

  @Override
  void setView(EditorView view) {
    super.setView(view);
    innerEditor.setView(view);
  }

  @Override
  public EditorView createAndSetView() {
    RepeatedEditorViewImpl viewImpl = new RepeatedEditorViewImpl();
    setView(viewImpl);
    return viewImpl;
  }

  /** {@link EditorView} implementation for repeated parameters. */
  class RepeatedEditorViewImpl extends FlowPanel implements EditorView {

    private final List<EditorView> views = Lists.newArrayList();
    private final Label errorMessage = new Label();
    private final InlineLabel addMore = new InlineLabel("add more");

    RepeatedEditorViewImpl() {
      // Add the error message, editors will appear above
      errorMessage.setVisible(false);
      add(errorMessage);

      // The first editor is added immediately.
      addEditor();

      // The first editor displays an "add more" link that, when clicked, adds
      // another editor to the view.
      addMore.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          addEditor();
        }
      });
      addMore.addStyleName(Resources.INSTANCE.style().addParameter());
    }

    private EditorView addEditor() {
      // Get the correct inner editor view to display, and add it.
      final EditorView view = innerEditor.createAndSetView();

      // We want the editor to use our view as the source of values.
      innerEditor.setView(this);
      views.add(view);

      insert(view, getWidgetIndex(errorMessage));

      // If this is not the first editor view, add a clickable image that, when
      // clicked, will remove this editor from the view, as well as the image
      // itself.
      if (views.size() > 1) {
        final Image remove = new Image(Resources.INSTANCE.removeParameter());
        remove.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            removeView(view);
            remove(remove);
          }
        });
        remove.setAltText("Remove this value");
        remove.addStyleName(Resources.INSTANCE.style().removeParameter());
        insert(remove, getWidgetIndex(errorMessage));
      } else {
        insert(addMore, getWidgetIndex(errorMessage));
      }

      return view;
    }

    private void removeView(EditorView view) {
      views.remove(view);
      remove(view);
    }

    /** Return all the values given by the inner editor views. */
    @Override
    public List<String> getValue() {
      List<String> value = Lists.newArrayList();
      for (EditorView view : views) {
        value.addAll(view.getValue());
      }
      return value;
    }

    /**
     * Set the values of the inner editor views based on the values given. This
     * will add N inner editor views to this view, and set their values.
     */
    @Override
    public void setValue(List<String> values) {
      for (int i = 0; i < views.size(); i++) {
        remove(views.get(i));
      }
      views.clear();
      remove(addMore);
      for (String value : values) {
        EditorView view = addEditor();
        view.setValue(ImmutableList.of(value));
      }
    }

    @Override
    public void setEnabled(boolean enabled) {
      for (EditorView view : views) {
        view.setEnabled(enabled);
      }
    }

    @Override
    public void displayValidation(ValidationResult valid) {
      errorMessage.setVisible(valid.getType() != Type.VALID);
      removeStyleName(Resources.INSTANCE.style().infoParameter());
      removeStyleName(Resources.INSTANCE.style().invalidParameter());

      switch(valid.getType()) {
        case INFO:
          addStyleName(Resources.INSTANCE.style().infoParameter());
          errorMessage.setText(valid.getMessage());
          break;

        case ERROR:
          addStyleName(Resources.INSTANCE.style().invalidParameter());
          errorMessage.setText(valid.getMessage());
          break;
      }
    }
  }
}
