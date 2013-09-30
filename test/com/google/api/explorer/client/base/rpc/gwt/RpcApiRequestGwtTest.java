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

package com.google.api.explorer.client.base.rpc.gwt;

import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.BaseGwtTest;
import com.google.api.explorer.client.base.mock.MockApiMethod;
import com.google.api.explorer.client.base.rpc.RpcApiRequest;
import com.google.api.explorer.client.base.rpc.RpcApiService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * Tests that are specific to the RPC variant of {@link ApiRequest}
 *
 */
public class RpcApiRequestGwtTest extends BaseGwtTest {

  private ApiMethod mockMethod = new MockApiMethod();

  public void testRequestBody() {
    JSONObject root = new JSONObject();
    root.put("method", new JSONString("api.method.name"));
    root.put("version", new JSONString("v1"));

    JSONObject params = new JSONObject();
    root.put("params", params);
    params.put("param1", new JSONString("value1"));

    ApiService service = RpcApiService.Helper.fromString("{}");
    RpcApiRequest request = new RpcApiRequest(service, mockMethod);

    request.setBody(root);

    ListMultimap<String, String> paramValues = request.getParamValues();
    assertEquals(Sets.newHashSet("param1"), paramValues.keySet());
    assertEquals(ImmutableList.of("\"value1\""), paramValues.get("param1"));

    assertEquals(root.toString(), request.getRequestBody());
  }
}
