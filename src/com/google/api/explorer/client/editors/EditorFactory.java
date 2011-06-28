/*
 * Copyright (C) 2011 Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.editors;

import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.ApiParameter.Type;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.math.BigInteger;
import java.util.List;

/**
 * Factory for {@link Editor}s that will produce the appropriate editor based on
 *  the properties of the parameter being edited. The factory will also add
 * appropriate {@link Validator}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EditorFactory {

  private static final List<String> TRUE_FALSE =
      ImmutableList.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());

  // The UI for repeated parameters is not yet fully baked, so this feature will
  // be disabled for now. The unit test will still test the functionality
  // though, and enables this for the test.
  // TODO(jasonhall): Finalize UI for repeated parameters and remove this flag.
  @VisibleForTesting
  static boolean enableRepeatedParameters = false;

  /**
   * Identifies the relevant {@link Editor} implementation for the given
   * {@link ApiParameter}.
   */
  public static Editor forParameter(ApiParameter parameter) {
    return forParameter(parameter, false);
  }

  /**
   * Identifies the revelant {@link Editor} implementation for the given
   * parameter, ignoring whether the parameter is repeated.
   *
   * @param ignoreRepeated is necessary because a {@link RepeatedEditor} wraps
   *        an inner {@link Editor} implementation to display repeatedly. When
   *        ignoreRepeated is true, this method is used to identify the
   *        correct inner editor to use.
   */
  static Editor forParameter(ApiParameter parameter, boolean ignoreRepeated) {
    if (enableRepeatedParameters && !ignoreRepeated && parameter.isRepeated()) {
      return new RepeatedEditor(forParameter(parameter, true));
    }

    // In most cases we'll just want to use a BasicEditor.
    Editor editor = new BasicEditor();

    Type type = parameter.getType();
    // If the parameter expects a boolean value, use an EnumEditor with values
    // "true" and "false".
    if (type == Type.BOOLEAN) {
      editor = new EnumEditor(TRUE_FALSE, null);
    }

    // Get possible enum values and descriptions. If enum values are specified,
    // then use an EnumEditor.
    List<String> enumValues = parameter.getEnumValues();
    List<String> enumDescriptions = parameter.getEnumDescriptions();

    if (enumValues != null && !enumValues.isEmpty()) {
      editor = new EnumEditor(enumValues, enumDescriptions);
    }

    // If the parameter expects an integer value, add an IntegerValidator.
    String minimum = parameter.getMinimum();
    String maximum = parameter.getMaximum();
    final String pattern = parameter.getPattern();

    maybeAddValidatorTo(editor, new IntegerValidator(), type == Type.INTEGER);
    maybeAddValidatorTo(
        editor, new MinimumMaximumValidator(minimum, maximum), minimum != null || maximum != null);
    maybeAddValidatorTo(editor, new DecimalValidator(), type == Type.DECIMAL);
    maybeAddValidatorTo(editor, new RequiredValidator(), parameter.isRequired());
    maybeAddValidatorTo(editor, new PatternValidator(pattern), pattern != null);

    return editor;
  }

  /**
   * Conditionally adds the given validator to the given editor, if the
   * condition is true.
   */
  private static void maybeAddValidatorTo(Editor editor, Validator validator, boolean condition) {
    if (condition) {
      editor.addValidator(validator);
    }
  }

  @VisibleForTesting
  static final class MinimumMaximumValidator implements Validator {
    private final BigInteger minimum;
    private final BigInteger maximum;

    MinimumMaximumValidator(String minimum, String maximum) {
      this.minimum = minimum == null ? null : new BigInteger(minimum);
      this.maximum = maximum == null ? null : new BigInteger(maximum);
    }

    /**
     * Returns true if all values are parseable integers within the bounds of
     * the minimum and maximum.
     */
    @Override
    public boolean isValid(List<String> values) {
      for (String value : values) {
        if (value.isEmpty()) {
          continue;
        }
        BigInteger val;
        try {
          val = new BigInteger(value);
        } catch (NumberFormatException nfe) {
          return false;
        }
        if (minimum != null && val.compareTo(minimum) == -1) {
          return false;
        }
        if (maximum != null && val.compareTo(maximum) == 1) {
          return false;
        }
      }
      return true;
    }
  }

  @VisibleForTesting
  static class IntegerValidator extends PatternValidator {
    private static final String INTEGER_PATTERN = "-?[0-9]+";

    public IntegerValidator() {
      super(INTEGER_PATTERN);
    }
  }

  @VisibleForTesting
  static final class DecimalValidator extends PatternValidator {
    private static final String DECIMAL_PATTERN = "-?[0-9]+(\\.[0-9]+)?";

    public DecimalValidator() {
      super(DECIMAL_PATTERN);
    }
  }

  @VisibleForTesting
  static class PatternValidator implements Validator {
    @VisibleForTesting
    final String pattern;

    private PatternValidator(String pattern) {
      this.pattern = pattern;
    }

    /** Returns true if all values are either empty or match the pattern. */
    @Override
    public boolean isValid(List<String> values) {
      for (String value : values) {
        if (!value.isEmpty() && !value.matches(pattern)) {
          return false;
        }
      }
      return true;
    }
  }

  @VisibleForTesting
  static final class RequiredValidator implements Validator {
    /**
     * Returns true if at least one string is given, and none of the values are
     * empty.
     */
    @Override
    public boolean isValid(List<String> values) {
      if (values.isEmpty()) {
        return false;
      }
      for (String value : values) {
        if (value.isEmpty()) {
          return false;
        }
      }
      return true;
    }
  }
}
