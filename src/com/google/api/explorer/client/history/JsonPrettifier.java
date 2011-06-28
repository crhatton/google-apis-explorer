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

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.Resources.Css;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.dynamicjso.DynamicJsArray;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.base.dynamicjso.JsType;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple syntax highlighter for JSON data.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
// TODO(jasonhall): Add collapsible sections for objects and arrays.
class JsonPrettifier {

  // Indent one space per indentation level (matches real response)
  private static final int INDENT_BY = 1;
  private static final Document DOCUMENT = Document.get();
  private static final Css STYLE = Resources.INSTANCE.style();

  public static AppState appState;

  // Current indentation level.
  private static int indent = 0;

  /**
   * Appends a syntax-highlighted DOM structure based on the JSON string to the
   * parent element. If any error is encountered, such as the string not being
   * valid JSON, or anything else, the text will simply be added to the parent
   * element unhighlighted.
   */
  public static void syntaxHighlight(Element parent, String jsonString) {
    // Reset indentation (just in case)
    indent = 0;
    if (!GWT.isScript()) {
      // Syntax highlighting is *very* slow in Development Mode (~30s for large
      // responses), but very fast when compiled and run as JS (~30ms). For the
      // sake of my sanity, syntax highlighting is disabled in Development Mode.
      parent.setInnerText(jsonString);
    } else {

      try {
        parent.appendChild(objectToElement(JsonUtils.<DynamicJso>safeEval(jsonString)));
      } catch (Exception e) {
        // As a fallback in case anything goes wrong, just set the inner text
        // without any highlighting.
        parent.setInnerText(jsonString);
      }
    }
  }

  private static SpanElement wrapInSpan(String value) {
    return wrapInSpan(value, "");
  }

  private static SpanElement wrapInSpan(String value, String className) {
    SpanElement span = DOCUMENT.createSpanElement();
    if (!className.isEmpty()) {
      span.setClassName(className);
    }
    span.setInnerText(value);
    return span;
  }

  private static AnchorElement wrapInAnchor(String url) {
    Map.Entry<String, ApiMethod> entry = getMethodForUrl(url);
    if (entry != null) {
      try {
        return wrapInExplorerAnchor(url, entry);
      } catch (Exception e) {
        // Fall through here in case the Explorer link cannot be successfully
        // constructed. The link will be treated as a regular link.
      }
    }
    AnchorElement a = DOCUMENT.createAnchorElement();
    a.setInnerText(url);
    a.setClassName(STYLE.responseLine() + " " + STYLE.jsonStringLink());
    a.setHref(url);
    a.setTarget("_blank");
    return a;
  }

