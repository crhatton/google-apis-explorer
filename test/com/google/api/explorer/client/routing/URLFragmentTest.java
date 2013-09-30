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

import com.google.api.explorer.client.base.TestUrlEncoder;
import com.google.api.explorer.client.base.UrlEncoder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import junit.framework.TestCase;

/**
 * Tests for the {@link URLFragment} class.
 *
 */
public class URLFragmentTest extends TestCase {
  private UrlEncoder originalEncoder;

  @Override
  public void setUp() {
    originalEncoder = URLFragment.urlEncoder;
    URLFragment.urlEncoder = new TestUrlEncoder();
  }

  @Override
  public void tearDown() {
    URLFragment.urlEncoder = originalEncoder;
  }

  public void testParsingNoQueryString() {
    String urlFragment = "p/path/to/a/thing";
    URLFragment parsed = URLFragment.parseFragment(urlFragment);

    assertEquals(urlFragment, parsed.getPath());
    assertTrue(parsed.getQueryString().isEmpty());
  }

  public void testParsingWithQueryString() {
    String urlFragment = "p/path/to/a/thing?aqueryString=true";
    URLFragment parsed = URLFragment.parseFragment(urlFragment);

    assertEquals("p/path/to/a/thing", parsed.getPath());
    assertEquals("aqueryString=true", parsed.getQueryString());

    Multimap<String, String> expected = HashMultimap.create();
    expected.put("aqueryString", "true");
    assertEquals(expected, parsed.getParams());
  }

  public void testParsingParams() {
    String queryParamString = "akey=avalue1&anotherkey=anothervalue&akey=avalue2";
    Multimap<String, String> parsedParams = URLFragment.parseParams(queryParamString);

    Multimap<String, String> expected = HashMultimap.create();
    expected.put("akey", "avalue1");
    expected.put("akey", "avalue2");
    expected.put("anotherkey", "anothervalue");
    assertEquals(expected, parsedParams);
  }

  public void testParsingValuelessParam() {
    String queryParamString = "novalue";
    Multimap<String, String> parsedParams = URLFragment.parseParams(queryParamString);

    Multimap<String, String> expected = HashMultimap.create();
    expected.put("novalue", null);
    assertEquals(expected, parsedParams);
  }

  public void testToString() {
    String pathOnly = "p/path/to/a/thing";
    URLFragment constructed = new URLFragment(pathOnly, null);

    assertEquals(pathOnly, constructed.toString());

    String queryPortion = "aquerykey=avalue";
    constructed.setQueryString(queryPortion);

    assertEquals(pathOnly + URLFragment.QUERY_SEPARATOR + queryPortion, constructed.toString());
  }
}
