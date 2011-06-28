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

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.common.base.Objects;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is called when a {@link ApiRequest} is finished.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class RequestFinishedEvent extends GwtEvent<RequestFinishedEvent.Handler> {

  /** Interface that handlers of this event must implement. */
  public interface Handler extends EventHandler {
    public void onRequestFinished(RequestFinishedEvent event);
  }

  public static final GwtEvent.Type<RequestFinishedEvent.Handler> TYPE =
      new GwtEvent.Type<RequestFinishedEvent.Handler>();

  public final ApiRequest request;
  public final ApiResponse response;
  public final long timeMillis;

  public RequestFinishedEvent(
      ApiRequest request, ApiResponse response, long timeMillis) {
    this.request = request;
    this.response = response;
    this.timeMillis = timeMillis;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRequestFinished(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof RequestFinishedEvent
        && ((RequestFinishedEvent) other).request.equals(request)
        && ((RequestFinishedEvent) other).response.equals(response);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(request, response);
  }
}
