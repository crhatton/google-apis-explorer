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

import com.google.api.explorer.client.auth.AuthPresenterTest;
import com.google.api.explorer.client.base.ApiDirectoryTest;
import com.google.api.explorer.client.base.ApiRequestTest;
import com.google.api.explorer.client.base.ApiServiceHelperTest;
import com.google.api.explorer.client.base.SchemaTest;
import com.google.api.explorer.client.base.ServiceLoaderTest;
import com.google.api.explorer.client.base.rest.RestApiRequestTest;
import com.google.api.explorer.client.base.rest.RestApiServiceTest;
import com.google.api.explorer.client.base.rpc.RpcApiServiceTest;
import com.google.api.explorer.client.editors.EditorFactoryTest;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenterTest;
import com.google.api.explorer.client.history.JsonPrettifierTest;
import com.google.api.explorer.client.routing.RegexMatchRouterTest;
import com.google.api.explorer.client.routing.URLBuilderTest;
import com.google.api.explorer.client.routing.URLFragmentTest;
import com.google.api.explorer.client.routing.handler.HistoryManagerTest;
import com.google.api.explorer.client.search.DiscoveryFullTextIndexingStrategyTest;
import com.google.api.explorer.client.search.KeywordExtractorTest;
import com.google.api.explorer.client.search.SearchEntryTest;
import com.google.api.explorer.client.search.SearchResultIndexTest;

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
    suite.addTestSuite(DiscoveryFullTextIndexingStrategyTest.class);
    suite.addTestSuite(RestApiRequestTest.class);
    suite.addTestSuite(RestApiServiceTest.class);
    suite.addTestSuite(RpcApiServiceTest.class);
    suite.addTestSuite(AuthPresenterTest.class);
    suite.addTestSuite(EditorFactoryTest.class);
    suite.addTestSuite(HistoryManagerTest.class);
    suite.addTestSuite(RegexMatchRouterTest.class);
    suite.addTestSuite(URLBuilderTest.class);
    suite.addTestSuite(URLFragmentTest.class);
    suite.addTestSuite(JsonPrettifierTest.class);
    suite.addTestSuite(KeywordExtractorTest.class);
    suite.addTestSuite(ApiServiceHelperTest.class);
    suite.addTestSuite(EmbeddedParameterFormPresenterTest.class);
    suite.addTestSuite(SearchEntryTest.class);
    suite.addTestSuite(SearchResultIndexTest.class);
    suite.addTestSuite(ServiceLoaderTest.class);
    suite.addTestSuite(SchemaTest.class);
    return suite;
  }
}
