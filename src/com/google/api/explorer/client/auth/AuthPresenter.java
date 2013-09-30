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
import com.google.api.explorer.client.AuthManager.AuthCompleteCallback;
import com.google.api.explorer.client.AuthManager.AuthToken;
import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.analytics.AnalyticsManager.AnalyticsEvent;
import com.google.api.explorer.client.auth.AuthPresenter.Display.State;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Presenter to handle logic for authentication view.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthPresenter {

  interface Display {
    /** Set the list of available scopes from discovery for the current service. */
    void setScopes(Map<String, AuthScope> scopes);

    /** Get the names of the selected scopes. */
    Set<String> getSelectedScopes();

    /** Set which state the Authentication UI should be in. */
    void setState(State state, Set<String> requiredScopes, Set<String> heldScopes);

    /** Authentication states that can be displayed. */
    enum State {
      /** Only public access is available. */
      ONLY_PUBLIC,
      /** Only public access is granted (private is available). */
      PUBLIC,
      /** Private access is granted. */
      PRIVATE;
    }

    /** Show the scope editor dialog. */
    void showScopeDialog();

    /** Hide the scope editor dialog. */
    void hideScopeDialog();

    /** Add a free form scope editor to the auth dialog. */
    void addScopeEditor();

    /** Select the named scopes from discovery to select. */
    void preSelectScopes(Set<String> scopes);
  }

  /** Common prefix of all auth scopes. Remove this to get the unique ID. */
  // TODO(user): When Discovery provides an auth scope alias, use that
  // instead of the de-prefixed scope URL.
  private static final String AUTH_URL_PREFIX = "https://www.googleapis.com/auth/";

  private final AuthManager authManager;
  private final Display display;
  private final ApiService service;
  private final AnalyticsManager analytics;

  private ImmutableSet<String> requiredScopes = ImmutableSet.of();

  public AuthPresenter(
      ApiService service, AuthManager authManager, AnalyticsManager analytics, Display display) {
    this.service = service;
    this.authManager = authManager;
    this.display = display;
    this.analytics = analytics;

    setStateForService();
  }

  void clickExecuteAuth() {
    display.hideScopeDialog();

    // Get and sort the list of scopes.
    Set<String> scopes = Sets.newTreeSet(display.getSelectedScopes());

    // Inform analytics that a user is requesting auth.
    final String scopesString = Joiner.on("; ").join(scopes);
    analytics.trackEventWithValue(AnalyticsEvent.AUTH_REQUEST, scopesString);

    authManager.requestAuth(service, scopes, new AuthCompleteCallback() {
      @Override
      public void complete(AuthToken token) {
        analytics.trackEventWithValue(AnalyticsEvent.AUTH_TOKEN, scopesString);
        display.setState(State.PRIVATE, requiredScopes, token.getScopes());
      }
    });
  }

  void clickCancelAuth() {
    display.hideScopeDialog();
  }

  void clickEnableAuth() {
    display.showScopeDialog();

    // We are setting the state back to public to reset the toggle until auth is completed.
    display.setState(State.PUBLIC, requiredScopes, Collections.<String>emptySet());
  }

  void clickDisableAuth() {
    authManager.revokeAccess(service);

    setStateForService();
  }

  /** Add button was clicked indicating that a new scope editor is desired. */
  void addNewScope() {
    display.addScopeEditor();
  }

  /** Compute the scope name from the URL. */
  static String scopeName(String scopeUrl) {
    // If the scope begins with the common URL prefix, strip it.
    return scopeUrl.startsWith(AUTH_URL_PREFIX) ? scopeUrl.substring(
        AUTH_URL_PREFIX.length(), scopeUrl.length()) : scopeUrl;
  }

  /** Key in the "auth" map that defines OAuth 2.0 information. */
  private static final String OAUTH2_KEY = "oauth2";

  private void setStateForService() {
    if (service.getAuth() == null || !service.getAuth().containsKey(OAUTH2_KEY)
        || service.getAuth().get(OAUTH2_KEY).getScopes().isEmpty()) {
      display.setScopes(Collections.<String, AuthScope>emptyMap());
    } else {
      display.setScopes(service.getAuth().get(OAUTH2_KEY).getScopes());
    }

    AuthToken existingToken = authManager.getToken(service);
    if (existingToken != null) {
      display.setState(State.PRIVATE, requiredScopes, existingToken.getScopes());
      display.preSelectScopes(existingToken.getScopes());
    } else {
      display.setState(State.PUBLIC, requiredScopes, Collections.<String>emptySet());
      display.preSelectScopes(requiredScopes);
    }
  }

  public void setStateForMethod(ApiMethod method) {
    Preconditions.checkNotNull(method);

    requiredScopes = method.getScopes() == null
        ? ImmutableSet.<String>of() : ImmutableSet.copyOf(method.getScopes());

    setStateForService();
  }
}
