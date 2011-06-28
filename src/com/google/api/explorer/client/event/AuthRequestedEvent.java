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

import com.google.common.base.Objects;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is fired when authentication is requested to the current service.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthRequestedEvent extends GwtEvent<AuthRequestedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onAuthRequested(AuthRequestedEvent event);
  }

  public static final GwtEvent.Type<AuthRequestedEvent.Handler> TYPE =
      new GwtEvent.Type<AuthRequestedEvent.Handler>();

  /** The scope URL for which to request authentication. */
  public final String scope;

  public AuthRequestedEvent(String scope) {
    this.scope = scope;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAuthRequested(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof AuthRequestedEvent
        && ((AuthRequestedEvent) other).scope.equals(scope);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(scope);
  }
}
