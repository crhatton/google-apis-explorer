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

package com.google.api.explorer.client.history;

import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;

/**
 * A plain old data class which contains a historical record of a request.
 *
 */
@Immutable
public final class HistoryItem implements Comparable<HistoryItem> {
  private final String key;
  private final ApiRequest request;
  private final ApiResponse response;
  private final long startTime;
  private final long endTime;

  /**
   * Create a new history item from the specified data.
   *
   * @param key Key which was used to store this history item and which can be used to reference it.
   * @param request Original request object that was executed.
   * @param response Response object that was returned when the original request was executed.
   * @param startTime Time at which the request was started.
   * @param endTime Time at which the request completed.
   */
  public HistoryItem(String key,
      ApiRequest request,
      ApiResponse response,
      long startTime,
      long endTime) {

    this.key = Preconditions.checkNotNull(key);
    this.request = Preconditions.checkNotNull(request);
    this.response = Preconditions.checkNotNull(response);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * Returns the key which was used to store this history item and which can be used to reference
   * it.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the original request object that was executed.
   */
  public ApiRequest getRequest() {
    return request;
  }

  /**
   * Returns the response object that was returned when the original request was executed.
   */
  public ApiResponse getResponse() {
    return response;
  }

  /**
   * Returns the time at which the request was started.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Returns the time at which the request completed.
   */
  public long getEndTime() {
    return endTime;
  }

  @Override
  public int compareTo(HistoryItem o) {
    return new Long(endTime).compareTo(o.endTime);
  }
}
