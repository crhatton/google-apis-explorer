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

package com.google.api.explorer.client;

import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Config;
import com.google.api.gwt.oauth2.client.Auth;
import com.google.api.gwt.oauth2.client.AuthRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Callback;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

/**
 * Manages authentication state by accepting auth requests asynchronously and notifies callers when
 * auth is ready.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthManager {

  /**
   * Class which binds scope information with the granted auth token for more intelligent messaging
   * when there is insufficient auth.
   *
   */
  @Immutable
  public class AuthToken {
    private final String authToken;
    private final ImmutableSet<String> scopes;

    private AuthToken(String authToken, Set<String> scopes) {
      this.authToken = Preconditions.checkNotNull(authToken);
      this.scopes = ImmutableSet.copyOf(scopes);
    }

    /**
     * Returns the auth token.
     */
    public String getAuthToken() {
      return authToken;
    }

    /**
     * Returns the scopes that were used when this token was granted.
     */
    public ImmutableSet<String> getScopes() {
      return scopes;
    }
  }

  /**
   * Interface which defines the callback definition format that must be implemented to receive
   * information about when auth has been completed.
   *
   */
  public interface AuthCompleteCallback {
    /**
     * Callback method that is invoked when auth has completed successfully.
     *
     * @param token Token which was obtained.
     */
    public void complete(AuthToken token);
  }

  private static final Map<ApiService, AuthToken> authTokens = Maps.newHashMap();

  /**
   * Get the token stored for the current service, or {@code null} if there is
   * no token.
   */
  public AuthToken getToken(ApiService service) {
    return authTokens.get(service);
  }

  /**
   * Request auth for the given service and scopes, and notify the callback when complete.
   *
   * @param service Service for which auth is being requested.
   * @param scopes Scopes which the user is requesting access to.
   * @param callback Receiver which should be notified when there is a failure.
   * @throws RuntimeException when an exception occurs completing the auth.
   */
  public void requestAuth(
      final ApiService service, final Set<String> scopes, final AuthCompleteCallback callback) {

    // TODO(jasonhall): Show some indication that auth is in progress here.
    String[] scopeArray = scopes.toArray(new String[] {});
    AuthRequest req = new AuthRequest(Config.AUTH_URL, Config.CLIENT_ID).withScopes(scopeArray);

    Auth.get().login(req, new Callback<String, Throwable>() {
      @Override
      public void onSuccess(String tokenString) {
        AuthToken token = new AuthToken(tokenString, scopes);
        authTokens.put(service, token);
        callback.complete(token);
      }

      @Override
      public void onFailure(Throwable caught) {
        // When this occurs the UI is left unchanged and the user is allowed to retry the auth
        // request by clicking the toggle again.
        throw new RuntimeException(caught);
      }
    });
  }

  /**
   * "Revokes" the access token, in the sense that it tells the app to forget
   * that it knows the auth token.
   *
   * <p>
   * This method does *not* actually revoke the token on the server, but will,
   * when this is supported.
   * </p>
   */
  public void revokeAccess(ApiService service) {
    // TODO(jasonhall): This should actually revoke access on the server, and
    // remove the token from the cookie. It currently does nothing more than
    // "forget" it knows the token.
    authTokens.remove(service);
  }
}
