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

import com.google.gwt.user.client.ui.IsWidget;

import java.util.List;

/**
 * Represents the view of an {@link Editor} displayed to the user.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface EditorView extends IsWidget {

  /** Sets the values used to pre-fill the view. */
  void setValue(List<String> values);

  /** Returns the values given by the user. */
  List<String> getValue();

  /** Allow/disallow the value to be edited. */
  void setEnabled(boolean enabled);

  /** Update the view to denote whether the values are valid. */
  void displayValidation(boolean valid);
}
