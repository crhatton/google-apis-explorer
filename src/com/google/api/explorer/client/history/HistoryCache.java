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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

/**
 * Cache which will keep track of all of the history items generated in this run of the APIs
 * explorer.
 *
 */
public class HistoryCache {
  /**
   * Interface for an observer of history cache events.
   */
  public interface HistoryCacheObserver {
    /**
     * Method which is invoked each time a new history item becomes available.
     *
     * @param newItem New history item which has been cached.
     */
    public void newHistoryItem(HistoryItem newItem);
  }

  /** Store the history items in a sorted map so that they remain in chronological order. */
  private final SortedMap<String, HistoryItem> historyCache = Maps.newTreeMap(
      Collections.reverseOrder());

  private int lastKey = 0;

  /**
   * Field which contains the observer to notify of cache change events.
   */
  public HistoryCacheObserver observer = new HistoryCacheObserver() {
    @Override
    public void newHistoryItem(HistoryItem newItem) {
      // Intentionally blank
    }
  };

  /**
   * Create a new history item and add it to the cache.
   *
   * @param request Request object used to execute the request.
   * @param response Response object generated from the server response.
   * @param startTime Time at which the request started.
   * @param endTime Time at which the request completed.
   *
   * @return Key by which this new item is accessible.
   */
  public String addHistoryItem(
      ApiRequest request, ApiResponse response, long startTime, long endTime) {

    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(response);

    String key = Integer.toString(++lastKey);
    HistoryItem item = new HistoryItem(key, request, response, startTime, endTime);
    historyCache.put(key, item);

    // Inform our observer that there is a new item
    observer.newHistoryItem(item);
    return key;
  }

  /**
   * Returns a history item retrieved using the key provided.
   *
   * @param key Key which will be used to retrieve the cache item.
   *
   * @return History item stored associated with the key or {@code null} if none.
   */
  public HistoryItem getHistoryItem(String key) {
    return historyCache.get(Preconditions.checkNotNull(key));
  }

  /**
   * Returns the list of all cached items in reverse chronological order.
   */
  public List<HistoryItem> listHistoryItems() {
    return ImmutableList.copyOf(historyCache.values());
  }
}
