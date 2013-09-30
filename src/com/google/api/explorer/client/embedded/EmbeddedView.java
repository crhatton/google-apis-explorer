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

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.auth.AuthView;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.RequestFinishedCallback;
import com.google.api.explorer.client.history.EmbeddedHistoryItemView;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Simple view that shows a parameter form and the associated history panel when a request has been
 * executed.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedView extends Composite {

  private static EmbeddedViewUiBinder uiBinder = GWT.create(EmbeddedViewUiBinder.class);

  interface EmbeddedViewUiBinder extends UiBinder<Widget, EmbeddedView> {
  }

  @UiField(provided = true) EmbeddedParameterForm parameterForm;
  @UiField(provided = true) EmbeddedHistoryPanel historyPanel;
  @UiField Panel authViewPlaceholder;
  @UiField Panel authViewContainer;

  /**
   * Create an instance with the provided dependencies.
   *
   * @param authManager Auth manager dependency that is responsible for managing auth tokens.
   * @param service Service for which this view is being constructed.
   * @param method Method for which this view is being constructed.
   * @param params Parameters that should be filled out for the form in advance.
   * @param callback Callback which should be invoked when a request has completed.
   * @param showAuth Whether auth should be shown in this view or provided by the embedding context.
   * @param analytics Analytics object to use when reporting events.
   */
  public EmbeddedView(AuthManager authManager,
      ApiService service,
      final ApiMethod method,
      Multimap<String, String> params,
      RequestFinishedCallback callback,
      boolean showAuth,
      AnalyticsManager analytics) {

    this.historyPanel = new EmbeddedHistoryPanel();
    this.parameterForm = new EmbeddedParameterForm(authManager, callback);

    initWidget(uiBinder.createAndBindUi(this));

    parameterForm.getPresenter().selectMethod(service, method, params);

    authViewContainer.setVisible(showAuth);
    if (showAuth) {
      showAuth(authManager, service, method, analytics);
    }

    historyPanel.setVisible(false);
  }

  private void showAuth(
      AuthManager authManager, ApiService service, ApiMethod method, AnalyticsManager analytics) {
    AuthView auth = new AuthView(authManager, service, analytics);
    auth.getPresenter().setStateForMethod(method);

    authViewPlaceholder.add(auth);
  }

  public void showHistoryItem(EmbeddedHistoryItemView item) {
    historyPanel.setVisible(true);
    historyPanel.setHistoryItemView(item);
  }
}
