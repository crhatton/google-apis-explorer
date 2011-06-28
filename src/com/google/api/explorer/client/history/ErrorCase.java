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

package com.google.api.explorer.client.history;

import com.google.api.explorer.client.base.dynamicjso.DynamicJso;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.user.client.ui.Label;

/**
 * Common error cases and friendly messages to display in the history pane when
 * they are encountered.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public enum ErrorCase {
  NEEDS_AUTH("Authorization required", "This method requires you to be authenticated. "
      + "You may need to switch to private access (above)."),
  FORBIDDEN("Forbidden", "You do not have permission to execute this method."),
  REQUIRED("Required", "A required value was not given."),
  REQUIRED_PARAMETER("Required Parameter", "A required parameter was not given."),
  NOT_FOUND("Not Found", "The resource was not found."),
  INVALID_VALUE("Invalid Value", "An invalid value was given."),
  INVALID_PARAMETER("Invalid Parameter", "An invalid parameter was given."),
  INVALID_QUERY("Invalid Query", "The value of the parameter 'q' is invalid."),

  DAILY_LIMIT_EXCEEDED("Daily Limit Exceeded",
      "You have made too many requests to this service today."),
  USER_RATE_LIMIT_EXCEEDED("User Rate Limit Exceeded",
      "You have made too many requests recently."),
  BACKEND_ERROR("Backend Error", "There was an error on the server. Retry the request."),
  INTERNAL_ERROR("Internal Error", "There was an error on the server."),
  ACCESS_NOT_CONFIGURED("Access Not Configured",
      "The API Explorer does not have access to this service."),
  // Fall-through case that will match any error if one of the above doesn't.
  DEFAULT("", "An error occurred. See the response for details."),


  ;

  private final String message;
  private final String prettyMessage;

  ErrorCase(String message, String prettyMessage) {
    this.message = Preconditions.checkNotNull(message);
    this.prettyMessage = Preconditions.checkNotNull(prettyMessage);
  }

  Label getErrorLabel() {
    return new Label(prettyMessage);
  }

  static ErrorCase forJsonString(String jsonString) {
    DynamicJso error = JsonUtils.<DynamicJso>safeEval(jsonString).get("error");
    int code = error.getInteger("code");
    String message = error.getString("message");
    
    // All 401 responses should show the "needs auth" error message.
    if (code == 401) {
      return ErrorCase.NEEDS_AUTH;
    }

    for (ErrorCase errorCase : values()) {
      if (errorCase.message.isEmpty() || errorCase.message.equalsIgnoreCase(message)) {
        return errorCase;
      }
    }
    return ErrorCase.DEFAULT;
  }
}
