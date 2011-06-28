/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.api.explorer.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

/**
 * @author jasonhall@google.com (Jason Hall)
 */
public interface Resources extends ClientBundle {

  final Resources INSTANCE = GWT.create(Resources.class);

  static final String PATH = "com/google/api/explorer/public/";

  @Source(PATH + "logo.png")
  ImageResource logo();

  @Source(PATH + "plus-box.gif")
  ImageResource expand();

  @Source(PATH + "minus-box.gif")
  ImageResource collapse();

  @Source(PATH + "loading.gif")
  ImageResource loading();

  @Source(PATH + "error.gif")
  ImageResource error();

  @Source(PATH + "showHide.png")
  ImageResource showHide();

  @Source(PATH + "mini-loading.gif")
  ImageResource miniLoading();

  @Source(PATH + "remove.gif")
  ImageResource removeParameter();

  @Source(PATH + "magnifyingGlass.png")
  ImageResource magnifyingGlass();

  @Source(PATH + "explorer.css")
  Css style();

  /**
   * {@link CssResource} for explorer.css
   */
  public interface Css extends CssResource {
    String expanded();

    String collapsed();

    String selected();

    String loading();

    String error();

    String showHide();

    String parameterFormNameCell();

    String parameterFormTextBoxCell();

    String parameterFormDescriptionCell();

    String addParameter();

    String removeParameter();

    String invalidParameter();

    String jsonNull();

    String jsonNumber();

    String jsonBoolean();

    String jsonString();

    String jsonStringLink();

    String jsonStringExplorerLink();

    String jsonKey();

    String jsonObject();

    String jsonArray();

    String responseLine();
  }
}
