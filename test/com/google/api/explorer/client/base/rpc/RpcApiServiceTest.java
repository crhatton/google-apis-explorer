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

package com.google.api.explorer.client.base.rpc;

import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.base.Schema.Type;
import com.google.api.explorer.client.base.rest.RestApiService;
import com.google.api.explorer.client.base.rest.RestApiService.Helper.Factory;
import com.google.api.explorer.client.base.rpc.RpcApiService.ApiServiceWrapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Tests for {@link RestApiService}, namely creation of the
 * service/resource/method tree from JSON as if retrieved from Discovery.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class RpcApiServiceTest extends TestCase {

  protected final RpcApiService service;

  public RpcApiServiceTest() {
    this.service = decodeTestJson();
  }

  /**
   * Reads discovery-small.json and parses it into a {@link RestApiService}
   * that can be tested.
   */
  protected RpcApiService decodeTestJson() {
    InputStream stream = getClass().getResourceAsStream("discovery-small.json");
    if (stream == null) {
      fail("discovery-small.json missing");
    }
    try {
      Reader reader = new InputStreamReader(stream, Charsets.UTF_8);
      String jsonString = CharStreams.toString(reader);
      stream.close();

      Factory factory = AutoBeanFactorySource.create(Factory.class);
      RpcApiService service =
          AutoBeanCodex.decode(factory, RpcApiService.class, jsonString).as();
      return service;
    } catch (IOException e) {
      fail("IOException: " + e.getMessage());
    }
    return null;
  }

  /** Tests that basic service information is parsed as expected. */
  public void testService() {
    assertEquals("moderator", service.getName());
    assertEquals("v1", service.getVersion());
    assertEquals("Moderator API", service.getDescription());
    assertEquals("/rpc", service.getRpcPath());
    assertEquals(3, service.getMethods().keySet().size());

    @SuppressWarnings("unchecked")
    AutoBean<RpcApiService> serviceBean = EasyMock.createMock(AutoBean.class);
    EasyMock.expect(serviceBean.as()).andReturn(service).anyTimes();
    EasyMock.replay(serviceBean);

    assertEquals("/rpc", ApiServiceWrapper.basePath(serviceBean));
  }

  /** Tests that a top-level method is parsed as expected. */
  public void testGetMethod() {
    RpcApiMethod get = service.getMethods().get("moderator.get");
    assertEquals(ImmutableList.of("https://www.googleapis.com/auth/scopename"), get.getScopes());
    assertEquals(ImmutableList.<String>of(), get.getParameterOrder());

    Schema param = get.getParameters().get("param");
    assertFalse(param.isRequired());
    assertNull(param.getPattern());

    @SuppressWarnings("unchecked")
    AutoBean<RpcApiService> serviceBean = EasyMock.createMock(AutoBean.class);
    EasyMock.expect(serviceBean.as()).andReturn(service).anyTimes();
    EasyMock.replay(serviceBean);

    Schema returnsSchema = ApiServiceWrapper.responseSchema(serviceBean, get);
    assertEquals(Type.OBJECT, returnsSchema.getType());

    Schema status = returnsSchema.getProperties().get("status");
    assertEquals(Type.STRING, status.getType());
  }
}
