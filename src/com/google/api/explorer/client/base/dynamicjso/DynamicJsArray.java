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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A generic mutable JavaScript Overlay Type that exposes methods to get values
 * of a JavaScript array.
 *
 * <p>
 * This is preferable to GWT's {@link JsArrayMixed} because it adds the
 * {@link #typeofIndex(int)} method, which gives a hint about which getter to
 * use for each element in the array.
 * </p>
 *
 * <p>
 * See {@link
 * "http://code.google.com/webtoolkit/doc/latest/DevGuideCodingBasicsOverlay.html"}
 * for more information about JavaScript overlay types.
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class DynamicJsArray extends JavaScriptObject {

  protected DynamicJsArray() {}

  /** Returns the boolean field found at the given index. */
  public final native boolean getBoolean(int index) /*-{
    return this[index];
  }-*/;

  /** Returns the double field found at the given index. */
  public final native double getDouble(int index) /*-{
    return this[index];
  }-*/;

  /** Returns the integer field found at the given index. */
  public final native int getInteger(int index) /*-{
    return this[index];
  }-*/;

  /** Returns the String found at the given index. */
  public final native String getString(int index) /*-{
    return this[index];
  }-*/;

  /** Returns the nested JavaScript object field found at the given index. */
  public final native <T extends JavaScriptObject> T get(int index) /*-{
    return this[index];
  }-*/;

  /** Returns the length of the array. */
  public final native int length() /*-{
    return this.length;
  }-*/;

  /** Sets the length of the array. */
  public final native void setLength(int length) /*-{
    this.length = length;
  }-*/;

  /**
   * Returns the {@link JsType} corresponding to the type of the value at the
   * given index.
   */
  public final native JsType typeofIndex(int index) /*-{
    var value = this[index];
    if (value == null) {
      return null;
    } else if (typeof value == "string") {
      // Because the value will be wrapped in an Object(), JsType can't tell
      // whether the object is a String or an Object, so we must explicitly
      // check this case beforehand here.
      return @com.google.api.explorer.client.base.dynamicjso.JsType::STRING;
    } else {
      return @com.google.api.explorer.client.base.dynamicjso.JsType::typeof(Ljava/lang/Object;)
      (Object(value));
    }
  }-*/;

  /** Adds the given boolean to the end of the array. */
  public final native void push(boolean value) /*-{
    this[this.length] = value;
  }-*/;

  /** Adds the given double to the end of the array. */
  public final native void push(double value) /*-{
    this[this.length] = value;
  }-*/;

  /** Adds the given integer to the end of the array. */
  public final native void push(int value) /*-{
    this[this.length] = value;
  }-*/;

  /** Adds the given String to the end of the array. */
  public final native void push(String value) /*-{
    this[this.length] = value;
  }-*/;

  /** Adds the given object to the end of the array. */
  public final native void push(JavaScriptObject value) /*-{
    this[this.length] = value;
  }-*/;

  /** Sets the boolean value at the given index. */
  public final native void set(int index, boolean value) /*-{
    this[index] = value;
  }-*/;

  /** Sets the double value at the given index. */
  public final native void set(int index, double value) /*-{
    this[index] = value;
  }-*/;

  /** Sets the integer value at the given index. */
  public final native void set(int index, int value) /*-{
    this[index] = value;
  }-*/;

  /** Sets the String value at the given index. */
  public final native void set(int index, String value) /*-{
    this[index] = value;
  }-*/;

  /** Sets the object value at the given index. */
  public final native void set(int index, JavaScriptObject value) /*-{
    this[index] = value;
  }-*/;

  /** Returns the first value off the array, and removes it from the array. */
  public final native boolean shiftBoolean() /*-{
    return this.shift();
  }-*/;

  /** Returns the first value off the array, and removes it from the array. */
  public final native double shiftDouble() /*-{
    return this.shift();
  }-*/;

  /** Returns the first value off the array, and removes it from the array. */
  public final native int shiftInteger() /*-{
    return this.shift();
  }-*/;

  /** Returns the first value off the array, and removes it from the array. */
  public final native String shiftString() /*-{
    return this.shift();
  }-*/;

  /** Returns the first value off the array, and removes it from the array. */
  public final native <T extends JavaScriptObject> T shift() /*-{
    return this.shift();
  }-*/;

  /** Shifts the given boolean onto the beginning of the array. */
  public final native void unshift(boolean value) /*-{
    this.unshift(value);
  }-*/;

  /** Shifts the given double onto the beginning of the array. */
  public final native void unshift(double value) /*-{
    this.unshift(value);
  }-*/;

  /** Shifts the given integer onto the beginning of the array. */
  public final native void unshift(int value) /*-{
    this.unshift(value);
  }-*/;

  /** Shifts the given String onto the beginning of the array. */
  public final native void unshift(String value) /*-{
    this.unshift(value);
  }-*/;

  /** Shifts the given object onto the beginning of the array. */
  public final native void unshift(JavaScriptObject value) /*-{
    this.unshift(value);
  }-*/;
}
