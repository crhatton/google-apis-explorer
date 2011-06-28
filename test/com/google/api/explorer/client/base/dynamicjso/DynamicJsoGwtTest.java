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

import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsonUtils;

/**
 * Tests for {@link DynamicJso}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class DynamicJsoGwtTest extends BaseGwtTest {

  /** Values stored in JSON strings are accessible through the JSO. */
  public void testDynamicJso_getters() {
    DynamicJso jso = JsonUtils.safeEval("{\"a\":\"b\"}");
    assertEquals("b", jso.getString("a"));
    assertEquals(JsType.STRING, jso.typeofKey("a"));

    jso = JsonUtils.safeEval("{\"a\":42}");
    assertEquals(42, jso.getInteger("a"));
    assertEquals(JsType.INTEGER, jso.typeofKey("a"));

    jso = JsonUtils.safeEval("{\"a\":1.2}");
    assertEquals(1.2, jso.getDouble("a"));
    assertEquals(JsType.NUMBER, jso.typeofKey("a"));

    jso = JsonUtils.safeEval("{\"a\":false}");
    assertFalse(jso.getBoolean("a"));
    assertEquals(JsType.BOOLEAN, jso.typeofKey("a"));

    jso = JsonUtils.safeEval("{\"a\":[1,2,3]}");
    JsArrayNumber arr = jso.get("a");
    assertEquals(1.0, arr.get(0));
    assertEquals(2.0, arr.get(1));
    assertEquals(3.0, arr.get(2));
    assertEquals(JsType.ARRAY, jso.typeofKey("a"));

    jso = JsonUtils.safeEval("{\"a\":{\"foo\":\"bar\"}}");
    assertEquals("bar", jso.<DynamicJso>get("a").getString("foo"));
    assertEquals(JsType.OBJECT, jso.typeofKey("a"));
  }

  /** Array of keys is accessible through the JSO. */
  public void testDynamicJso_keys() {
    DynamicJso jso = JsonUtils.safeEval("{\"a\":{\"foo\":\"bar\"}}");
    assertEquals(1, jso.keys().length());
    assertEquals("a", jso.keys().get(0));

    jso = JavaScriptObject.createObject().cast();
    jso.set("a", true);
    jso.set("b", false);
    jso.set("c", 123);
    assertEquals(3, jso.keys().length());
    assertEquals("a", jso.keys().get(0));
    assertEquals("b", jso.keys().get(1));
    assertEquals("c", jso.keys().get(2));

    // Getting a non-existent key return null
    assertNull(jso.get("zzz"));
    assertNull(jso.typeofKey("zzz"));
  }

  /** Data can be set on the object and retrieved as expected. */
  public void testDynamicJso_setters() {
    DynamicJso jso = JavaScriptObject.createObject().cast();

    assertEquals(0, jso.keys().length());

    jso.set("a", "b");
    assertEquals("b", jso.getString("a"));
    assertEquals(1, jso.keys().length());

    jso.set("bool", false);
    assertFalse(jso.getBoolean("bool"));

    jso.set("int", 12);
    assertEquals(12, jso.getInteger("int"));

    jso.set("double", 2.1);
    assertEquals(2.1, jso.getDouble("double"));

    jso.set("obj", JavaScriptObject.createObject().cast());
    assertEquals(0, jso.<DynamicJso>get("obj").keys().length());
    assertEquals(JsType.OBJECT, jso.typeofKey("obj"));

    // All these setters have added keys to the object
    assertEquals(5, jso.keys().length());
  }

  /** Data can be cleared from the object. */
  public void testDynamicJso_clear() {
    DynamicJso jso = JavaScriptObject.createObject().cast();
    jso.set("a", true);
    assertTrue(jso.getBoolean("a"));
    assertEquals(1, jso.keys().length());

    assertTrue(jso.clear("a"));
    assertNull(jso.getString("a"));
    assertEquals(0, jso.keys().length());
  }

  /** The type of data stored in the object is accessible as expected. */
  public void testDynamicJso_typeof() {
    DynamicJso jso = JsonUtils.safeEval(
        "{\"a\":1.2,\"b\":12,\"c\":\"foo\",\"d\":false,\"e\":[\"a\"],\"f\":{\"fa\":\"bar\"}}");
    assertEquals(JsType.NUMBER, jso.typeofKey("a"));
    assertEquals(JsType.INTEGER, jso.typeofKey("b"));
    assertEquals(JsType.STRING, jso.typeofKey("c"));
    assertEquals(JsType.BOOLEAN, jso.typeofKey("d"));
    assertEquals(JsType.ARRAY, jso.typeofKey("e"));
    assertEquals(JsType.OBJECT, jso.typeofKey("f"));
    assertEquals(JsType.STRING, jso.<DynamicJso>get("f").typeofKey("fa"));
  }
}
