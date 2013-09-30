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

package com.google.api.explorer.client.history;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.TestUrlEncoder;
import com.google.api.explorer.client.base.UrlEncoder;
import com.google.api.explorer.client.base.rest.RestApiService;
import com.google.api.explorer.client.routing.UrlBuilder;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Map;

/**
 * Tests for the JsonPreffifier
 *
 */
public class JsonPrettifierTest extends TestCase {
  private static final String PLUS_BASE_PATH = "/plus/v1/";
  private static final String PLUS_LINK =
      "https://www.googleapis.com/plus/v1/people/123456789/activities/public?";
  private static final String EXPLORER_LINK =
      "s/plus/v1/plus.activities.list?userId=123456789&collection=public&";
  private static final String LIST_METHOD_NAME = "plus.activities.list";
  private static final String LIST_METHOD_PATH = "people/{userId}/activities/{collection}";
  private static final String PLUS_NAME = "plus";
  private static final String PLUS_VERSION = "v1";

  private ApiService plusService;
  private UrlEncoder originalEncoder;

  @Override
  public void setUp() {
    ApiMethod listActivities = EasyMock.createNiceMock(ApiMethod.class);
    expect(listActivities.getHttpMethod()).andReturn(HttpMethod.GET).anyTimes();
    expect(listActivities.getPath()).andReturn(LIST_METHOD_PATH).anyTimes();
    expect(listActivities.getId()).andReturn(LIST_METHOD_NAME).anyTimes();
    replay(listActivities);

    Map<String, ApiMethod> allActivities = Maps.newHashMap();
    allActivities.put(LIST_METHOD_NAME, listActivities);

    plusService = EasyMock.createNiceMock(RestApiService.class);
    expect(plusService.basePath()).andReturn(PLUS_BASE_PATH).anyTimes();
    expect(plusService.allMethods()).andReturn(allActivities).anyTimes();
    expect(plusService.getName()).andReturn(PLUS_NAME).anyTimes();
    expect(plusService.getVersion()).andReturn(PLUS_VERSION).anyTimes();
    replay(plusService);

    originalEncoder = UrlBuilder.urlEncoder;
    UrlBuilder.urlEncoder = new TestUrlEncoder();
  }

  @Override
  public void tearDown() {
    UrlBuilder.urlEncoder = originalEncoder;
  }

  /**
   * Test the identification of explorer links
   */
  public void testExplorerLinks() {
    ApiMethod method = JsonPrettifier.getMethodForUrl(plusService, PLUS_LINK);
    assertNotNull(method);
    assertEquals(LIST_METHOD_NAME, method.getId());
    assertEquals(HttpMethod.GET, method.getHttpMethod());

    String link = JsonPrettifier.createExplorerLink(plusService, PLUS_LINK, method);
    assertEquals(EXPLORER_LINK, link);
  }
}
