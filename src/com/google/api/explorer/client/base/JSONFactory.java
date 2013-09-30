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

package com.google.api.explorer.client.base;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Allows swappable JSON implementations at test time. The default
 * implementation uses the built in GWT JSON classes.
 *
 */
public class JSONFactory {
  /** Create a new JSONString instance */
  public JSONString newJSONString(String value) {
    return new JSONString(value);
  }

  /** Create a new JSONObject instance */
  public JSONObject newJSONObject() {
    return new JSONObject();
  }

  /** Create a new JSONArray instance */
  public JSONArray newJSONArray() {
    return new JSONArray();
  }
}
