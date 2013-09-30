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
package com.google.api.explorer.client.base;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Helper code which will generate a presentable name from a potentially null title and a service
 * name.
 *
 */
public class NameHelper {
  /**
   * Generate the title for the specified title and name.
   *
   * @param title Title to directly return or {@code null}.
   * @param name Name which should be turned into a title if the title is null.
   * @return Printable title.
   */
  public static String generateDisplayTitle(@Nullable String title, String name) {
    return Objects.firstNonNull(title, Preconditions.checkNotNull(name) + " API");
  }
}
