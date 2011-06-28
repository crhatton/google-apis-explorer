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

import com.google.api.explorer.client.base.ApiMethod;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is fired when a method has been selected and should be displayed
 * in the UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class MethodSelectedEvent extends GwtEvent<MethodSelectedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onMethodSelected(MethodSelectedEvent event);
  }

  public static final GwtEvent.Type<MethodSelectedEvent.Handler> TYPE =
      new GwtEvent.Type<MethodSelectedEvent.Handler>();

  public final String methodIdentifier;
  public final ApiMethod method;
  public final Multimap<String, String> params;
  
  public MethodSelectedEvent(String methodIdentifier, ApiMethod method) {
    this(methodIdentifier, method, ImmutableMultimap.<String, String>of());
  }

  public MethodSelectedEvent(
      String methodIdentifier, ApiMethod method, Multimap<String, String> params) {
    this.methodIdentifier = methodIdentifier;
    this.method = method;
    this.params = params;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMethodSelected(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof MethodSelectedEvent
        && ((MethodSelectedEvent) other).methodIdentifier.equals(methodIdentifier)
        && ((MethodSelectedEvent) other).method.equals(method);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(methodIdentifier, method);
  }
}
