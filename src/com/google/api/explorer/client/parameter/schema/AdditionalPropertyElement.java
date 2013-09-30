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
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

/**
 * Encapsulates the key/value of one key/value pair in an
 * {@link ObjectSchemaEditor}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AdditionalPropertyElement extends Composite {

  private static RowUiBinder uiBinder = GWT.create(RowUiBinder.class);

  interface RowUiBinder extends UiBinder<Widget, AdditionalPropertyElement> {
  }

  /** Helper function that will normalize this item into a view consistent with other editors. */
  public static final Function<AdditionalPropertyElement, Map.Entry<String, SchemaEditor>>
      normalizeEditor = new Function<AdditionalPropertyElement, Map.Entry<String, SchemaEditor>>() {
        @Override
        public Map.Entry<String, SchemaEditor> apply(final AdditionalPropertyElement input) {
          return new Map.Entry<String, SchemaEditor>() {
            @Override
            public String getKey() {
              return input.getKeyValue();
            }

            @Override
            public SchemaEditor getValue() {
              return input.getEditor();
            }

            @Override
            public SchemaEditor setValue(SchemaEditor value) {
              throw new UnsupportedOperationException();
            }
          };
        }
      };

  @UiField HTMLPanel panel;
  @UiField TextBox keyInput;
  @UiField HTMLPanel placeholder;
  @UiField InlineLabel remove;

  private final SchemaEditor editor;

  public AdditionalPropertyElement(SchemaEditor editor, Schema property) {

    // Keep track of the editor we are wrapping.
    this.editor = Preconditions.checkNotNull(editor);

    // Bind this UI to the declarative layout file.
    initWidget(uiBinder.createAndBindUi(this));

    // Show our inner editor in the placeholder panel.
    placeholder.add(editor.render(property));

    // If the editor is not an Object or Array editor, it can contain inner
    // elements, and will have its own inner remove link to manage them. If
    // that's the case, move the remove link to after the editor.
    if (!editor.isComposite()) {
      moveRemoveAfterEditor();
    }
  }

  public String getKeyValue() {
    return keyInput.getText();
  }

  public void setKeyValue(String keyValue) {
    keyInput.setText(keyValue);
  }

  public SchemaEditor getEditor() {
    return editor;
  }

  private void moveRemoveAfterEditor() {
    panel.remove(remove);
    panel.add(remove);
  }
}
