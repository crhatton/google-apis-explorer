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

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.parameter.ParameterForm;
import com.google.api.explorer.client.parameter.ParameterFormPresenter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Widget;

import java.util.SortedMap;

/**
 * View of the parameter form UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedParameterForm extends ParameterForm implements ParameterFormPresenter.Display {

  private static EmbeddedParameterFormUiBinder embeddedUiBinder =
      GWT.create(EmbeddedParameterFormUiBinder.class);

  @UiTemplate("EmbeddedParameterForm.ui.xml")
  interface EmbeddedParameterFormUiBinder extends UiBinder<Widget, EmbeddedParameterForm> {
  }

  public EmbeddedParameterForm(EventBus eventBus, AppState appState, AuthManager authManager) {
    super(eventBus, appState, authManager);
  }

  @Override
  protected void initWidget() {
    initWidget(embeddedUiBinder.createAndBindUi(this));
  }

  /** Sets the parameters displayed in the table. */
  @Override
  public void setMethod(ApiMethod method, SortedMap<String, ApiParameter> sortedParams) {
    setVisible(true);
    super.setMethod(method, sortedParams);
  }
}
