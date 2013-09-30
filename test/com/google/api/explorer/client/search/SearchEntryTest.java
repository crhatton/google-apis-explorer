// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.search;

import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for the search entry.
 *
 */
public class SearchEntryTest extends TestCase {

  /** Test the function which returns a view with additional keywords. */
  public void testKeywordAddition() {
    ServiceDefinition service = EasyMock.createMock(ServiceDefinition.class);
    EasyMock.expect(service.getId()).andReturn(null).anyTimes();
    SearchResult emptyResult = SearchResult.createServiceResult(service);

    EasyMock.replay(service);

    SearchEntry emptyEntry = new SearchEntry(emptyResult, Collections.<String>emptySet());

    Set<String> additionalKeywords = ImmutableSet.of("added", "keywords");
    SearchEntry addedKeywords = emptyEntry.withAdditionalKeywords(additionalKeywords);

    assertEquals(additionalKeywords, addedKeywords.getKeywords());
    assertEquals(emptyResult, addedKeywords.getSearchResult());

    // Test that keywords get merged.
    Set<String> startingKeywords = ImmutableSet.of("starting", "keywords");
    SearchEntry someKeywords = new SearchEntry(emptyResult, startingKeywords);
    SearchEntry moreKeywords = someKeywords.withAdditionalKeywords(additionalKeywords);

    assertEquals(Sets.union(additionalKeywords, startingKeywords), moreKeywords.getKeywords());
  }
}
