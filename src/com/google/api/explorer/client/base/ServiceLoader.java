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

import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiService.CallStyle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to encapsulate logic of loading services.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceLoader {
  /**
   * Delegate format for an observer of service and directory loaded events.
   */
  public interface ServiceLoaderDelegate {
    /**
     * Invoked when a service has been loaded.
     *
     * @param service Service definition for the service which has been loaded.
     */
    public void serviceLoaded(ApiService service);

    /**
     * Invoked when a directory document has been loaded and parsed.
     *
     * @param directoryServices Parsed set of services from the directory document.
     */
    public void directoryLoaded(Set<ServiceDefinition> directoryServices);
  }

  private final ApiServiceFactory googleApi;

  /**
   * Delegate property which can be set to be notified of events. Default value discards
   * notifications.
   */
  public ServiceLoaderDelegate delegate = new ServiceLoaderDelegate() {
    @Override
    public void serviceLoaded(ApiService service) {
      // Intentionally blank, null implementation.
    }

    @Override
    public void directoryLoaded(Set<ServiceDefinition> directoryServices) {
      // Intentionally blank, null implementation.
    }
  };

  /**
   * List of services that should not be returned by directory because they would provide a bad
   * experience in APIs explorer.
   */
  private static final Set<String> SERVICE_NAME_BLACKLIST = Collections.emptySet();

  /**
   * List of specific versions of services that should not be returned by directory because they
   * would provide a bad experience.
   */
  private static final Set<String> SERVICE_ID_BLACKLIST = ImmutableSet.of("drive:v1");

  @VisibleForTesting
  final Map<String, ApiService> cache = Maps.newHashMap();

  final Multimap<String, Callback<ApiService, String>> outstandingRequestCallbacks =
      HashMultimap.create();

  private Set<ServiceDefinition> directoryCache;

  /**
   * Create an instance.
   *
   * @param googleApi Factory from which to obtain services on the wire.
   */
  public ServiceLoader(ApiServiceFactory googleApi) {
    this.googleApi = googleApi;
  }

  /**
   * Load the specified service from cache or request it from the discovery service.
   *
   * @param name Name of the service.
   * @param version Version of the service.
   * @param callback Callback to invoke when loading is complete.
   */
  public void loadService(String name, String version, Callback<ApiService, String> callback) {
    final String cacheKey = generateCacheKey(name, version, CallStyle.REST);

    // Handle the request immediately if possible.
    if (cache.containsKey(cacheKey)) {
      callback.onSuccess(cache.get(cacheKey));
      return;
    }

    outstandingRequestCallbacks.put(cacheKey, callback);

    // Only send the request if our request is the only one waiting on the resource.
    if (outstandingRequestCallbacks.get(cacheKey).size() == 1) {
      googleApi.createService(name, version, CallStyle.REST,
          new AsyncCallback<ApiService>() {
            @Override
            public void onSuccess(ApiService service) {
              cache.put(cacheKey, service);

              for (Callback<ApiService, String> cb : copyAndClearOutstandingCallbacks(cacheKey)) {
                cb.onSuccess(service);
              }

              delegate.serviceLoaded(service);
            }

            @Override
            public void onFailure(Throwable caught) {
              String failureMessage = caught.getMessage();
              for (Callback<ApiService, String> cb : copyAndClearOutstandingCallbacks(cacheKey)) {
                cb.onFailure(failureMessage);
              }
            }
          });
    }
  }

  /**
   * Copy the callbacks associated with the specified cache key and remove them from the list of
   * outstanding callbacks.
   */
  private Collection<Callback<ApiService, String>> copyAndClearOutstandingCallbacks(
      String cacheKey) {

    Collection<Callback<ApiService, String>> callbacks =
        ImmutableList.copyOf(outstandingRequestCallbacks.get(cacheKey));
    outstandingRequestCallbacks.removeAll(cacheKey);
    return callbacks;
  }

  /**
   * Alternate interface for callers to use when they don't care about when the service has been
   * loaded (e.g. search).
   */
  public void backgroundLoadService(String serviceId) {
    String[] components = serviceId.split(":");
    Preconditions.checkArgument(components.length == 2);

    String serviceName = components[0];
    String version = components[1];

    loadService(serviceName, version, new Callback<ApiService, String>() {
      @Override
      public void onFailure(String reason) {
        // Intentionally blank.
      }

      @Override
      public void onSuccess(ApiService result) {
        // Intentionally blank.
      }
    });
  }

  /**
   * Load the directory document from either cache or the wire and notify the specified callback
   * when done.
   */
  public void loadServiceDefinitions(final Callback<Set<ServiceDefinition>, String> callback) {
    if (directoryCache == null) {
      googleApi.loadApiDirectory(new AsyncCallback<Set<ServiceDefinition>>() {
        @Override
        public void onSuccess(Set<ServiceDefinition> unfiltered) {
          // Filter the list of services according to the blacklist.
          directoryCache = Sets.filter(unfiltered, new Predicate<ServiceDefinition>() {
            @Override
            public boolean apply(ServiceDefinition service) {
              return !SERVICE_NAME_BLACKLIST.contains(service.getName())
                  && !SERVICE_ID_BLACKLIST.contains(service.getId());
            }
          });

          callback.onSuccess(directoryCache);
          delegate.directoryLoaded(directoryCache);
        }

        @Override
        public void onFailure(Throwable caught) {
          callback.onFailure(caught.getMessage());
        }

      });
    } else {
      callback.onSuccess(directoryCache);
    }
  }

  /**
   * Load the directory document in the background.
   */
  public void backgroundLoadServiceDefinitions() {
    loadServiceDefinitions(new Callback<Set<ServiceDefinition>, String>() {
      @Override
      public void onSuccess(Set<ServiceDefinition> directoryServices) {
        // Intentionally blank.
      }

      @Override
      public void onFailure(String reason) {
        // Intentionally blank.
      }
    });
  }

  /**
   * Create a cache key that encodes the service name, version name, and call
   * style. Example: urlshortener_v1_REST
   */
  @VisibleForTesting
  static String generateCacheKey(
      String serviceName, String versionName, CallStyle callStyle) {

    if (serviceName == null || versionName == null || callStyle == null) {
      return null;
    }
    List<String> portions = ImmutableList.of(serviceName, versionName, callStyle.name());
    return Joiner.on("_").join(portions);
  }
}