  /**
   * Attempts to identify an {@link ApiMethod} corresponding to the given url.
   * If one is found, a {@link Map.Entry} will be returned where the key is the
   * name of the method, and the value is the {@link ApiMethod} itself. If no
   * method is found, this will return {@code null}.
   */
  private static Map.Entry<String, ApiMethod> getMethodForUrl(String url) {
    String apiLinkPrefix = Config.getBaseUrl() + appState.getCurrentService().getBasePath();
    if (!url.startsWith(apiLinkPrefix)) {
      return null;
    }

    // Only check GET methods since those are the only ones that can be returned
    // in the response.
    Iterable<Map.Entry<String, ApiMethod>> getMethods =
        Iterables.filter(appState.getCurrentService().allMethods().entrySet(),
            new Predicate<Map.Entry<String, ApiMethod>>() {
              @Override
              public boolean apply(Entry<String, ApiMethod> input) {
                return input.getValue().getHttpMethod() == HttpMethod.GET;
              }
            });
    for (Map.Entry<String, ApiMethod> entry : getMethods) {
      int paramIndex = url.indexOf("?");
      String path = url.substring(0, paramIndex > 0 ? paramIndex : url.length());
      // Try to match the request URL with its method by comparing it to the
      // method's rest base path URI template. To do this we have to remove the
      // {...} placeholders.
      String regex =
          apiLinkPrefix + entry.getValue().getPath().replaceAll("\\{[^\\/]+\\}", "[^\\/]+");
      if (path.matches(regex)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Wraps the URL in an anchor that points to the {@link ApiMethod} contained
   * in the entry. This assumes that the entry is non-null, signifying that a
   * relevant method to link to has been identified.
   */
  private static AnchorElement wrapInExplorerAnchor(String url, Entry<String, ApiMethod> entry) {
    AnchorElement a = DOCUMENT.createAnchorElement();
    a.setInnerText(url);
    a.setClassName(STYLE.responseLine() + " " + STYLE.jsonStringExplorerLink());
    a.setHref(createExplorerLink(url, entry));
    a.setTitle("Click to load this request in the APIs Explorer");
    return a;
  }

  /**
   * Creates an Explorer link token (e.g.,
   * #_s=<service>&_v=<version>&_m=<method>) corresponding to the given request
   * URL, given the method name and method definition returned by
   * {@link #getMethodForUrl(String)}.
   */
  private static String createExplorerLink(String url, Entry<String, ApiMethod> entry) {
    String path = url.substring(url.indexOf('?') + 1);
    StringBuilder tokenBuilder = new StringBuilder()
        .append("#_s=")
        .append(appState.getCurrentService().getName())
        .append("&_v=")
        .append(appState.getCurrentService().getVersion())
        .append("&_m=")
        .append(entry.getKey())
        .append("&")
        .append(path);

    String pathTemplate = entry.getValue().getPath();
    if (pathTemplate.contains("{")) {
      String urlPath = url.replaceFirst(
          Config.getBaseUrl() + appState.getCurrentService().getBasePath(), "");
      if (urlPath.contains("?")) {
        urlPath = urlPath.substring(0, urlPath.indexOf('?'));
      }
      String[] templateSections = pathTemplate.split("/");
      String[] urlSections = urlPath.split("/");
      for (int i = 0; i < templateSections.length; i++) {
        if (templateSections[i].contains("{")) {
          String paramName = templateSections[i].substring(1, templateSections[i].length() - 1);
          tokenBuilder.append("&").append(paramName).append("=").append(urlSections[i]);
        }
      }
    }
    return tokenBuilder.toString();
  }

  /** Syntax highlights an array element based on its type. */
  private static SpanElement arrValueToElement(DynamicJsArray arr, int index) {
    JsType type = arr.typeofIndex(index);
    if (type == null) {
      return wrapInSpan("null", STYLE.jsonNull());
    }
    switch (type) {
      case NUMBER:
        return wrapInSpan(String.valueOf(arr.getDouble(index)), STYLE.jsonNumber());
      case INTEGER:
        return wrapInSpan(String.valueOf(arr.getInteger(index)), STYLE.jsonNumber());
      case BOOLEAN:
        return wrapInSpan(String.valueOf(arr.getBoolean(index)), STYLE.jsonBoolean());
      case STRING:
        return wrapStringValue(arr.getString(index));
      case ARRAY:
        return arrayToElement(arr.<DynamicJsArray>get(index));
      case OBJECT:
        return objectToElement(arr.<DynamicJso>get(index));
    }
    return DOCUMENT.createSpanElement();
  }

  /**
   * Wraps a string value in a span. If the string value represents a link, the
   * string will be made into a link by {@link #wrapInAnchor(String)}.
   */
  private static SpanElement wrapStringValue(String value) {
    if (isLink(value)) {
      SpanElement span = DOCUMENT.createSpanElement();
      span.appendChild(wrapInSpan("\""));
      span.appendChild(wrapInAnchor(value));
      span.appendChild(wrapInSpan("\""));
      return span;
    } else {
      return wrapInSpan("\"" + fixQuotes(value) + "\"",
          STYLE.jsonString() + " " + STYLE.responseLine());
    }
  }

  /**
   * Fix quotes in JSON strings by escaping them. Without this, the JSON we
   * display will not be valid JSON.
   */
  private static String fixQuotes(String value) {
    return value.replace("\"", "\\\"");

  }

  // TODO(jasonhall): This could be more robust.
  private static boolean isLink(String value) {
    return (value.startsWith("http://") || value.startsWith("https://")) && !value.contains("\n")
        && !value.contains("\t");
  }

  /** Syntax highlights an array. */
  private static SpanElement arrayToElement(DynamicJsArray arr) {
    SpanElement span = DOCUMENT.createSpanElement();
    if (arr.length() == 0) {
      span.setInnerText("[ ]");
      return span;
    }

    span.appendChild(wrapInSpan("["));
    span.appendChild(DOCUMENT.createBRElement());

    indent();
    span.appendChild(wrapInSpan(indentationSpaces()));

    for (int i = 0; i < arr.length(); i++) {
      SpanElement item = DOCUMENT.createSpanElement();
      if (i > 0) {
        span.appendChild(wrapInSpan(","));
      }
      item.appendChild(arrValueToElement(arr, i));
      span.appendChild(item);
    }
    dedent();

    span.appendChild(DOCUMENT.createBRElement());
    span.appendChild(wrapInSpan(indentationSpaces() + "]"));
    return span;
  }

  /** Syntax highlights an object's value based on its type. */
  private static SpanElement objValueToElement(DynamicJso obj, String key) {
    JsType type = obj.typeofKey(key);
    if (type == null) {
      return wrapInSpan("null", STYLE.jsonNull());
    }
    switch (type) {
      case NUMBER:
        return wrapInSpan(String.valueOf(obj.getDouble(key)), STYLE.jsonNumber());
      case INTEGER:
        return wrapInSpan(String.valueOf(obj.getInteger(key)), STYLE.jsonNumber());
      case BOOLEAN:
        return wrapInSpan(String.valueOf(obj.getBoolean(key)), STYLE.jsonBoolean());
      case STRING:
        return wrapStringValue(obj.getString(key));
      case ARRAY:
        return arrayToElement(obj.<DynamicJsArray>get(key));
      case OBJECT:
        return objectToElement(obj.<DynamicJso>get(key));
    }
    return DOCUMENT.createSpanElement();
  }

  /** Syntax highlights an object. */
  private static SpanElement objectToElement(DynamicJso obj) {
    JsArrayString keys = obj.keys();
    SpanElement span = DOCUMENT.createSpanElement();

    if (keys.length() == 0) {
      span.setInnerText("{ }");
      return span;
    }

    span.appendChild(wrapInSpan("{"));

    indent();
    for (int i = 0; i < keys.length(); i++) {
      String key = keys.get(i);
      SpanElement item = DOCUMENT.createSpanElement();
      item.appendChild(wrapInSpan(indentationSpaces() + "\""));
      item.appendChild(wrapInSpan(key, STYLE.jsonKey()));
      item.appendChild(wrapInSpan("\": "));
      item.appendChild(objValueToElement(obj, key));
      if (i < keys.length() - 1) {
        item.appendChild(wrapInSpan(","));
      }
      span.appendChild(DOCUMENT.createBRElement());
      span.appendChild(item);
    }
    dedent();

    span.appendChild(DOCUMENT.createBRElement());
    span.appendChild(wrapInSpan(indentationSpaces() + "}"));
    return span;
  }

  private static void indent() {
    indent += INDENT_BY;
  }

  private static void dedent() {
    indent -= INDENT_BY;
  }

  /**
   * Returns a string filled with a number of spaces corresponding to the
   * current indentation level.
   */
  private static String indentationSpaces() {
    return Strings.repeat(" ", indent);
  }
}
