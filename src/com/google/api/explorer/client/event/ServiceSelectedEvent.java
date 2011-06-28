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
 * Event that is triggered when the user selects a new API service to be
 * displayed.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceSelectedEvent extends GwtEvent<ServiceSelectedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onServiceSelected(ServiceSelectedEvent event);
  }

  public static final GwtEvent.Type<ServiceSelectedEvent.Handler> TYPE =
      new GwtEvent.Type<ServiceSelectedEvent.Handler>();

  public final String serviceName;

  public ServiceSelectedEvent(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onServiceSelected(this);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj instanceof ServiceSelectedEvent
        && ((ServiceSelectedEvent) obj).serviceName.equals(serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceName);
  }
}
