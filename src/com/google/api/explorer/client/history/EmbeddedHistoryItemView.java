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

package com.google.api.explorer.client.history;

import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiResponse.HeaderValue;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.ExplorerConfig;
import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.api.explorer.client.history.JsonPrettifier.JsonFormatException;
import com.google.api.explorer.client.history.JsonPrettifier.PrettifierLinkFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

public class EmbeddedHistoryItemView extends Composite {

  private static HistoryItemUiBinder uiBinder = GWT.create(HistoryItemUiBinder.class);

  private static final String IMAGE_TYPE_PREFIX = "image/";
  private static final String TEXT_TYPE_PREFIX = "text/";
  private static final String CONTENT_TYPE_HEADER = "content-type";
  private static final String AUTH_HEADER = "authorization";

  interface HistoryItemUiBinder extends UiBinder<Widget, EmbeddedHistoryItemView> {
  }

  interface EmbeddedHistoryItemViewStyle extends CssResource {
    String fadeIn();
  }

  @UiField public Panel titleBar;
  @UiField public SpanElement title;
  @UiField public SpanElement time;
  @UiField public SimplePanel errorPanel;
  @UiField public PreElement requestDiv;
  @UiField public FlowPanel requestBodyDiv;
  @UiField public PreElement statusDiv;
  @UiField public Label showHideHeaders;
  @UiField public PreElement responseHeadersDiv;
  @UiField public FlowPanel responseBodyDiv;
  @UiField public Panel executing;
  @UiField public HTMLPanel wireContent;

  @UiField EmbeddedHistoryItemViewStyle style;

  private final ApiRequest request;
  private final String realPathFragment;

  public EmbeddedHistoryItemView(ApiRequest request) {
    initWidget();

    this.request = request;

    // Stash the real URL in case we need it for loading media
    realPathFragment = request.getRequestPath();

    // Replace the API key with a fake version if the default was used
    if (ExplorerConfig.API_KEY.equals(request.getApiKey())) {
      request.setApiKey("{YOUR_API_KEY}");
    }

    String prefix = request.getMethod().getId() + " executed ";
    PrettyDate.keepMakingPretty(new Date(), prefix, title);

    String dateString =
        DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT).format(new Date());
    title.setTitle(dateString);

