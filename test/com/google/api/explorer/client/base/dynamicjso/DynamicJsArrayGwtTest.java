/*
 * Copyright 2011 Google Inc.
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

import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

/**
 * Tests for {@link DynamicJsArray}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class DynamicJsArrayGwtTest extends BaseGwtTest {
  /** Values stored in JSON strings are accessible through the JSO. */
  public void testDynamicJsArray_getters() {
    DynamicJsArray arr = JsonUtils.safeEval("[\"a\"]");
    assertEquals("a", arr.getString(0));
    assertEquals(JsType.STRING, arr.typeofIndex(0));

    arr = JsonUtils.safeEval("[42]");
    assertEquals(42, arr.getInteger(0));
    assertEquals(JsType.INTEGER, arr.typeofIndex(0));

    arr = JsonUtils.safeEval("[1.2]");
    assertEquals(1.2, arr.getDouble(0));
    assertEquals(JsType.NUMBER, arr.typeofIndex(0));

    arr = JsonUtils.safeEval("[false]");
    assertFalse(arr.getBoolean(0));
    assertEquals(JsType.BOOLEAN, arr.typeofIndex(0));

    arr = JsonUtils.safeEval("[[1,2,3]]");
    assertEquals(1, arr.length());
    DynamicJsArray inner = arr.get(0);
    assertEquals(1, inner.getInteger(0));
    assertEquals(2, inner.getInteger(1));
    assertEquals(3, inner.getInteger(2));
    assertEquals(JsType.ARRAY, arr.typeofIndex(0));

    arr = JsonUtils.safeEval("[{\"foo\":\"bar\"}]");
    assertEquals("bar", arr.<DynamicJso>get(0).getString("foo"));
    assertEquals(JsType.OBJECT, arr.typeofIndex(0));
  }

  /** Array of keys is accessible through the JSO. */
  public void testDynamicJsArray_length() {
    DynamicJsArray arr = JsonUtils.safeEval("[1, \"a\", false]");
    assertEquals(3, arr.length());

    // Length can be increased arbitrarily, but items default to null.
    arr.setLength(6);
    assertEquals(6, arr.length());
    assertNull(arr.get(3));

    // Getting a non-existent element returns null.
    assertNull(arr.get(10));
    assertNull(arr.typeofIndex(10));

    // Array can be shortened, trimming items from the end.
    arr.setLength(1);
    assertEquals(1, arr.length());
    assertEquals(1, arr.getInteger(0));
    assertNull(arr.get(1));
  }

  /** Data can be set on the array and retrieved as expected. */
  public void testDynamicJsArray_setters() {
    DynamicJsArray arr = JavaScriptObject.createArray().cast();

    assertEquals(0, arr.length());

    arr.set(0, "b");
    assertEquals("b", arr.getString(0));
    assertEquals(1, arr.length());

    arr.set(1, false);
    assertFalse(arr.getBoolean(1));

    arr.set(2, 12);
    assertEquals(12, arr.getInteger(2));

    arr.set(3, 2.1);
    assertEquals(2.1, arr.getDouble(3));

    arr.set(4, JavaScriptObject.createObject().cast());
    assertEquals(0, arr.<DynamicJso>get(4).keys().length());

    // All these setters have added to the length.
    assertEquals(5, arr.length());
  }

  /** shift() pops a value off the beginning of the array. */
  public void testDynamicJsArray_shift() {
    DynamicJsArray arr = JsonUtils.safeEval("[false, 1.2, 12, \"a\", {\"a\":\"b\"}]");
    assertEquals(5, arr.length());

    assertFalse(arr.shiftBoolean());
    assertEquals(4, arr.length());

    assertEquals(1.2, arr.shiftDouble());
    assertEquals(3, arr.length());

    assertEquals(12, arr.shiftInteger());
    assertEquals(2, arr.length());

    assertEquals("a", arr.shiftString());
    assertEquals(1, arr.length());

    DynamicJso jso = arr.shift();
    assertEquals(0, arr.length());
    assertEquals(1, jso.keys().length());
    assertEquals("b", jso.getString("a"));

    // With nothing remaining, shift() returns null.
    assertNull(arr.shift());
  }

  /** unshift() adds the value to the beginning of the array. */
  public void testDynamicJsArray_unshift() {
    DynamicJsArray arr = JavaScriptObject.createArray().cast();
    assertEquals(0, arr.length());

    arr.unshift(true);
    assertTrue(arr.getBoolean(0));
    assertEquals(1, arr.length());

    arr.unshift(1.2);
    assertEquals(1.2, arr.getDouble(0));
    assertEquals(2, arr.length());

    arr.unshift(12);
    assertEquals(12, arr.getInteger(0));
    assertEquals(3, arr.length());

    arr.unshift("b");
    assertEquals("b", arr.getString(0));
    assertEquals(4, arr.length());

    JavaScriptObject obj = JavaScriptObject.createObject();
    arr.unshift(obj);
    assertEquals(obj, arr.get(0));
    assertEquals(5, arr.length());
  }

  /**
   * Calls to push() add the value to the end of the array, lengthening it as it
   * goes.
   */
  public void testDynamicJsArray_push() {
    DynamicJsArray arr = JavaScriptObject.createArray().cast();
    assertEquals(0, arr.length());

    arr.push(true);
    assertTrue(arr.getBoolean(0));
    assertEquals(1, arr.length());

    arr.push(1.2);
    assertEquals(1.2, arr.getDouble(1));
    assertEquals(2, arr.length());

    arr.push(12);
    assertEquals(12, arr.getInteger(2));
    assertEquals(3, arr.length());

    arr.push("b");
    assertEquals("b", arr.getString(3));
    assertEquals(4, arr.length());

    JavaScriptObject obj = JavaScriptObject.createObject();
    arr.push(obj);
    assertEquals(obj, arr.get(4));
    assertEquals(5, arr.length());
  }

  /** The type of data stored in the array is accessible as expected. */
  public void testDynamicJsArray_typeof() {
    DynamicJsArray arr = JsonUtils.safeEval("[1.2, 12, \"foo\", false, [\"a\"], {\"fa\":\"bar\"}]");
    assertEquals(JsType.NUMBER, arr.typeofIndex(0));
    assertEquals(JsType.INTEGER, arr.typeofIndex(1));
    assertEquals(JsType.STRING, arr.typeofIndex(2));
    assertEquals(JsType.BOOLEAN, arr.typeofIndex(3));
    assertEquals(JsType.ARRAY, arr.typeofIndex(4));
    assertEquals(JsType.OBJECT, arr.typeofIndex(5));
  }
}
