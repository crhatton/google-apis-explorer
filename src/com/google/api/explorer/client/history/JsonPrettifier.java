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

import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.Resources.Css;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.dynamicjso.DynamicJsArray;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.base.dynamicjso.JsType;
import com.google.api.explorer.client.routing.HistoryWrapper;
import com.google.api.explorer.client.routing.HistoryWrapperImpl;
import com.google.api.explorer.client.routing.URLFragment;
import com.google.api.explorer.client.routing.UrlBuilder;
import com.google.api.explorer.client.routing.UrlBuilder.RootNavigationItem;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * A simple syntax highlighter for JSON data.
 *
 */
public class JsonPrettifier {
  /**
   * Class that we can use to re-write runtime Json exceptions to checked.
   */
  public static class JsonFormatException extends Exception {
    private JsonFormatException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static final String PLACEHOLDER_TEXT = "...";
  private static final String SEPARATOR_TEXT = ",";
  private static final String OPEN_IN_NEW_WINDOW = "_blank";
  private static final HistoryWrapper history = new HistoryWrapperImpl();

  private static Css style;
  private static Resources resources;

  /**
   * Factory that can be used to manufacture link information that can vary between the full and
   * embedded explorer.
   */
  public interface PrettifierLinkFactory {
    /**
     * Generate a click handler that will redirect to the fragment specified when invoked.
     */
    ClickHandler generateMenuHandler(String fragment);

    /**
     * Generate an anchor widget which will redirect to the fragment specified when clicked.
     */
    Widget generateAnchor(String embeddingText, String fragment);
  }

  /**
   * Link factory which generates links which manipulate the current browser view. This should be
   * used for the explorer full-view context.
   */
  public static final PrettifierLinkFactory LOCAL_LINK_FACTORY = new PrettifierLinkFactory() {
    @Override
    public ClickHandler generateMenuHandler(final String fragment) {
      return new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          history.newItem(fragment);
        }
      };
    }

