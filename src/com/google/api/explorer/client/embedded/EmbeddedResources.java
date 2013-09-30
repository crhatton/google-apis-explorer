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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Shared;

/**
 * Styling resources for the embedded version of the Explorer.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface EmbeddedResources extends ClientBundle {

  final EmbeddedResources INSTANCE = GWT.create(EmbeddedResources.class);

  static final String PATH = "com/google/api/explorer/public/";

  @Source(PATH + "embedded.css")
  Css style();

  /**
   * {@link CssResource} for embedded.css
   */
  @Shared
  public interface Css extends CssResource {

    String parameterFormNameCell();

    String requiredParameter();

    String parameterFormDescriptionCell();

    String parameterFormEditorCell();

    String bodyDisclosure();
  }
}
