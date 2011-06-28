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

import com.google.api.explorer.client.ApiDirectoryTest;
import com.google.api.explorer.client.AppStateTest;
import com.google.api.explorer.client.HistoryManagerTest;
import com.google.api.explorer.client.ServiceLoaderTest;
import com.google.api.explorer.client.auth.AuthPresenterTest;
import com.google.api.explorer.client.base.ApiRequestTest;
import com.google.api.explorer.client.base.ApiServiceTest;
import com.google.api.explorer.client.editors.EditorFactoryTest;
import com.google.api.explorer.client.method.MethodSelectorPresenterTest;
import com.google.api.explorer.client.parameter.ParameterFormPresenterTest;
import com.google.api.explorer.client.service.ServiceSelectorPresenterTest;
import com.google.api.explorer.client.version.VersionSelectorPresenterTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author jasonhall@google.com (Jason Hall)
 */
public class JUnitTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("JUnit tests for API Explorer.");
    suite.addTestSuite(ApiDirectoryTest.class);
    suite.addTestSuite(ApiRequestTest.class);
    suite.addTestSuite(ApiServiceTest.class);
    suite.addTestSuite(AppStateTest.class);
    suite.addTestSuite(AuthPresenterTest.class);
    suite.addTestSuite(EditorFactoryTest.class);
    suite.addTestSuite(HistoryManagerTest.class);
    suite.addTestSuite(MethodSelectorPresenterTest.class);
    suite.addTestSuite(ParameterFormPresenterTest.class);
    suite.addTestSuite(ServiceLoaderTest.class);
    suite.addTestSuite(ServiceSelectorPresenterTest.class);
    suite.addTestSuite(VersionSelectorPresenterTest.class);
    return suite;
  }
}
