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

package com.google.api.explorer.client.selector;

import com.google.common.base.Strings;

/**
 * Represents a selector item's values.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class SelectorItem implements Comparable<SelectorItem> {

  final String text;
  String iconUrl;
  String subtext;
  String url;

  public SelectorItem(String text, String iconUrl, String subtext) {
    this.text = text;
    this.iconUrl = iconUrl;
    this.subtext = subtext;
  }

  public SelectorItem(String text) {
    this(text, "", "");
  }

  /**
   * Makes the subtext a link to the given URL, with the given text.
   *
   * <p>
   * If the URL is null or empty, nothing will happen.
   * </p>
   */
  public SelectorItem setSublink(String subtext, String url) {
    if (!Strings.isNullOrEmpty(url)) {
      this.subtext = subtext;
      this.url = url;
    }
    return this;
  }

  @Override
  public int compareTo(SelectorItem o) {
    return text.compareTo(o.text);
  }
}
