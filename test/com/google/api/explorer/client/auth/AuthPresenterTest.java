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
import com.google.api.explorer.client.auth.AuthPresenter.Display;
import com.google.api.explorer.client.auth.AuthPresenter.Display.State;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.AuthInformation;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.EasyMock;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link AuthPresenter}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthPresenterTest extends TestCase {

  private static final Set<String> EMPTY_SCOPES = Collections.emptySet();

  private final Display display = EasyMock.createMock(Display.class);
  private final AuthManager authManager = EasyMock.createStrictMock(AuthManager.class);
  private final ApiService service = EasyMock.createControl().createMock(ApiService.class);
  private final AnalyticsManager analytics =
      EasyMock.createControl().createMock(AnalyticsManager.class);

  /**
   * When a ServiceLoadedEvent fires, the Display becomes visible and displays
   * the user as un-authenticated by default.
   */

  public void testServiceLoadedWithoutAuth() {
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(service.allMethods()).andReturn(
        ImmutableMap.of("method.one", method1, "method.two", method2));
    EasyMock.expect(service.getName()).andReturn("service");
    display.setScopes(Collections.<String, ApiService.AuthScope>emptyMap());
    display.setState(State.PUBLIC, EMPTY_SCOPES, EMPTY_SCOPES);
    display.preSelectScopes(EMPTY_SCOPES);
    EasyMock.replay(display);

    @SuppressWarnings("unused")
    AuthPresenter presenter = new AuthPresenter(service, authManager, analytics, display);

    EasyMock.verify(display);
  }

  /** Check the flow which sets up the display for auth. */
  public void testAuthGranted() {
    ApiMethod method1 = EasyMock.createControl().createMock(ApiMethod.class);
    ApiMethod method2 = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method1.getScopes()).andReturn(ImmutableList.of("scopeName")).anyTimes();
    ImmutableSet<String> authScopes = ImmutableSet.of("scopeName");
    Map<String, AuthInformation> auth = generateAuthInformation(authScopes);
    EasyMock.expect(service.getAuth()).andReturn(auth).anyTimes();

    // This is called once for every call to the presenter.
    display.setScopes(auth.get("oauth2").getScopes());
    EasyMock.expectLastCall().times(2);

    // When the presenter is created these will be called.
    display.setState(State.PUBLIC, EMPTY_SCOPES, EMPTY_SCOPES);
    EasyMock.expectLastCall();
    display.preSelectScopes(EMPTY_SCOPES);
    EasyMock.expectLastCall();
    EasyMock.expect(authManager.getToken(service)).andReturn(null).times(2);

    // When the method is set, these will be called.
    EasyMock.expect(display.getSelectedScopes()).andReturn(authScopes);
    final Capture<AuthCompleteCallback> cbCapture = new Capture<AuthCompleteCallback>();
    authManager.requestAuth(
        EasyMock.eq(service), EasyMock.eq(authScopes), EasyMock.capture(cbCapture));
    EasyMock.expectLastCall();

    display.setState(State.PUBLIC, authScopes, EMPTY_SCOPES);
    EasyMock.expectLastCall();
    display.preSelectScopes(authScopes);
    EasyMock.expectLastCall();

    // When execute auth is clicked, these will be called.
    display.hideScopeDialog();
    EasyMock.expectLastCall();
    display.setState(State.PRIVATE, authScopes, authScopes);
    EasyMock.expectLastCall();

    AuthToken token = EasyMock.createMock(AuthToken.class);
    EasyMock.expect(token.getScopes()).andReturn(authScopes);
    EasyMock.expect(authManager.getToken(service)).andReturn(token);

    EasyMock.replay(service, display, method1, method2, authManager, token);

    AuthPresenter presenter = new AuthPresenter(service, authManager, analytics, display);
    presenter.setStateForMethod(method1);
    presenter.clickExecuteAuth();
    cbCapture.getValue().complete(token);

    EasyMock.verify(display);
  }

  private Map<String, AuthInformation> generateAuthInformation(Set<String> scopes) {
    Map<String, AuthScope> authScopes = Maps.newHashMap();

    for (String scopeName : scopes) {
      AuthScope mockScope = EasyMock.createMock(AuthScope.class);
      EasyMock.replay(mockScope);
      authScopes.put(scopeName, mockScope);
    }

    AuthInformation mockAuth = EasyMock.createMock(AuthInformation.class);
    EasyMock.expect(mockAuth.getScopes()).andReturn(authScopes).anyTimes();

    EasyMock.replay(mockAuth);

    Map<String, AuthInformation> auth = Maps.newHashMap();
    auth.put("oauth2", mockAuth);

    return auth;
  }
}
