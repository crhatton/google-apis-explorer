/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.editors;

import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.ApiParameter.Type;
import com.google.api.explorer.client.editors.EditorFactory.DecimalValidator;
import com.google.api.explorer.client.editors.EditorFactory.IntegerValidator;
import com.google.api.explorer.client.editors.EditorFactory.MinimumMaximumValidator;
import com.google.api.explorer.client.editors.EditorFactory.PatternValidator;
import com.google.api.explorer.client.editors.EditorFactory.RequiredValidator;
import com.google.api.explorer.client.editors.EnumEditor.EnumValidator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.Widget;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.List;

/**
 * Tests for {@link EditorFactory} and all {@link Editor}s and {@link Validator}
 * s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EditorFactoryTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    // Even though this functionality is not ready to be released, we still want
    // to be able to test it.
    EditorFactory.enableRepeatedParameters = true;
  }

  /** A simple parameter (no restrictions) results in a BasicEditor. */
  public void testEditorFactory_basic() {
    Editor editor = createParameter(false, Type.STRING, null, null, null, null, false, null);
    assertTrue(editor instanceof BasicEditor);

    editor.setValue(ImmutableList.of("abc"));
    assertEquals(ImmutableList.of("abc"), editor.getValue());
  }

  /** A required parameter results in a BasicEditor with a RequiredValidator. */
  public void testEditorFactory_required() {
    Editor editor = createParameter(false, Type.STRING, null, null, null, null, true, null);
    assertTrue(editor instanceof BasicEditor);
    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof RequiredValidator);

    // No value given.
    assertInvalid(editor);

    // Empty value given.
    assertInvalid(editor, "");

    // That's better.
    assertValid(editor, "abc");
  }

  /**
   * A parameter with a pattern results in a BasicEditor with a
   * PatternValidator.
   */
  public void testEditorFactory_pattern() {
    Editor editor = createParameter(false, Type.STRING, null, null, null, null, false, "[0-9]+");
    assertTrue(editor instanceof BasicEditor);
    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof PatternValidator);
    assertEquals("[0-9]+", ((PatternValidator) editor.validators.get(0)).pattern);

    assertValid(editor, "000");
    assertInvalid(editor, "abc");
  }

  /**
   * A boolean parameter results in an EnumEditor with values "true" and
   * "false".
   */
  public void testEditorFactory_boolean() {
    Editor editor = createParameter(false, Type.BOOLEAN, null, null, null, null, false, null);
    assertTrue(editor instanceof EnumEditor);
    assertEquals(ImmutableList.of("true", "false"), ((EnumEditor) editor).enumValues);
    assertNull(((EnumEditor) editor).enumDescriptions);
    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof EnumValidator);
    
    assertValid(editor, "");
    assertValid(editor, "true");
    assertValid(editor, "false");
    assertInvalid(editor, "unknown");
  }

  /**
   * An integer parameter results in a BasicEditor with an IntegerValidator
   */
  public void testEditorFactory_integer() {
    Editor editor = createParameter(false, Type.INTEGER, null, null, null, null, false, null);
    assertTrue(editor instanceof BasicEditor);
    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof IntegerValidator);

    assertValid(editor, "");
    assertValid(editor, "1");
    assertValid(editor, "-1");
    assertValid(editor, "1234567890123456789");
    assertInvalid(editor, "1.1");
    assertInvalid(editor, "-1.1");
  }

  /**
   * An integer parameter with a minimum and maximum results in a BasicEditor
   * with a MinimumMaximumValidator.
   */
  public void testEditorFactory_integerMinMax() {
    Editor editor = createParameter(false, Type.INTEGER, null, null, "1", "10", false, null);
    assertTrue(editor instanceof BasicEditor);
    assertEquals(2, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof IntegerValidator);
    assertTrue(editor.validators.get(1) instanceof MinimumMaximumValidator);

    assertValid(editor, "");
    assertValid(editor, "4");
    assertValid(editor, "1");
    assertValid(editor, "10");
    assertInvalid(editor, "-1");
    assertInvalid(editor, "11");
  }

  /**
   * An integer parameter results in a BasicEditor with an IntegerValidator
   */
  public void testEditorFactory_decimal() {
    Editor editor = createParameter(false, Type.DECIMAL, null, null, null, null, false, null);
    assertTrue(editor instanceof BasicEditor);
    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof DecimalValidator);

    assertValid(editor, "");
    assertValid(editor, "1.1");
    assertValid(editor, "10");
    assertValid(editor, "12345678901234567890.012345678901234567890");
    assertValid(editor, "-4");
    assertValid(editor, "-4.4");
    assertInvalid(editor, ".4");
    assertInvalid(editor, "4.");
  }

  /**
   * A parameter with enum values results in an EnumEditor with those values as
   * options.
   */
  public void testEditorFactory_enumValues() {
    Editor editor = createParameter(false,
        Type.STRING,
        ImmutableList.of("foo", "bar"),
        ImmutableList.of("Foo desc", "Bar desc"),
        null,
        null,
        false,
        null);
    assertTrue(editor instanceof EnumEditor);
    assertEquals(ImmutableList.of("foo", "bar"), ((EnumEditor) editor).enumValues);
    assertEquals(ImmutableList.of("Foo desc", "Bar desc"), ((EnumEditor) editor).enumDescriptions);

    assertEquals(1, editor.validators.size());
    assertTrue(editor.validators.get(0) instanceof EnumValidator);

    assertValid(editor, "foo");
    assertInvalid(editor, "bad");
  }

  /** A repeated parameter results in a RepeatedEditor of BasicEditors. */
  public void testEditorFactory_repeated() {
    Editor editor = createParameter(true, Type.STRING, null, null, null, null, false, null);
    assertTrue(editor instanceof RepeatedEditor);
    assertTrue(((RepeatedEditor) editor).innerEditor instanceof BasicEditor);
    assertEquals(0, editor.validators.size());
    assertEquals(0, ((RepeatedEditor) editor).innerEditor.validators.size());
  }

  /**
   * When the repeated parameters UI is disabled, isRepeated() is never called
   * and the parameter results in a BasicEditor.
   */
  // TODO(jasonhall): Remove this test when the enableRepeatedParameters flag is
  // removed.
  public void testEditorFactory_repeatedDisabled() {
    EditorFactory.enableRepeatedParameters = false;
    ApiParameter parameter = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(parameter.getType()).andReturn(Type.STRING);
    EasyMock.expect(parameter.getEnumValues()).andReturn(null);
    EasyMock.expect(parameter.getEnumDescriptions()).andReturn(null);
    EasyMock.expect(parameter.getMinimum()).andReturn(null);
    EasyMock.expect(parameter.getMaximum()).andReturn(null);
    EasyMock.expect(parameter.isRequired()).andReturn(false);
    EasyMock.expect(parameter.getPattern()).andReturn(null);
    EasyMock.replay(parameter);

    Editor editor = EditorFactory.forParameter(parameter);
    EasyMock.verify(parameter);

    assertTrue(editor instanceof BasicEditor);
  }

  /**
   * For a complex parameter (one that is required, repeated, must match a
   * pattern, and has valid enum values) the editor hierarchy and validators
   * result as expected.
   */
  public void testEditorFactory_complex() {
    Editor editor = createParameter(true,
        Type.STRING,
        ImmutableList.of("foo", "bar"),
        ImmutableList.of("Foo desc", "Bar desc"),
        null,
        null,
        true,
        "[a-z]{3}");
    assertTrue(editor instanceof RepeatedEditor);

    Editor innerEditor = ((RepeatedEditor) editor).innerEditor;
    assertTrue(innerEditor instanceof EnumEditor);

    assertEquals(3, innerEditor.validators.size());
    assertTrue(innerEditor.validators.get(0) instanceof EnumValidator);
    assertTrue(innerEditor.validators.get(1) instanceof RequiredValidator);
    assertTrue(innerEditor.validators.get(2) instanceof PatternValidator);
    assertEquals("[a-z]{3}", ((PatternValidator) innerEditor.validators.get(2)).pattern);

    assertValid(editor, "foo", "bar");

    // Doesn't match enum validator.
    assertInvalid(editor, "foo", "bad");

    // Doesn't match pattern.
    assertInvalid(editor, "foo", "barrrrrr");

    // Value is required not to be empty.
    assertInvalid(editor, "");
  }

  /**
   * Create an editor based on a mock parameter with the given qualities.
   *
   * @param minimum TODO
   * @param maximum TODO
   */
  private static Editor createParameter(boolean repeated,
      Type type,
      List<String> enumValues,
      List<String> enumDescriptions,
      String minimum,
      String maximum,
      boolean required,
      String pattern) {
    ApiParameter parameter = EasyMock.createControl().createMock(ApiParameter.class);
    EasyMock.expect(parameter.isRepeated()).andReturn(repeated);
    EasyMock.expect(parameter.getType()).andReturn(type);
    EasyMock.expect(parameter.getEnumValues()).andReturn(enumValues);
    EasyMock.expect(parameter.getEnumDescriptions()).andReturn(enumDescriptions);
    EasyMock.expect(parameter.getMinimum()).andReturn(minimum);
    EasyMock.expect(parameter.getMaximum()).andReturn(maximum);
    EasyMock.expect(parameter.isRequired()).andReturn(required);
    EasyMock.expect(parameter.getPattern()).andReturn(pattern);

    EasyMock.replay(parameter);

    Editor editor = EditorFactory.forParameter(parameter);
    EasyMock.verify(parameter);

    editor.setView(new MockEditorView());

    return editor;
  }

  /** Asserts that if the editor is given the value(s), it will be valid. */
  private static void assertValid(Editor editor, String... values) {
    editor.setValue(ImmutableList.copyOf(values));
    assertTrue(editor.isValid());
  }

  /** Asserts that if the editor is given the value(s), it will be invalid. */
  private static void assertInvalid(Editor editor, String... values) {
    editor.setValue(ImmutableList.copyOf(values));
    assertFalse(editor.isValid());
  }

  private static class MockEditorView implements EditorView {
    private List<String> values = Lists.newArrayList("");

    @Override
    public List<String> getValue() {
      return values;
    }

    @Override
    public void setValue(List<String> values) {
      this.values = ImmutableList.copyOf(values);
    }

    @Override
    public void setEnabled(boolean enabled) {
    }

    @Override
    public Widget asWidget() {
      return null;
    }

    @Override
    public void displayValidation(boolean valid) {
    }
  }
}
