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

package com.google.api.explorer;

import com.google.api.explorer.client.base.dynamicjso.DynamicJsArrayGwtTest;
import com.google.api.explorer.client.base.dynamicjso.DynamicJsoGwtTest;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequestBuilderGwtTest;
import com.google.api.explorer.client.base.http.crossdomain.CrossDomainRequestGwtTest;
import com.google.api.explorer.client.base.rpc.gwt.RpcApiRequestGwtTest;
import com.google.api.explorer.client.embedded.RequestBodyFormGwtTest;
import com.google.api.explorer.client.parameter.schema.ObjectSchemaEditorGwtTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.TestSuite;

/**
 * {@link GWTTestSuite} for GoogleApi GWT tests.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class GwtTests extends GWTTestSuite {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite("GWT tests for GoogleApi module");
    suite.addTestSuite(DynamicJsoGwtTest.class);
    suite.addTestSuite(DynamicJsArrayGwtTest.class);
    suite.addTestSuite(RpcApiRequestGwtTest.class);
    suite.addTestSuite(CrossDomainRequestGwtTest.class);
    suite.addTestSuite(CrossDomainRequestBuilderGwtTest.class);
    suite.addTestSuite(ObjectSchemaEditorGwtTest.class);
    suite.addTestSuite(RequestBodyFormGwtTest.class);
    return suite;
  }
}
