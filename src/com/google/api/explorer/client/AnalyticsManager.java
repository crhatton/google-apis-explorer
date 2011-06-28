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

package com.google.api.explorer.client;

import com.google.api.explorer.client.event.RequestFinishedEvent;
import com.google.gwt.event.shared.EventBus;

/**
 * Class responsible for calling out to Analytics to tell it when a "page" has
 * been viewed.
 */
public class AnalyticsManager implements RequestFinishedEvent.Handler {

  public AnalyticsManager(EventBus eventBus) {
    eventBus.addHandler(RequestFinishedEvent.TYPE, this);
  }

  public static native void trackPageview(String pageName) /*-{
    $wnd._gaq.push(['_trackPageview', pageName]);
  }-*/;

  @Override
  public void onRequestFinished(RequestFinishedEvent event) {
    trackPageview(event.request.getRequestPath());
  }
}
