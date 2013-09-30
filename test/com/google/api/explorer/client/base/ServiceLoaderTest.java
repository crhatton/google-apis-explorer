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
import com.google.api.explorer.client.base.rest.RestApiService;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.util.Set;

/**
 * Tests for {@link ServiceLoader}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceLoaderTest extends TestCase {

  private MockGoogleApi googleApi;
  private ServiceLoader loader;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    googleApi = new MockGoogleApi();
    loader = new ServiceLoader(googleApi);
  }

  /**
   * Test that the callback gets invoked properly when a service is requested.
   */
  public void testVersionSelected() {
    googleApi.service = EasyMock.createControl().createMock(RestApiService.class);

    @SuppressWarnings("unchecked")
    Callback<ApiService, String> mockCallback = EasyMock.createMock(Callback.class);
    mockCallback.onSuccess(googleApi.service);
    EasyMock.replay(mockCallback);

    loader.loadService("service", "version", mockCallback);

    EasyMock.verify(mockCallback);
    assertEquals(1, googleApi.invocations);
  }

  /**
   * Test that we don't call our dependency when we should have a cached version.
   */
  public void testVersionSelected_cached() {
    RestApiService service = EasyMock.createControl().createMock(RestApiService.class);

    // Populate the cache with the service we'll request later.
    loader.cache.put(ServiceLoader.generateCacheKey("service", "version", CallStyle.REST), service);

    @SuppressWarnings("unchecked")
    Callback<ApiService, String> mockCallback = EasyMock.createMock(Callback.class);
    mockCallback.onSuccess(service);
    EasyMock.expectLastCall();
    EasyMock.replay(mockCallback);

    loader.loadService("service", "version", mockCallback);

    EasyMock.verify(mockCallback);
    assertEquals(0, googleApi.invocations);
  }

  /**
   * Test that a blacklisted API doesn't show up in the directory list.
   */
  public void testDirectoryBlacklist() {
    ServiceDefinition toFilter = EasyMock.createMock(ServiceDefinition.class);
    EasyMock.expect(toFilter.getName()).andReturn("drive").atLeastOnce();
    EasyMock.expect(toFilter.getId()).andReturn("drive:v1").atLeastOnce();

    ServiceDefinition toLeave = EasyMock.createMock(ServiceDefinition.class);
    EasyMock.expect(toLeave.getName()).andReturn("drive").atLeastOnce();
    EasyMock.expect(toLeave.getId()).andReturn("drive:v2").atLeastOnce();

    final Set<ServiceDefinition> directory = ImmutableSet.of(toFilter, toLeave);
    ApiServiceFactory mockDirectory = EasyMock.createMock(ApiServiceFactory.class);

    final Capture<AsyncCallback<Set<ServiceDefinition>>> cbCapture =
        new Capture<AsyncCallback<Set<ServiceDefinition>>>();

    mockDirectory.loadApiDirectory(EasyMock.capture(cbCapture));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        cbCapture.getValue().onSuccess(directory);
        return null;
      }
    });

    EasyMock.replay(toFilter, toLeave, mockDirectory);

    loader = new ServiceLoader(mockDirectory);

    final Set<ServiceDefinition> filtered = Sets.newHashSet();
    loader.loadServiceDefinitions(new Callback<Set<ServiceDefinition>, String>() {
      @Override
      public void onFailure(String reason) {
        fail();
      }

      @Override
      public void onSuccess(Set<ServiceDefinition> result) {
        filtered.addAll(result);
      }
    });

    assertEquals(1, filtered.size());
    assertEquals("drive:v2", filtered.iterator().next().getId());

    EasyMock.verify(toFilter, toLeave, mockDirectory);
  }

  /**
   * Mock implementation of {@link ApiServiceFactory} which allows its returned service
   * to be set.
   */
  private static class MockGoogleApi extends ApiServiceFactory {
    private RestApiService service;

    int invocations = 0;

    public MockGoogleApi() {
      super();
    }

    /**
     * Mocks out real API requests by immediately calling the callback with the
     * pre-defined service.
     */
    @Override
    public void createService(final String serviceName, final String version,
        final CallStyle callStyle, final AsyncCallback<ApiService> callback) {
      invocations++;
      callback.onSuccess(service);
    }
  }
}
