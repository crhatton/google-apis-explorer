/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.api.explorer.client.routing;

import com.google.common.base.Receiver;
import com.google.gwt.core.client.Callback;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * Tests for the regex match router.
 *
 */
public class RegexMatchRouterTest extends TestCase {
  private static final String ANY_CHARACTER_REGEX = "(.+)";
  private static final String OLD_STYLE_QUERY_STRING = "_some=query&_params=true";
  private static final String STRING_WITH_URL = "_m=athing&shortUrl=http://goo.gl/abc";

  /**
   * Test that the type of routing necessary for the redirect explorer works correctly.
   */
  public void testRedirectUrlRouting() {
    RegexMatchRouter<String> router = new RegexMatchRouter<String>();
    router.addUrlDefinition("{oldStyleQueryString}", new Handler<String>() {
      @Override
      public void handle(Map<String, String> identifiers, String parentUrl, String queryString,
          List<TitleSupplier> titles, Callback<String, String> callback) {
        callback.onSuccess(identifiers.get("oldStyleQueryString"));
      }

      @Override
      public void getTitle(Map<String, String> identifiers, UrlBuilder urlBuilder,
          boolean isFromSearch, Receiver<Title> callback) {
        callback.accept(new Title("Title", null, urlBuilder.toString()));
      }
    }, ANY_CHARACTER_REGEX);

    checkUrlRouting(OLD_STYLE_QUERY_STRING, router);
    checkUrlRouting(STRING_WITH_URL, router);
  }

  /**
   * Check that a given URL comes through in full given the initialized router. Used in the for the
   * redirect explorer.
   */
  private void checkUrlRouting(String testUrl, RegexMatchRouter<String> router) {
    final StringBuilder resultsBuilder = new StringBuilder();
    router.route(testUrl, new Callback<String, String>() {
      @Override
      public void onFailure(String reason) {
        fail();
      }

      @Override
      public void onSuccess(String result) {
        resultsBuilder.append(result);
      }
    });

    assertEquals(testUrl, resultsBuilder.toString());
  }
}
