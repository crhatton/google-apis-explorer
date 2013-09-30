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

import com.google.api.explorer.client.base.ApiDirectory.Factory;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition.Label;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Tests for {@link ApiDirectory}.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiDirectoryTest extends TestCase {

  private ApiDirectory decodeTestJson() {
    InputStream stream = getClass().getResourceAsStream("directory-small.json");
    if (stream == null) {
      fail("directory-small.json missing");
    }
    try {
      Reader reader = new InputStreamReader(stream, Charsets.UTF_8);
      String jsonString = CharStreams.toString(reader);
      stream.close();

      Factory factory = AutoBeanFactorySource.create(Factory.class);
      return AutoBeanCodex.decode(factory, ApiDirectory.class, jsonString).as();
    } catch (IOException e) {
      fail("IOException: " + e.getMessage());
    }
    return null;
  }

  public void testDirectory() {
    ApiDirectory directory = decodeTestJson();

    assertEquals(3, directory.getItems().size());
    for (ServiceDefinition def : directory.getItems()) {
      // All service definitions share these
      assertEquals("TBD-16", def.getIcons().getIcon16Url());
      assertEquals("TBD-32", def.getIcons().getIcon32Url());
      assertEquals("TBD", def.getDocumentationLink());

      if (def.getName().equals("anotherapi")) {
        assertEquals("v1.1beta3", def.getVersion());
        assertEquals("Another sample API for testing", def.getDescription());
        assertEquals("https://www.googleapis.com/discovery/v0.3/describe/anotherapi/v1.1beta3",
            def.getDiscoveryLink());
        assertEquals(ImmutableSet.of(Label.DEPRECATED, Label.LABS), def.getLabels());

        // TODO(jasonhall): There is a bug with decoding JSON in JRE where
        // booleans are always false. When this bug is fixed, uncomment this
        // line. This test passes when run as a GWT test.
        // assertTrue(def.isPreferred());
      } else if (def.getName().equals("testapi")) {

        // All testapi service defs share this.
        assertEquals("Sample API for testing", def.getDescription());

        if (def.getVersion().equals("v1")) {
          assertEquals("https://www.googleapis.com/discovery/v0.3/describe/testapi/v1",
              def.getDiscoveryLink());
          assertEquals(ImmutableSet.of(Label.STABLE, Label.LABS), def.getLabels());
          assertFalse(def.isPreferred());
        } else if (def.getVersion().equals("v2")) {
          assertEquals("https://www.googleapis.com/discovery/v0.3/describe/testapi/v2",
              def.getDiscoveryLink());
          assertEquals(ImmutableSet.of(Label.LABS), def.getLabels());

          // TODO(jasonhall): There is a bug with decoding JSON in JRE where
          // booleans are always false. When this bug is fixed, uncomment this
          // line. This test passes when run as a GWT test.
          // assertTrue(def.isPreferred());
        } else {
          // Should have been picked up by one of the above
          fail("Unknown service definition: " + def.getName() + " " + def.getVersion());
        }
      } else {
        // Should have been picked up by one of the above
        fail("Unknown service definition: " + def.getName() + " " + def.getVersion());
      }
    }
  }
}
