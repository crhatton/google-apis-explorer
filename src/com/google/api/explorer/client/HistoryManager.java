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

package com.google.api.explorer.client;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.RequestFinishedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;

import java.util.Map;
import java.util.Set;

/**
 * Manages app state encoded in the document location.
 *
 * <p>
 * The service, version and method are expected to be specified as such:
 * #_s={service name}&_v={version name}&_m={method identifier}&{param
 * key}={param value}...
 * </p>
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class HistoryManager
    implements
    ValueChangeHandler<String>,
    VersionSelectedEvent.Handler,
    MethodSelectedEvent.Handler,
    RequestFinishedEvent.Handler {

  private static final String FIELD_SEPARATOR = "&";
  private static final String VALUE_SEPARATOR = "=";
  public static final String SERVICE = "_s";
  public static final String VERSION = "_v";
  public static final String METHOD = "_m";

  // Parameter keys to ignore when constructing a token. These are either
  // sensitive (key) or unimportant (pp) parameter keys that should not be shown
  // in the URL
  private static final Set<String> IGNORED_KEYS = ImmutableSet.of("pp", "key");

  private final EventBus eventBus;
  private final AppState appState;
  private final HistoryWrapper historyWrapper;

  // When issuing events that end up changing the URL fragment, and listening to
  // URL fragment changes, we want to avoid infinite loops. We do this by
  // ignoring events while an event is being processed, using this boolean.
  private boolean ignoreEvents = false;

  public HistoryManager(EventBus eventBus, AppState appState, HistoryWrapper historyWrapper) {
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
    eventBus.addHandler(RequestFinishedEvent.TYPE, this);
    historyWrapper.addValueChangeHandler(this);
    this.eventBus = eventBus;
    this.appState = appState;
    this.historyWrapper = historyWrapper;
  }

  /**
   * When the URL fragment changes (as in the case when a link was clicked),
   * issue the correct VersionSelectedEvent with the service and version
   * specified, and any method or parameters included.
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    if (!ignoreEvents) {
      final Multimap<String, String> params = parseParams(event.getValue());
      final String serviceName = Iterables.getFirst(params.get(SERVICE), null);
      final String version = Iterables.getFirst(params.get(VERSION), "v1");
      final String methodName = Iterables.getFirst(params.get(METHOD), null);
      params.remove(SERVICE, serviceName);
      params.remove(VERSION, version);
      params.remove(METHOD, methodName);
      ignoreEvents = false;
      eventBus.fireEvent(new VersionSelectedEvent(serviceName, version, methodName, params));
    }
    ignoreEvents = false;
  }

  public static Multimap<String, String> parseParams(String token) {
    Multimap<String, String> params = HashMultimap.create();
    String[] fields = token.split(FIELD_SEPARATOR);
    for (String field : fields) {
      if (field.contains(VALUE_SEPARATOR)) {
        String[] keyVal = field.split(VALUE_SEPARATOR, 2);
        params.put(keyVal[0], keyVal[1]);
      } else {
        params.put(field, null);
      }
    }
    return params;
  }

  /**
   * When a version is selected, update the URL fragment to specify the version,
   * and any method or params included.
   */
  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    // TODO(jasonhall): Handle cases like this more gracefully...
    if (event.serviceName == null || event.versionName == null) {
      historyWrapper.newItem("");
      return;
    }
    StringBuilder tokenBuilder = new StringBuilder()
        .append(SERVICE)
        .append(VALUE_SEPARATOR)
        .append(event.serviceName)
        .append(FIELD_SEPARATOR)
        .append(VERSION)
        .append(VALUE_SEPARATOR)
        .append(event.versionName);
    if (event.methodName != null) {
      tokenBuilder
          .append(FIELD_SEPARATOR)
          .append(METHOD)
          .append(VALUE_SEPARATOR)
          .append(event.methodName);
    }

    appendParams(event.params, tokenBuilder);

    ignoreEvents = true;
    historyWrapper.newItem(tokenBuilder.toString());
    ignoreEvents = false;
  }

  /**
   * When a method is selected, update the URL fragment to specify the method,
   * and any params that might be included.
   */
  @Override
  public void onMethodSelected(MethodSelectedEvent event) {
    if (event.method != null) {
      StringBuilder tokenBuilder = new StringBuilder()
          .append(SERVICE)
          .append(VALUE_SEPARATOR)
          .append(appState.getCurrentService().getName())
          .append(FIELD_SEPARATOR)
          .append(VERSION)
          .append(VALUE_SEPARATOR)
          .append(appState.getCurrentService().getVersion())
          .append(FIELD_SEPARATOR)
          .append(METHOD)
          .append(VALUE_SEPARATOR)
          .append(appState.getCurrentMethodIdentifier());

      appendParams(event.params, tokenBuilder);

      ignoreEvents = true;
      historyWrapper.newItem(tokenBuilder.toString());
      ignoreEvents = false;
    }
  }

  /**
   * When a request finishes, update the URL fragment to specify the method
   * called and params used.
   */
  @Override
  public void onRequestFinished(RequestFinishedEvent event) {
    ApiRequest request = event.request;
    StringBuilder tokenBuilder = new StringBuilder()
        .append(SERVICE)
        .append(VALUE_SEPARATOR)
        .append(request.service.getName())
        .append(FIELD_SEPARATOR)
        .append(VERSION)
        .append(VALUE_SEPARATOR)
        .append(request.service.getVersion())
        .append(FIELD_SEPARATOR)
        .append(METHOD)
        .append(VALUE_SEPARATOR)
        .append(appState.getCurrentMethodIdentifier());

    appendParams(event.request.paramValues, tokenBuilder);

    ignoreEvents = true;
    historyWrapper.newItem(tokenBuilder.toString());
  }

  /** Appends all other parameters to the token string. */
  private void appendParams(Multimap<String, String> params, StringBuilder tokenBuilder) {
    for (Map.Entry<String, String> entry : params.entries()) {
      if (!IGNORED_KEYS.contains(entry.getKey())) {
        tokenBuilder
            .append(FIELD_SEPARATOR)
            .append(entry.getKey())
            .append(VALUE_SEPARATOR)
            .append(entry.getValue());
      }
    }
  }
}
