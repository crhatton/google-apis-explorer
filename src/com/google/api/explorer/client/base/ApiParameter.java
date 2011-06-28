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

import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.util.List;

/**
 * Represents a method parameter specification.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface ApiParameter {
  /**
   * Regular expression pattern that any value of this parameter must match, or
   * {@code null} if no pattern is defined.
   */
  String getPattern();

  /** Whether or not this parameter is required. */
  boolean isRequired();

  /** Whether or not this parameter can be specified multiple times. */
  boolean isRepeated();

  /**
   * A short description of what this parameter represents, or {@code null} if
   * no description is defined.
   */
  String getDescription();

  /** Valid types for parameter values. */
  enum Type {
    /** The parameter value can be any string. */
    @PropertyName("string")
    STRING,

    /** The parameter value must be an integer number. */
    @PropertyName("integer")
    INTEGER,

    /** The parameter value must be a decimal number or integer. */
    @PropertyName("decimal")
    DECIMAL,

    /** The parameter value must be a boolean. */
    @PropertyName("boolean")
    BOOLEAN;
  }

  /** The type that this parameter's value is expected to be. */
  Type getType();

  /**
   * The minimum valid value for this parameter if the {@link Type} of the
   * parameter's value is {@link Type#INTEGER} or {@link Type#DECIMAL}, or
   * {@code null} if no minimum value is defined.
   */
  String getMinimum();

  /**
   * The maximum valid value for this parameter if the {@link Type} of the
   * parameter's value is {@link Type#INTEGER} or {@link Type#DECIMAL}, or
   * {@code null} if no maximum value is defined.
   */
  String getMaximum();

  /**
   * The {@link List} of valid String values for this parameter, or {@code null}
   * if no specific values are defined.
   */
  @PropertyName("enum")
  List<String> getEnumValues();

  /**
   * Descriptions corresponding to the valid values specified in
   * {@link #getEnumValues()}, or {@code null} if no specific enum values are
   * specified.
   *
   * <p>
   * The ordering of this list corresponds to the order of values in
   * {@link #getEnumValues()}, and may include empty strings if a value does not
   * have a corresponding description.
   * </p>
   */
  List<String> getEnumDescriptions();

  /**
   * The default value of this parameter, or {@code null} if a default value is
   * not defined.
   */
  String getDefault();
}
