// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.base;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.Map;

/**
 * Class which can be used for common functionality across REST and RPC specializations of the
 * ApiService.
 *
 */
public class ApiServiceHelper {
  /**
   * Returns the {@link ApiMethod} identified by the old-style method which is constructed by
   * joining resource and method names and omitting the service names.
   */
  public static ApiMethod resolveMethod(ApiService service, String oldMethodIdentifier) {
    // Try to look up the method directly.
    ApiMethod method = service.method(oldMethodIdentifier);
    if (method == null) {
      // TODO(user): Remove this when all docs sets have been regenerated.
      // Try to look up the method with a simple search of available method names.
      // This is only to support the old method of embedding services which did not include the
      // service name in the method name.
      for (Map.Entry<String, ApiMethod> oneMethod : service.allMethods().entrySet()) {
        String fullMethodName = oneMethod.getKey();

        int firstSeparator = fullMethodName.indexOf(".");
        String methodWithoutService =
            firstSeparator > 0 ? fullMethodName.substring(firstSeparator + 1) : fullMethodName;
        if (methodWithoutService.equals(oldMethodIdentifier)) {
          // We found the method!
          method = oneMethod.getValue();
          break;
        }
      }
    }

    return method;
  }

  /**
   * Generates a map of all of the "kind"s in the service, mapped to the methods which use those
   * kinds as a request parameter.
   */
  public static Multimap<String, ApiMethod> generateKindUsages(ApiService service) {
    Multimap<String, ApiMethod> usages = HashMultimap.create();

    for (ApiMethod method : service.allMethods().values()) {
      Schema requestSchema = service.requestSchema(method);
      if (requestSchema != null) {
        Map<String, Schema> properties = Objects.firstNonNull(
            requestSchema.getProperties(), Collections.<String, Schema>emptyMap());
        Schema kind = properties.get(Schema.KIND_KEY);
        if (kind != null) {
          String kindValue = kind.getDefault();
          if (kindValue != null) {
            usages.put(kindValue, method);
          }
        }
      }
    }

    return usages;
  }
}
