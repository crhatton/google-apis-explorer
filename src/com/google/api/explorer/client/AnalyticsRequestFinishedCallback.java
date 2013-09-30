/*
 * Copyright (C) 2012 Google Inc.
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

import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.analytics.AnalyticsManager.AnalyticsEvent;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.RequestFinishedCallback;

/**
 * Base callback that provides common analytics utilities to callbacks.
 *
 */
public class AnalyticsRequestFinishedCallback implements RequestFinishedCallback {

  private final AnalyticsManager analytics;

  protected AnalyticsRequestFinishedCallback(AnalyticsManager analytics) {
    this.analytics = analytics;
  }

  @Override
  public void starting(ApiRequest request) {
    analytics.trackEventWithValue(AnalyticsEvent.EXECUTE_METHOD, request.getMethod().getId());
  }

  @Override
  public void finished(ApiRequest request, ApiResponse response, long startTime, long endTime) {
    analytics.trackEventWithValue(classifyResponse(response), request.getMethod().getId());
  }

  /**
   * Classify what type of success or failure the response was.
   */
  private AnalyticsEvent classifyResponse(ApiResponse response) {
    int statusCode = response.getStatus();
    int statusCodeClass = statusCode / 100;

    AnalyticsEvent responseType;

    if (statusCode == 401) {
      responseType = AnalyticsEvent.RESPONSE_FAILED_AUTH;
    } else if (statusCodeClass == 4) {
      responseType = AnalyticsEvent.RESPONSE_FAILED_CLIENT;
    } else if (statusCodeClass == 5) {
      responseType = AnalyticsEvent.RESPONSE_FAILED_SERVER;
    } else {
      responseType = AnalyticsEvent.RESPONSE_SUCCESSFUL;
    }

    return responseType;
  }
}