    @Override
    public Widget generateAnchor(String embeddingText, String fragment) {
      return new InlineHyperlink(embeddingText, fragment);
    }
  };

  /**
   * Link factory which generates links which either open a new tab, or switch the entire URL to a
   * new location. This should be used with the embedded explorer context.
   */
  public static final PrettifierLinkFactory EXTERNAL_LINK_FACTORY = new PrettifierLinkFactory() {
    @Override
    public ClickHandler generateMenuHandler(final String fragment) {
      return new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.open(createFullLink(fragment), "_blank", null);
        }
      };
    }

    @Override
    public Widget generateAnchor(String embeddingText, String fragment) {
      return new Anchor(embeddingText, createFullLink(fragment));
    }

    private String createFullLink(String fragment) {
      return Config.EXPLORER_URL + "#" + fragment;
    }
  };


  private static class Collapser implements ClickHandler {
    private final Widget toHide;
    private final Widget placeHolder;
    private final Widget clicker;

    public Collapser(Widget toHide, Widget placeHolder, Widget clicker) {
      this.toHide = toHide;
      this.placeHolder = placeHolder;
      this.clicker = clicker;
    }

    @Override
    public void onClick(ClickEvent arg0) {
      boolean makeVisible = !toHide.isVisible();
      decorateCollapserControl(clicker, makeVisible);
      toHide.setVisible(makeVisible);
      placeHolder.setVisible(!makeVisible);
    }

    public static void decorateCollapserControl(Widget collapser, boolean visible) {
      if (visible) {
        collapser.addStyleName(style.jsonExpanded());
        collapser.removeStyleName(style.jsonCollapsed());
      } else {
        collapser.addStyleName(style.jsonCollapsed());
        collapser.removeStyleName(style.jsonExpanded());
      }
    }
  }

  /**
   * This abstraction of an array creates formatted widgets from all children.
   */
  private static class JsArrayIterable implements Iterable<Widget> {
    private final DynamicJsArray backingObj;
    private final int depth;
    private final ApiService service;
    private final PrettifierLinkFactory linkFactory;

    public JsArrayIterable(
        ApiService service, DynamicJsArray array, int depth, PrettifierLinkFactory linkFactory) {
      this.backingObj = array;
      this.depth = depth;
      this.service = service;
      this.linkFactory = linkFactory;
    }

    @Override
    public Iterator<Widget> iterator() {
      return new Iterator<Widget>() {
        private int nextOffset = 0;

        @Override
        public boolean hasNext() {
          return nextOffset < backingObj.length();
        }

        @Override
        public Widget next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          Widget next = formatArrayValue(service,
              backingObj,
              nextOffset,
              depth,
              nextOffset + 1 < backingObj.length(),
              linkFactory);
          nextOffset++;
          return next;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  /**
   * This abstraction of an object creates formatted widgets from all children.
   */
  private static class JsObjectIterable implements Iterable<Widget> {
    private final DynamicJso backingObj;
    private final int depth;
    private final ApiService service;
    private final PrettifierLinkFactory linkFactory;

    public JsObjectIterable(
        ApiService service, DynamicJso obj, int depth, PrettifierLinkFactory linkFactory) {

      this.backingObj = obj;
      this.depth = depth;
      this.service = service;
      this.linkFactory = linkFactory;
    }

    @Override
    public Iterator<Widget> iterator() {
      return new Iterator<Widget>() {
         int nextOffset = 0;

        @Override
        public boolean hasNext() {
          return nextOffset < backingObj.keys().length();
        }

        @Override
        public Widget next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          Widget next =
              formatValue(service, backingObj, backingObj.keys().get(nextOffset), depth,
                  nextOffset + 1 < backingObj.keys().length(), linkFactory);
          nextOffset++;
          return next;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  /**
   * Must be called before calling prettify to set the resources file to be used. Makes it possible
   * to test this class under JUnit.
   *
   * @param resources Resources (images and style) to use when prettifying.
   */
  public static void setResources(Resources resources) {
    JsonPrettifier.resources = resources;
    JsonPrettifier.style = resources.style();
  }

  /**
   * Entry point for the formatter.
   *
   * @param destination Destination GWT object where the results will be placed
   * @param jsonString String to format
   * @param linkFactory Which links factory should be used when generating links and navigation
   *        menus.
   * @throws JsonFormatException when parsing the Json causes an error
   */
  public static void prettify(
      ApiService service, Panel destination, String jsonString, PrettifierLinkFactory linkFactory)
      throws JsonFormatException {

    // Make sure the user set a style before invoking prettify.
    Preconditions.checkState(style != null, "Must call setStyle before using.");

    Preconditions.checkNotNull(service);
    Preconditions.checkNotNull(destination);

    // Don't bother syntax highlighting empty text.
    boolean empty = Strings.isNullOrEmpty(jsonString);
    destination.setVisible(!empty);
    if (empty) {
      return;
    }

    if (!GWT.isScript()) {
      // Syntax highlighting is *very* slow in Development Mode (~30s for large
      // responses), but very fast when compiled and run as JS (~30ms). For the
      // sake of my sanity, syntax highlighting is disabled in Development
      destination.add(new InlineLabel(jsonString));
    } else {

      try {
        DynamicJso root = JsonUtils.<DynamicJso>safeEval(jsonString);
        Collection<ApiMethod> compatibleMethods = computeCompatibleMethods(root, service);
        Widget menuForMethods = createRequestMenu(compatibleMethods, service, root, linkFactory);
        JsObjectIterable rootObject = new JsObjectIterable(service, root, 1, linkFactory);
        Widget object = formatGroup(rootObject, "", 0, "{", "}", false, menuForMethods);
        destination.add(object);
      } catch (IllegalArgumentException e) {
        // JsonUtils will throw an IllegalArgumentException when it gets invalid
        // Json data. Rewrite as a checked exception and throw.
        throw new JsonFormatException("Invalid json.", e);
      }
    }
  }

  /**
   * Check the provided javascript object for a "kind" key and, and find all methods from the
   * provided service that accept the specified type for the request body.
   *
   * @param object Object which is checked against other methods.
   * @param service Service for which we want to find compatible methods.
   * @return Matching methods that accept the object type as an input, or an empty collection.
   */
  private static Collection<ApiMethod> computeCompatibleMethods(
      DynamicJso object, ApiService service) {

    String kind = object.getString(Schema.KIND_KEY);
    if (kind != null) {
      return service.usagesOfKind(kind);
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Iterate through an object or array adding the widgets generated for all children
   */
  private static FlowPanel formatGroup(Iterable<Widget> objIterable,
      String title,
      int depth,
      String openGroup,
      String closeGroup,
      boolean hasSeparator,
      @Nullable Widget menuButtonForReuse) {

    FlowPanel object = new FlowPanel();

    FlowPanel titlePanel = new FlowPanel();
    Label paddingSpaces = new InlineLabel(indentation(depth));
    titlePanel.add(paddingSpaces);

    Label titleLabel = new InlineLabel(title + openGroup);
    titleLabel.addStyleName(style.jsonKey());
    Collapser.decorateCollapserControl(titleLabel, true);
    titlePanel.add(titleLabel);

    object.add(titlePanel);

    FlowPanel objectContents = new FlowPanel();

    if (menuButtonForReuse != null) {
      objectContents.addStyleName(style.reusableResource());
      objectContents.add(menuButtonForReuse);
    }

    for (Widget child : objIterable) {
      objectContents.add(child);
    }
    object.add(objectContents);

    InlineLabel placeholder = new InlineLabel(indentation(depth + 1) + PLACEHOLDER_TEXT);
    ClickHandler collapsingHandler = new Collapser(objectContents, placeholder, titleLabel);
    placeholder.setVisible(false);
    placeholder.addClickHandler(collapsingHandler);
    object.add(placeholder);

    titleLabel.addClickHandler(collapsingHandler);

    StringBuilder closingLabelText = new StringBuilder(indentation(depth)).append(closeGroup);
    if (hasSeparator) {
      closingLabelText.append(SEPARATOR_TEXT);
    }

    object.add(new Label(closingLabelText.toString()));

    return object;
  }

  private static Widget formatArrayValue(ApiService service,
      DynamicJsArray obj,
      int index,
      int depth,
      boolean hasSeparator,
      PrettifierLinkFactory linkFactory) {

    JsType type = obj.typeofIndex(index);
    if (type == null) {
      return simpleInline("", "null", style.jsonNull(), depth, hasSeparator);
    }
    String title = "";
    switch (type) {
      case NUMBER:
        return simpleInline(
            title, String.valueOf(obj.getDouble(index)), style.jsonNumber(), depth, hasSeparator);

      case INTEGER:
        return simpleInline(
            title, String.valueOf(obj.getInteger(index)), style.jsonNumber(), depth, hasSeparator);

      case BOOLEAN:
        return simpleInline(
            title, String.valueOf(obj.getBoolean(index)), style.jsonBoolean(), depth, hasSeparator);

      case STRING:
        return inlineWidget(
            title, formatString(service, obj.getString(index), linkFactory), depth, hasSeparator);

      case ARRAY:
        return formatGroup(
            new JsArrayIterable(service, obj.<DynamicJsArray>get(index), depth + 1, linkFactory),
            title, depth, "[", "]", hasSeparator, null);

      case OBJECT:
        DynamicJso subObject = obj.<DynamicJso>get(index);

        // Determine if this object can be used as the request parameter for another method.
        Collection<ApiMethod> compatibleMethods = computeCompatibleMethods(subObject, service);
        Widget menuFromMethods =
            createRequestMenu(compatibleMethods, service, subObject, linkFactory);
        JsObjectIterable objIter = new JsObjectIterable(service, subObject, depth + 1, linkFactory);
        return formatGroup(objIter, title, depth, "{", "}", hasSeparator, menuFromMethods);
    }
    return new FlowPanel();
  }

  private static Widget formatValue(ApiService service,
      DynamicJso obj,
      String key,
      int depth,
      boolean hasSeparator,
      PrettifierLinkFactory linkFactory) {

    JsType type = obj.typeofKey(key);
    if (type == null) {
      return simpleInline(titleString(key), "null", style.jsonNull(), depth, hasSeparator);
    }
    String title = titleString(key);
    switch (type) {
      case NUMBER:
        return simpleInline(
            title, String.valueOf(obj.getDouble(key)), style.jsonNumber(), depth, hasSeparator);

      case INTEGER:
        return simpleInline(
            title, String.valueOf(obj.getInteger(key)), style.jsonNumber(), depth, hasSeparator);

      case BOOLEAN:
        return simpleInline(
            title, String.valueOf(obj.getBoolean(key)), style.jsonBoolean(), depth, hasSeparator);

      case STRING:
        return inlineWidget(
            title, formatString(service, obj.getString(key), linkFactory), depth, hasSeparator);

      case ARRAY:
        return formatGroup(
            new JsArrayIterable(service, obj.<DynamicJsArray>get(key), depth + 1, linkFactory),
            title, depth, "[", "]", hasSeparator, null);

      case OBJECT:
        DynamicJso subObject = obj.<DynamicJso>get(key);

        // Determine if this object can be used as the request parameter for another method.
        Collection<ApiMethod> compatibleMethods = computeCompatibleMethods(subObject, service);
        JsObjectIterable objIter = new JsObjectIterable(service, subObject, depth + 1, linkFactory);
        return formatGroup(objIter, title, depth, "{", "}", hasSeparator, null);
    }
    return new FlowPanel();
  }

  private static Widget simpleInline(
      String title, String inlineText, String style, int depth, boolean hasSeparator) {
    Widget valueLabel = new InlineLabel(inlineText);
    valueLabel.addStyleName(style);
    return inlineWidget(title, Lists.newArrayList(valueLabel), depth, hasSeparator);
  }

  private static Widget inlineWidget(
      String title, List<Widget> inlineWidgets, int depth, boolean hasSeparator) {

    FlowPanel inlinePanel = new FlowPanel();

    StringBuilder keyText = new StringBuilder(indentation(depth)).append(title);
    InlineLabel keyLabel = new InlineLabel(keyText.toString());
    keyLabel.addStyleName(style.jsonKey());
    inlinePanel.add(keyLabel);

    for (Widget child : inlineWidgets) {
      inlinePanel.add(child);
    }

    if (hasSeparator) {
      inlinePanel.add(new InlineLabel(SEPARATOR_TEXT));
    }

    return inlinePanel;
  }

  private static String indentation(int depth) {
    return Strings.repeat(" ", depth);
  }

  private static List<Widget> formatString(
      ApiService service, String rawText, PrettifierLinkFactory linkFactory) {

    if (isLink(rawText)) {
      List<Widget> response = Lists.newArrayList();
      response.add(new InlineLabel("\""));

      boolean createdExplorerLink = false;
      try {
        ApiMethod method = getMethodForUrl(service, rawText);
        if (method != null) {
          String explorerLink = createExplorerLink(service, rawText, method);
          Widget linkObject = linkFactory.generateAnchor(rawText, explorerLink);
          linkObject.addStyleName(style.jsonStringExplorerLink());
          response.add(linkObject);
          createdExplorerLink = true;
        }
      } catch (IndexOutOfBoundsException e) {
        // Intentionally blank - this will only happen when iterating the method
        // url template in parallel with the url components and you run out of
        // components
      }

      if (!createdExplorerLink) {
        Anchor linkObject = new Anchor(rawText, rawText, OPEN_IN_NEW_WINDOW);
        linkObject.addStyleName(style.jsonStringLink());
        response.add(linkObject);
      }

      response.add(new InlineLabel("\""));
      return response;
    } else {
      JSONString encoded = new JSONString(rawText);
      Widget stringText = new InlineLabel(encoded.toString());
      stringText.addStyleName(style.jsonString());
      return Lists.newArrayList(stringText);
    }
  }

  private static String titleString(String name) {
    return "\"" + name + "\": ";
  }

  /**
   * Attempts to identify an {@link ApiMethod} corresponding to the given url.
   * If one is found, a {@link java.util.Map.Entry} will be returned where the key is the
   * name of the method, and the value is the {@link ApiMethod} itself. If no
   * method is found, this will return {@code null}.
   */
  @VisibleForTesting
  static ApiMethod getMethodForUrl(ApiService service, String url) {
    String apiLinkPrefix = Config.getBaseUrl() + service.basePath();
    if (!url.startsWith(apiLinkPrefix)) {
      return null;
    }

    // Only check GET methods since those are the only ones that can be returned
    // in the response.
    Iterable<ApiMethod> getMethods =
        Iterables.filter(service.allMethods().values(), new Predicate<ApiMethod>() {
          @Override
          public boolean apply(ApiMethod input) {
            return input.getHttpMethod() == HttpMethod.GET;
          }
        });

    int paramIndex = url.indexOf("?");
    String path = url.substring(0, paramIndex > 0 ? paramIndex : url.length());
    for (ApiMethod method : getMethods) {
      // Try to match the request URL with its method by comparing it to the
      // method's rest base path URI template. To do this we have to remove the
      // {...} placeholders.
      String regex =
          apiLinkPrefix + method.getPath().replaceAll("\\{[^\\/]+\\}", "[^\\/]+");
      if (path.matches(regex)) {
        return method;
      }
    }
    return null;
  }

  /**
   * Creates an Explorer link token (e.g.,
   * #s/<service>/<version>/<method>) corresponding to the given request
   * URL, given the method name and method definition returned by
   * {@link #getMethodForUrl(ApiService, String)}.
   */
  @VisibleForTesting
  static String createExplorerLink(ApiService service, String url, ApiMethod method) {
    UrlBuilder builder = new UrlBuilder();

    // Add the basic information to the
    builder.addRootNavigationItem(RootNavigationItem.ALL_VERSIONS)
        .addService(service.getName(), service.getVersion())
        .addMethodName(method.getId());

    // Calculate the params from the path template and url.
    URLFragment parsed = URLFragment.parseFragment(url);
    Multimap<String, String> params = HashMultimap.create();
    String pathTemplate = method.getPath();
    if (pathTemplate.contains("{")) {
      String urlPath = parsed.getPath().replaceFirst(Config.getBaseUrl() + service.basePath(), "");
      String[] templateSections = pathTemplate.split("/");
      String[] urlSections = urlPath.split("/");
      for (int i = 0; i < templateSections.length; i++) {
        if (templateSections[i].contains("{")) {
          String paramName = templateSections[i].substring(1, templateSections[i].length() - 1);
          params.put(paramName, urlSections[i]);
        }
      }
    }

    // Apply the params.
    String fullUrl = builder.addQueryParams(params).toString();

    // Check if the url had query parameters to add.
    if (!parsed.getQueryString().isEmpty()) {
      fullUrl = fullUrl + parsed.getQueryString();
    }

    return fullUrl;
  }

  private static boolean isLink(String value) {
    return (value.startsWith("http://") || value.startsWith("https://")) && !value.contains("\n")
        && !value.contains("\t");
  }

  /**
   * Create a drop down menu that allows the user to navigate to compatible methods for the
   * specified resource.
   *
   * @param methods Methods for which to build the menu.
   * @param service Service to which the methods correspond.
   * @param objectToPackage Object which should be passed to the destination menus.
   * @param linkFactory Factory that will be used to create links.
   * @return A button that will show the menu that was generated or {@code null} if there are no
   *         compatible methods.
   */
  private static PushButton createRequestMenu(final Collection<ApiMethod> methods,
      final ApiService service, DynamicJso objectToPackage, PrettifierLinkFactory linkFactory) {

    // Determine if a menu even needs to be generated.
    if (methods.isEmpty()) {
      return null;
    }

    // Create the parameters that will be passed to the destination menu.
    String resourceContents = new JSONObject(objectToPackage).toString();
    final Multimap<String, String> resourceParams =
        ImmutableMultimap.of(UrlBuilder.BODY_QUERY_PARAM_KEY, resourceContents);

    // Create the menu itself.
    FlowPanel menuContents = new FlowPanel();

    // Add a description of what the menu does.
    Label header = new Label("Use this resource in one of the following methods:");
    header.addStyleName(style.dropDownMenuItem());
    menuContents.add(header);

    // Add a menu item for each method.
    for (ApiMethod method : methods) {
      PushButton methodItem = new PushButton();
      methodItem.addStyleName(style.dropDownMenuItem());
      methodItem.addStyleName(style.selectableDropDownMenuItem());
      methodItem.setText(method.getId());
      menuContents.add(methodItem);

      // When clicked, Navigate to the menu item.
      UrlBuilder builder = new UrlBuilder();
      String newUrl = builder
          .addRootNavigationItem(RootNavigationItem.ALL_VERSIONS)
          .addService(service.getName(), service.getVersion())
          .addMethodName(method.getId())
          .addQueryParams(resourceParams)
          .toString();
      methodItem.addClickHandler(linkFactory.generateMenuHandler(newUrl));
    }

    // Create the panel which will be disclosed.
    final PopupPanel popupMenu = new PopupPanel(/* auto hide */ true);
    popupMenu.setStyleName(style.dropDownMenuPopup());

    FocusPanel focusContents = new FocusPanel();
    focusContents.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        popupMenu.hide();
      }
    });
    focusContents.setWidget(menuContents);

    popupMenu.setWidget(focusContents);

    // Create the button which will disclose the menu.
    final PushButton menuButton = new PushButton(new Image(resources.downArrow()));
    menuButton.addStyleName(style.reusableResourceButton());

    menuButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        popupMenu.setPopupPositionAndShow(new PositionCallback() {
          @Override
          public void setPosition(int offsetWidth, int offsetHeight) {
            popupMenu.setPopupPosition(
                menuButton.getAbsoluteLeft() + menuButton.getOffsetWidth() - offsetWidth,
                menuButton.getAbsoluteTop() + menuButton.getOffsetHeight());
          }
        });
      }
    });

    // Return only the button to the caller.
    return menuButton;
  }
}
