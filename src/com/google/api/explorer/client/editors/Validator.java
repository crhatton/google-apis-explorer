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

package com.google.api.explorer.client.editors;

import java.util.List;

/**
 * Represents a validator that will check if the value(s) of a parameter are
 * valid based on some criteria.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface Validator {

  /**
   * Returns true if all the values for this {@link Editor} are valid based on
   * this validator's requirements.
   */
  boolean isValid(List<String> value);
}
