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

package com.google.api.explorer.client.auth;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

/**
 * View for Authentication status and authentication link.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthView extends Composite implements AuthPresenter.Display {

  private static AuthUiBinder uiBinder = GWT.create(AuthUiBinder.class);

  interface AuthUiBinder extends UiBinder<Widget, AuthView> {
  }

  /** Common prefix of all auth scopes. Remove this to get the unique ID. */
  // TODO(jasonhall): When Discovery provides an auth scope alias, use that
  // instead of the de-prefixed scope URL.
  private static String AUTH_URL_PREFIX = "https://www.googleapis.com/auth/";

  @UiField InlineLabel authState;
  @UiField InlineLabel authLink;
  @UiField SpanElement scopeSelector;
  @UiField ListBox scopeSelectorListBox;
  @UiField InlineLabel revokeAccess;

  private final AuthPresenter presenter;
  private boolean hasScopes = false;

  public AuthView(EventBus eventBus, AuthManager authManager) {
    initWidget(uiBinder.createAndBindUi(this));

    this.presenter = new AuthPresenter(eventBus, authManager, this);
    setState(State.ONLY_PUBLIC);
  }

  @UiHandler("authLink")
  void authorize(ClickEvent event) {
    presenter.clickAuthLink();
  }
  
  @UiHandler("revokeAccess")
  void revoke(ClickEvent event) {
    presenter.clickRevokeLink();
  }

  @Override
  public void setState(State state) {
    authLink.setVisible(state == State.PUBLIC);
    authState.setText(state == State.PRIVATE ? "Using Private Access" : "Using Public Access.");
    UIObject.setVisible(scopeSelector, state != State.ONLY_PUBLIC);
    scopeSelectorListBox.setEnabled(state == State.PUBLIC);
    revokeAccess.setVisible(state == State.PRIVATE);
  }

  @Override
  public void setScopes(Map<String, AuthScope> scopes) {
    hasScopes = !scopes.isEmpty();
    scopeSelectorListBox.clear();

    if (hasScopes) {
      for (String scope : scopes.keySet()) {
        // If the scope begins with the common URL prefix, strip it.
        String name = scope.startsWith(AUTH_URL_PREFIX)
            ? scope.substring(AUTH_URL_PREFIX.length(), scope.length())
            : scope;
        scopeSelectorListBox.addItem(name);
      }
      scopeSelectorListBox.setSelectedIndex(0);
    }
  }

  @Override
  public String getSelectedScope() {
    return hasScopes
        ? AUTH_URL_PREFIX + scopeSelectorListBox.getValue(scopeSelectorListBox.getSelectedIndex())
        : null;
  }
}
