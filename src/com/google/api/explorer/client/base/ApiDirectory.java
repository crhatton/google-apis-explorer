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

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

import java.util.Set;

/**
 * Represents the result of a Directory API request, containing the definition
 * of Google APIs that are supported by the Discovery API.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface ApiDirectory {

  /** {@link Set} of {@link ServiceDefinition}s available. */
  Set<ServiceDefinition> getItems();

  /** Defines one version of a Google API service. */
  public interface ServiceDefinition {
    /** Possible statuses for a Google API service. */
    enum Label {
      /**
       * Indicates that the service is "in labs", meaning that it may change in
       * breaking ways, or be removed entirely. See
       * {@link "http://code.google.com/labs/"}.
       */
      @PropertyName("labs")
      LABS,

      /**
       * Indicates that the service is stable, meaning that it can be depended
       * upon not to change in breaking ways or be removed without notice.
       */
      @PropertyName("stable")
      STABLE,

      /** Indicates that the service is deprecated, and should not be used. */
      @PropertyName("deprecated")
      DEPRECATED,

      /**
       * Indicates that the service is limited availability and may not be available for everyone.
       */
      @PropertyName("limited_availability")
      LIMITED_AVAILABILITY,
    }

    /** Formatted title for the service. */
    String getTitle();

    /** Name of the service. */
    String getName();

    /** Name and version concatenated into a unique id. */
    String getId();

    /** Version of the service. */
    String getVersion();

    /** Description of the service. */
    String getDescription();

    /** URL to call to get a full REST-style discovery document about this service. */
    String getDiscoveryLink();

    /** URL to find the icon for this service. */
    Icons getIcons();

    /** URL to find documentation for this service. */
    String getDocumentationLink();

    /** Labels for the service. See {@link Label}. */
    Set<Label> getLabels();

    /** Whether or not this version is the preferred version for this service. */
    boolean isPreferred();
  }

  /** Encapsulates different icon sizes available for the service. */
  interface Icons {
    @PropertyName("x16")
    String getIcon16Url();

    @PropertyName("x32")
    String getIcon32Url();
  }

  /**
   * Useful helper class to facilitate instantiation of a
   * {@link ApiDirectory} from a JSON string.
   */
  class Helper {
    /**
     * Returns a {@link ApiDirectory} based on the JSON representation of
     * that directory.
     */
    public static ApiDirectory fromString(String jsonString) {
      Factory factory = GWT.create(Factory.class);
      return AutoBeanCodex.decode(factory, ApiDirectory.class, jsonString).as();
    }
  }

  /** {@link AutoBeanFactory} class for {@link ApiDirectory}s. */
  interface Factory extends AutoBeanFactory {
    AutoBean<ApiDirectory> directory();
  }
}
