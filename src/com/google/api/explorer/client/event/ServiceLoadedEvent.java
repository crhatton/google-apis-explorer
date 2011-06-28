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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is triggered when an API service is done loading from Discovery.
 *
 * <p>
 * This event is typically the result of a {@link ServiceSelectedEvent} being
 * fired, which may include a resource and/or method name, and optionally a map
 * of parameters to fill in. If these are specified in the selection event, they
 * will be propagated to the resulting {@link ServiceLoadedEvent}.
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceLoadedEvent extends GwtEvent<ServiceLoadedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onServiceLoaded(ServiceLoadedEvent event);
  }

  public static final GwtEvent.Type<ServiceLoadedEvent.Handler> TYPE =
      new GwtEvent.Type<ServiceLoadedEvent.Handler>();

  /** Service that was loaded. */
  public final ApiService service;

  /**
   * Parameter keys/values that have been specified that should be used in cases when a service
   * includes only one method. In this case the method is auto-selected and these parameter values
   * are pre-filled in the parameter form.
   */
  public final Multimap<String, String> initialParams;

  public ServiceLoadedEvent(ApiService service) {
    this(service, ImmutableMultimap.<String, String>of());
  }

  public ServiceLoadedEvent(ApiService service, Multimap<String, String> params) {
    this.service = service;
    this.initialParams = params;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onServiceLoaded(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof ServiceLoadedEvent
        && ((ServiceLoadedEvent) other).service.equals(service);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(service);
  }
}
