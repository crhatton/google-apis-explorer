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

package com.google.api.explorer.client.base;

import com.google.api.explorer.client.base.ApiDirectory.Icons;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition.Label;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Represents an API service containing methods that can be called. This
 * interface will be extended by specializations for different variants.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface ApiService {

  /**
   * Calling style of the ApiService instance.
   */
  public enum CallStyle {
    REST("rest"),
    RPC("rpc");

    /** Path fragment used to request the discovery document for this call style */
    public final String discoveryPathFragment;

    private CallStyle(String discoveryPathFragment) {
      this.discoveryPathFragment = discoveryPathFragment;
    }
  }

  /** Name of this service. */
  String getName();

  /** Version of this service. */
  String getVersion();

  /** Name and version concatentated into a unique identifier. */
  String getId();

  /** Human readable title for the service. */
  String getTitle();

  /** Short description of this service. */
  String getDescription();

  /** URL to find the icon for this service. */
  Icons getIcons();

  /** URL to find documentation for this service. */
  String getDocumentationLink();

  /** Labels for the service. See {@link Label}. */
  Set<Label> getLabels();

  /**
   * Returns a {@link Map} of {@link AuthInformation} keyed by its auth type.
   * Currently, the only key will be "oauth2", describing OAuth 2.0
   * authentication information.
   */
  Map<String, AuthInformation> getAuth();

  /** {@link Map} of global parameters for all methods in this service. */
  Map<String, Schema> getParameters();

  /** Represents information about authentication options for a service. */
  static interface AuthInformation {
    /**
     * Map of auth scope information, where the key is the auth scope URL and
     * the value is a {@link AuthScope} further describing that scope.
     */
    Map<String, AuthScope> getScopes();
  }

  /**
   * Returns the request schema used by the given method, or {@code null} if
   * none is required.
   */
  Schema requestSchema(ApiMethod method);

  /**
   * Returns a {@link Map} of all the {@link ApiMethod}s available in this
   * service, keyed by the method's unique identifier.
   */
  Map<String, ApiMethod> allMethods();

  /**
   * Returns the response schema used by the given method, or {@code null} if
   * none is required.
   */
  Schema responseSchema(ApiMethod method);

  /**
   * Returns the base path, across REST and RPC calling styles
   */
  String basePath();

  /**
   * Return whether this API uses the REST or RPC call style
   */
  CallStyle callStyle();

  /**
   * Returns the {@link ApiMethod} identified by the given method identifier,
   * belonging to the given {@link ApiService}, or {@code null} if no such
   * method exists.
   */
  ApiMethod method(String methodIdentifier);

  /**
   * Returns the {@link ApiMethod} identified by the old-style method which is constructed by
   * joining resource and method names and omitting the service names.
   */
  ApiMethod resolveMethod(String oldMethodIdentifier);

  /**
   * Returns the methods from this service that use the specified kind as the request schema.
   */
  Collection<ApiMethod> usagesOfKind(String kind);

  /** Represents information about authentication scopes for a service. */
  static interface AuthScope {
    /** Returns the description of this authentication scope. */
    String getDescription();
  }

  /** Returns a mapping of all schemas used by this service. */
  Map<String, Schema> getSchemas();

  /**
   * Returns a title that can be used for display purposes by using the one the API designer set, or
   * by prettifying the name of the API name.
   */
  String displayTitle();
}