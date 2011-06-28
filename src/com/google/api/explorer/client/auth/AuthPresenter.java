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
import com.google.api.explorer.client.auth.AuthPresenter.Display.State;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.api.explorer.client.event.AuthGrantedEvent;
import com.google.api.explorer.client.event.AuthRequestedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.gwt.event.shared.EventBus;

import java.util.Map;

/**
 * Presenter to handle logic for authentication view.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthPresenter
    implements ServiceLoadedEvent.Handler, AuthGrantedEvent.Handler, ServiceSelectedEvent.Handler {

  interface Display {
    /** Set the list of available scopes for the current service. */
    void setScopes(Map<String, AuthScope> scopes);

    /**
     * Get the name of the selected scope, or {@code null} if there are no
     * scopes.
     */
    String getSelectedScope();

    /** Set which state the Authentication UI should be in. */
    void setState(State state);

    /** Authentication states that can be displayed. */
    enum State {
      /** Only public access is available. */
      ONLY_PUBLIC,
      /** Only public access is granted (private is available). */
      PUBLIC,
      /** Private access is granted. */
      PRIVATE;
    }
  }

  private final EventBus eventBus;
  private final AuthManager authManager;
  private final Display display;

  public AuthPresenter(EventBus eventBus, AuthManager authManager, Display display) {
    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    eventBus.addHandler(AuthGrantedEvent.TYPE, this);
    this.eventBus = eventBus;
    this.authManager = authManager;
    this.display = display;
  }

  void clickAuthLink() {
    String scope = display.getSelectedScope();
    if (scope != null) {
      eventBus.fireEvent(new AuthRequestedEvent(scope));
    }
  }

  void clickRevokeLink() {
    authManager.revokeAccess();
    display.setState(State.PUBLIC);
  }

  /** Key in the "auth" map that defines OAuth 2.0 information. */
  private static final String OAUTH2_KEY = "oauth2";

  @Override
  public void onServiceLoaded(ServiceLoadedEvent event) {
    if (event.service.getAuth() == null
        || !event.service.getAuth().containsKey(OAUTH2_KEY)
        || event.service.getAuth().get(OAUTH2_KEY).getScopes().isEmpty()) {
      display.setState(State.ONLY_PUBLIC);
    } else {
      display.setState(authManager.getToken() == null ? State.PUBLIC : State.PRIVATE);
      display.setScopes(event.service.getAuth().get(OAUTH2_KEY).getScopes());
    }
  }

  @Override
  public void onAuthGranted(AuthGrantedEvent event) {
    display.setState(State.PRIVATE);
  }

  @Override
  public void onServiceSelected(ServiceSelectedEvent event) {
    display.setState(State.ONLY_PUBLIC);
  }
}
