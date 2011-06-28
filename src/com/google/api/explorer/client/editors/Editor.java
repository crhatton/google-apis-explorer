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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Abstract base class for logic to get, set, and validate parameter values
 * based on the properties of the parameter value being edited.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public abstract class Editor {

  private EditorView view;

  @VisibleForTesting
  final List<Validator> validators = Lists.newArrayList();

  /** Sets the values to pre-fill for this editor. */
  public void setValue(List<String> value) {
    view.setValue(value);
  }

  /** Returns all the values specified for this editor. */
  public List<String> getValue() {
    return view.getValue();
  }

  /** Sets an arbitrary {@link EditorView} for the editor. Used in testing. */
  @VisibleForTesting
  void setView(EditorView view) {
    this.view = view;
  }

  /**
   * Instantiate an appropriate {@link EditorView} and sets it on this editor
   * using {@link #setView(EditorView)}. Each editor will have its own
   * implementation of how it should be displayed.
   */
  public abstract EditorView createAndSetView();

  /** Allow/disallow the value to be edited. */
  public void setEnabled(boolean enabled) {
    view.setEnabled(enabled);
  }

  /**
   * Adds a {@link Validator} which the parameter's value(s) will be checked
   * against.
   */
  void addValidator(Validator validator) {
    validators.add(validator);
  }

  /** Whether all the values are valid for all the validators. */
  boolean isValid() {
    for (Validator validator : validators) {
      if (!validator.isValid(getValue())) {
        return false;
      }
    }
    return true;
  }

  /** Update the display to denote whether the value(s) are valid or not. */
  public void displayValidation() {
    view.displayValidation(isValid());
  }
}