    requestDiv.setInnerText(getRequestString(request));
  }

  /**
   * Complete the partially filled history item with the response data.
   *
   * @param response Response data.
   * @param timeMillis Time that execution took in milliseconds.
   * @param linkFactory Link factory that is used to generate hyperlink and menu links in the
   *        response view.
   */
  public void complete(ApiResponse response, long timeMillis, PrettifierLinkFactory linkFactory) {
    executing.setVisible(false);
    wireContent.addStyleName(style.fadeIn());
    time.setInnerText("time to execute: " + timeMillis + " ms");
    statusDiv.setInnerText(response.getStatus() + " " + response.getStatusText());

    // Headers are hidden by default.
    UIObject.setVisible(responseHeadersDiv, false);
    responseHeadersDiv.setInnerText(getResponseHeadersString(response));
    try {
      JsonPrettifier.prettify(
          request.getService(), requestBodyDiv, request.getRequestBody(), linkFactory);
    } catch (JsonFormatException e) {
      // We should only be generating valid requests
      requestBodyDiv.add(new InlineLabel(request.getRequestBody()));
    }

    setResponseContent(request, response, realPathFragment, linkFactory);
  }

  /**
   * Set the value of the panel reserved for the formatted response. There are a couple different
   * scenarios to tackle. If we can determine that the request returned an image, and the request is
   * repeatable, we will create an image tag with a source of the original request.
   *
   * If the response is a non-JSON text type, we just show it directly.
   *
   * In all other cases we try to process the text as JSON and if for some reason that fails, we
   * just hide it under an opaque tag that says as much information as we know about the response.
   *
   * @param request Request object with the API key replaced.
   * @param response Response from the server.
   * @param originalPath Path object before we replaced the API key.
   * @param linkFactory Which links factory should be used when generating links and navigation
   *        menus.
   */
  private void setResponseContent(ApiRequest request, ApiResponse response, String originalPath,
      PrettifierLinkFactory linkFactory) {

    HeaderValue authorization = response.getHeaders().get(AUTH_HEADER);
    HeaderValue contentTypeHeader = response.getHeaders().get(CONTENT_TYPE_HEADER);
    GWT.log("Headers: " + response.getHeaders().entrySet());
    String contentType = contentTypeHeader == null ? "Unspecified" : contentTypeHeader.getValue();

    if (request.getHttpMethod() == HttpMethod.GET && contentType.startsWith(IMAGE_TYPE_PREFIX)
        && authorization == null) {

      // In the very special case that we performed a get and were given an
      // image, display it
      Image img = new Image();
      img.setUrl(Config.getBaseUrl() + originalPath);
      img.setAltText(Config.getBaseUrl() + request.getRequestPath());
      responseBodyDiv.add(img);
    } else if (contentType.startsWith(TEXT_TYPE_PREFIX)) {
      // We have non-JSON text, just show it.
      responseBodyDiv.add(new Label(response.getBodyAsString()));
    } else {
      // Treat the response as JSON, although we don't really know what it is
      try {
        JsonPrettifier.prettify(
            request.getService(), responseBodyDiv, response.getBodyAsString(), linkFactory);
      } catch (JsonFormatException e) {
        // If JSON processing fails, just say what we know about the data
        responseBodyDiv.add(new Label("[" + contentType + " data]"));
      }

      // Check if there was an error, and, if so, display it to the user.
      ErrorCase error = getErrorMessage(response);
      if (error != null) {
        setErrorMessage(error.getErrorLabel());
      }
    }
  }

  protected void initWidget() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  @UiHandler("showHideHeaders")
  public void showHide(ClickEvent event) {
    showHideHeaders.setText(
        UIObject.isVisible(responseHeadersDiv) ? "- Show headers -" : "- Hide headers -");
    UIObject.setVisible(responseHeadersDiv, !UIObject.isVisible(responseHeadersDiv));
  }

  private static String getRequestString(ApiRequest request) {
    StringBuilder sb = new StringBuilder()
        .append(request.getHttpMethod().name())
        .append(' ')
        .append(Config.getBaseUrl())
        // If the standard API key is being used, mask it in the UI.
        // The URL is already URL-escaped before making the request, so we don't
        // want to double-escape it.
        .append(request.getRequestPath());

    // Display headers that were set on the request.
    // TODO(jasonhall): This can be prettier.
    sb.append('\n');
    for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
      sb.append('\n').append(entry.getKey()).append(":  ").append(entry.getValue());
    }

    return sb.toString();
  }

  private static ErrorCase getErrorMessage(ApiResponse response) {
    // This requires a try-catch because there is no way to proactively check
    // that the JSON is both present and valid without just trying to parse it.
    try {
      DynamicJso jso = JsonUtils.safeEval(response.getBodyAsString());
      if (jso.get("error") != null) {
        return ErrorCase.forJsonString(response.getBodyAsString());
      }
    } catch (IllegalArgumentException e) {
      // Not valid json, definitely not an error payload.
    }
    return null;
  }

  private static String getResponseHeadersString(ApiResponse response) {
    StringBuilder sb = new StringBuilder();

    SortedMap<String, HeaderValue> sorted = Maps.newTreeMap(Ordering.natural());
    sorted.putAll(response.getHeaders());

    for (HeaderValue header : sorted.values()) {
      sb.append(header.getKey()).append(":  ").append(header.getValue()).append('\n');
    }

    return sb.toString();
  }

  private void setErrorMessage(Widget prettyMessage) {
    errorPanel.setVisible(true);
    errorPanel.add(prettyMessage);
  }
}
