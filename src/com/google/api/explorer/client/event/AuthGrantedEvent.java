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

package com.google.api.explorer.client.event;

import com.google.api.explorer.client.base.ApiService;
import com.google.common.base.Objects;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is fired when authentication is requested to a service.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthGrantedEvent extends GwtEvent<AuthGrantedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onAuthGranted(AuthGrantedEvent event);
  }

  public static final GwtEvent.Type<AuthGrantedEvent.Handler> TYPE =
      new GwtEvent.Type<AuthGrantedEvent.Handler>();

  public final ApiService service;
  public final String token;

  public AuthGrantedEvent(ApiService service, String token) {
    this.service = service;
    this.token = token;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAuthGranted(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof AuthGrantedEvent
        && ((AuthGrantedEvent) other).service.equals(service)
        && ((AuthGrantedEvent) other).token.equals(token);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(service, token);
  }
}
