/*
 * Copyright 2010 Google Inc.
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

package com.google.api.explorer.client.base.dynamicjso;

/**
 * JavaScript data types.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public enum JsType {
  ARRAY, BOOLEAN, INTEGER, NUMBER, OBJECT, STRING;

  /**
   * Returns the {@link JsType} of the object, or {@code null} if no value could
   * be discerned.
   *
   * <p>
   * If the object is {@code null}, or a String, this method will return
   * {@link JsType#OBJECT}, so these particular cases should be checked
   * beforehand.
   * </p>
   */
  static final native JsType typeof(Object obj) /*-{
    if (obj instanceof Number) {
      if (Math.floor(obj) == obj) {
        return @com.google.api.explorer.client.base.dynamicjso.JsType::INTEGER;
      } else {
        return @com.google.api.explorer.client.base.dynamicjso.JsType::NUMBER;
      }
    } else if (obj instanceof Boolean) {
      return @com.google.api.explorer.client.base.dynamicjso.JsType::BOOLEAN;
    } else {
      var isArray = obj instanceof Array ||
          (!(obj instanceof Object)
              && (Object.prototype.toString.call(obj) === '[object Array]') ||
          typeof obj.length === 'number' &&
          typeof obj.splice !== 'undefined' &&
          typeof obj.propertyIsEnumerable !== 'undefined' &&
          !obj.propertyIsEnumerable('splice'));
      if (isArray) {
        return @com.google.api.explorer.client.base.dynamicjso.JsType::ARRAY;
      } else {
        return @com.google.api.explorer.client.base.dynamicjso.JsType::OBJECT;
      }
    }
    return null;
  }-*/;

}
