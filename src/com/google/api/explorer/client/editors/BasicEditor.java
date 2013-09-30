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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import java.util.List;

/**
 * The most basic implementation of an {@link Editor}, which provides a simple
 * textbox to the user.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
class BasicEditor extends Editor {

  @Override
  public EditorView createAndSetView() {
    BasicViewImpl viewImpl = new BasicViewImpl();
    setView(viewImpl);
    return viewImpl;
  }

  public static class BasicViewImpl extends FlowPanel implements EditorView {

    private TextBox textBox;
    private Label errorMessage;

    public BasicViewImpl() {
      this.textBox = new TextBox();
      this.errorMessage = new Label("This parameter is invalid.");
      errorMessage.setVisible(false);
      add(textBox);
      add(errorMessage);
    }

    @Override
    public List<String> getValue() {
      return Lists.newArrayList(textBox.getValue());
    }

    @Override
    public void setValue(List<String> values) {
      textBox.setValue(Iterables.getOnlyElement(values));
    }

    @Override
    public void setEnabled(boolean enabled) {
      textBox.setEnabled(enabled);
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
