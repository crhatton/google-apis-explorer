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

package com.google.api.explorer.client.base.http.crossdomain;

import com.google.api.explorer.client.base.BaseGwtTest;

/**
 * Test that our complicated bootstrapping stuff actually works.
 *
 */
public class CrossDomainRequestBuilderGwtTest extends BaseGwtTest {

  private static class InstrumentedXDRB extends CrossDomainRequestBuilder {
    boolean scriptLoadedCallbackInvoked = false;

    @Override
    public void scriptFinishedLoading() {
      scriptLoadedCallbackInvoked = true;
    }
  }

  private static native void invokeCallback() /*-{
    $wnd.__apis_explorer_load_callback();
  }-*/;

  public void testScriptCallback() {
    InstrumentedXDRB xdrb = new InstrumentedXDRB();

    CrossDomainRequestBuilder.addLoadCallback(xdrb);

    invokeCallback();

    assertTrue(xdrb.scriptLoadedCallbackInvoked);
  }

}
