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

import com.google.api.explorer.client.ApiDirectory.ServiceDefinition;
import com.google.common.base.Objects;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import java.util.Set;

/**
 * Event that is triggered when the set of available services and versions is
 * loaded from the Discovery API.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceDefinitionsLoadedEvent extends GwtEvent<ServiceDefinitionsLoadedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onServiceDefinitionsLoaded(ServiceDefinitionsLoadedEvent event);
  }

  public static final GwtEvent.Type<ServiceDefinitionsLoadedEvent.Handler> TYPE =
      new GwtEvent.Type<ServiceDefinitionsLoadedEvent.Handler>();

  public final Set<ServiceDefinition> definitions;

  public ServiceDefinitionsLoadedEvent(Set<ServiceDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onServiceDefinitionsLoaded(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof ServiceDefinitionsLoadedEvent
        && ((ServiceDefinitionsLoadedEvent) other).definitions.equals(definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(definitions);
  }
}
