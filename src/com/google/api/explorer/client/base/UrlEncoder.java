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

import com.google.gwt.http.client.URL;

/**
 * Simple URL encoding interface that allows the URL encoding to be pluggable (for testing).
 *
 */
public interface UrlEncoder {
  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   *
   * @param decodedURLComponent URL query string to encode.
   * @return An escaped URL string.
   */
  String encodeQueryString(String decodedURLComponent);

  /**
   * Returns a string where escaped characters have been converted to
   * their raw string equivalents.
   *
   * @param encodedQueryComponent URL query string component that is already encoded.
   * @return A decoded query string component.
   */
  String decodeQueryString(String encodedQueryComponent);

  /**
   * Returns a string where all characters that are not valid for a URL
   * component have been escaped. The escaping of a character is done by
   * converting it into its UTF-8 encoding and then encoding each of the
   * resulting bytes as a %xx hexadecimal escape sequence.
   *
   * @param decodedURLComponent URL query string to encode.
   * @return An escaped URL string.
   */
  String encodePathSegment(String decodedURLComponent);

  /**
   * Default implementation that delegates to the GWT URL encoder.
   */
  static final UrlEncoder DEFAULT = new UrlEncoder() {
    @Override
    public String encodePathSegment(String decodedURLComponent) {
      return URL.encodePathSegment(decodedURLComponent);
    }

    @Override
    public String encodeQueryString(String decodedURLComponent) {
      return URL.encodeQueryString(decodedURLComponent);
    }

    @Override
    public String decodeQueryString(String encodedQueryComponent) {
      return URL.decodeQueryString(encodedQueryComponent);
    }
  };
}
