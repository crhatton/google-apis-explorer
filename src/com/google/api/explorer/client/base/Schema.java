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

package com.google.api.explorer.client.base;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a description of an request or response, including its type, and,
 * if it's an object, its fields.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public interface Schema {

  public static final String KIND_KEY = "kind";
  public static final String REF_KEY = "$ref";

  /** Possible types. */
  public enum Type {
    @PropertyName("string")
    STRING,

    @PropertyName("array")
    ARRAY,

    @PropertyName("object")
    OBJECT,

    @PropertyName("boolean")
    BOOLEAN,

    @PropertyName("integer")
    INTEGER,

    @PropertyName("number")
    NUMBER,

    @PropertyName("any")
    ANY;
  }

  /** Uniquely-identifying name of this schema. */
  String getId();

  /** Type of this data. */
  Type getType();

  /** All properties of this object, or {@code null} if this is not an object. */
  Map<String, Schema> getProperties();

  /** Type which should be used for the values associated with extra keys. */
  Schema getAdditionalProperties();

  /**
   * Reference to an object defining this property, or {@code null} if this is
   * not an otherwise-referenced object.
   */
  @PropertyName("$ref")
  String getRef();

  /** Default value of this property. */
  String getDefault();

  /**
   * If this is an array, Property definition of items in this array. Otherwise
   * {@code null}.
   */
  Schema getItems();

  /** Description of this property, {@code null} if none is given. */
  String getDescription();

  /**
   * Mapping of an annotation and a set of method identifiers for which that
   * annotation applies to this property.
   */
  Map<String, Set<String>> getAnnotations();

  /**
   * Returns true if this property is required for the method identified by the
   * given method identifier.
   */
  boolean requiredForMethod(String methodIdentifier);

  /**
   * Returns true if this property is mutable (or required) for the method
   * identified by the given method identifier.
   */
  boolean mutableForMethod(String methodIdentifier);

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
   * The minimum valid value for this parameter if the {@link Type} of the
   * parameter's value is {@link Type#INTEGER} or {@link Type#NUMBER}, or
   * {@code null} if no minimum value is defined.
   */
  String getMinimum();

  /**
   * The maximum valid value for this parameter if the {@link Type} of the
   * parameter's value is {@link Type#INTEGER} or {@link Type#NUMBER}, or
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
   * Extra information to indicate whether this schema object is locked, this
   * does not come from Discovery and is a manifestation of the APIs Explorer's
   * manual creation of Schemas to represent top level parameters such as method
   * and version.
   */
  boolean locked();

  /**
   * If this schema is a reference, follow the reference and return the
   * referenced schema, recursively.
   *
   * @param allSchemas Map of schemas which contains all named schemas.
   * @return Referenced schema.
   */
  Schema followRefs(Map<String, Schema> allSchemas);

  /**
   * Wrapper class used by the AutoBeanFactory to provide the implementation of
   * some methods.
   *
   * <p>
   * All of these methods map to a method in {@link Schema} which delegates to
   * the method in this class to provide its implementation.
   * </p>
   */
  static class PropertyWrapper {
    private static final String REQUIRED = "required";
    private static final String MUTABLE = "mutable";

    private static boolean hasAnnotationForMethod(
        Schema property, String annotation, String methodIdentifier) {
      return property.getAnnotations() != null && property.getAnnotations().containsKey(annotation)
          && property.getAnnotations().get(annotation).contains(methodIdentifier);
    }

    /**
     * Returns true if this property is required for the method identified by
     * the given method identifier.
     */
    public static boolean requiredForMethod(AutoBean<Schema> instance, String methodIdentifier) {
      return hasAnnotationForMethod(instance.as(), REQUIRED, methodIdentifier);
    }

    /**
     * Returns true if this property is mutable (or required) for the method
     * identified by the given method identifier.
     */
    public static boolean mutableForMethod(AutoBean<Schema> instance, String methodIdentifier) {
      // Required properties will not be explicitly marked mutable, since
      // mutablility is assumed for required properties.
      return requiredForMethod(instance, methodIdentifier)
          || hasAnnotationForMethod(instance.as(), MUTABLE, methodIdentifier);
    }

    /**
     * Always returns false, only hand created schemas can be locked
     */
    public static boolean locked(AutoBean<Schema> instance) {
      return false;
    }

    /**
     * Unwrap the schema and pass it off to the function that does the real work.
     */
    public static Schema followRefs(AutoBean<Schema> instance, Map<String, Schema> allSchemas) {
      return followRefsHelper(instance.as(), allSchemas);
    }

    /**
     * If this schema is a reference, follow the reference and return the
     * referenced schema, recursively.
     */
    private static Schema followRefsHelper(Schema possiblyARef, Map<String, Schema> allSchemas) {
      if (possiblyARef != null) {
        String ref = possiblyARef.getRef();
        if (ref != null) {
          Schema referenced = allSchemas.get(ref);
          return followRefsHelper(referenced, allSchemas);
        }
      }
      return possiblyARef;
    }
  }
}
