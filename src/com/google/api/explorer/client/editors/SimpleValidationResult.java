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

import com.google.api.explorer.client.editors.Validator.ValidationResult;
import com.google.common.base.Preconditions;

/**
 * This class creates a simplified interface for creating valid instances of validation result.
 *
 */
public class SimpleValidationResult implements ValidationResult{
  private final Type resultType;
  private final String message;

  private SimpleValidationResult(Type resultType, String message) {
    this.resultType = resultType;
    this.message = message;
  }

  @Override
  public String getMessage() {
    if (resultType != Type.VALID && message == null) {
      throw new IllegalStateException(
          "The object should never have an invalid status and null message");
    }
    return message;
  }

  @Override
  public Type getType() {
    return resultType;
  }

  /**
   * The shared valid result.
   */
  public static final ValidationResult STATUS_VALID = new SimpleValidationResult(Type.VALID, null);

  /**
   * Create a new validation result that provides information, but does not
   * indicate an error condition.
   */
  public static final ValidationResult createInfo(final String message) {
    return new SimpleValidationResult(Type.INFO, Preconditions.checkNotNull(message));
  }

  /**
   * Create a new validation result that indicates an error condition was found,
   * and a message describing the error condition.
   */
  public static final ValidationResult createError(final String message) {
    return new SimpleValidationResult(Type.ERROR, Preconditions.checkNotNull(message));
  }
}
