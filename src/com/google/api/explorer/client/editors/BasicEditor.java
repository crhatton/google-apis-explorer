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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.SimplePanel;
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

  public static class BasicViewImpl extends SimplePanel implements EditorView {

    private TextBox textBox;

    public BasicViewImpl() {
      this.textBox = new TextBox();
      add(textBox);
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
    public void displayValidation(boolean valid) {
      textBox.setStyleName(valid ? "" : Resources.INSTANCE.style().invalidParameter());
    }
  }
}
