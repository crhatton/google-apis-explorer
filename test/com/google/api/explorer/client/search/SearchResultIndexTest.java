// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.search;

import com.google.api.explorer.client.search.SearchResultIndex.KeywordCallback;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test the search result indexer.
 *
 */
public class SearchResultIndexTest extends TestCase {
  /**
   * Simple passthrough indexing strategy that convert and index to the stream that will eventually
   * become the same index.
   */
  private static class PassthroughIndex
      implements IndexingStrategy<SetMultimap<String, SearchResult>> {

    @Override
    public Iterable<SearchEntry> index(SetMultimap<String, SearchResult> fragment) {
      // Invert the map to extract all keywords for each result.
      SetMultimap<SearchResult, String> resultToKeywords = HashMultimap.create();
      for (Map.Entry<String, SearchResult> mapping : fragment.entries()) {
        resultToKeywords.put(mapping.getValue(), mapping.getKey());
      }

      List<SearchEntry> entries = Lists.newArrayListWithCapacity(resultToKeywords.keySet().size());
      for (SearchResult result : resultToKeywords.keySet()) {
        entries.add(new SearchEntry(result, resultToKeywords.get(result)));
      }

      return entries;
    }
  }

  private SearchResultIndex index = new SearchResultIndex();
  private PassthroughIndex strategy = new PassthroughIndex();

  /** Test the most basic query. */
  public void testSingleWordQuery() {
    SearchResult result1 = createUniqueSearchResult();

    SetMultimap<String, SearchResult> fragment = HashMultimap.create();
    fragment.put("keyword", result1);

    index.addDocument(fragment, strategy);

    assertEquals(result1, index.search("keyword").iterator().next());
    assertTrue(!index.search("notakeyword").iterator().hasNext());
  }

  /** Test queries that multiple documents. */
  public void testMultipleDocuments() {
    SearchResult result1 = createUniqueSearchResult();
    SearchResult result2 = createUniqueSearchResult();

    SetMultimap<String, SearchResult> fragment = HashMultimap.create();
    fragment.put("keyword", result1);
    fragment.put("keyword2", result2);
    fragment.putAll("commonkeyword", ImmutableSet.of(result1, result2));

    index.addDocument(fragment, strategy);

    verifyMultipleDocumentQueries(result1, result2);
  }

  /** Test queries that involve multiple documents from different indexing strategies. */
  public void testMergeIndexes() {
    SearchResult result1 = createUniqueSearchResult();
    SetMultimap<String, SearchResult> fragment1 = HashMultimap.create();
    fragment1.put("keyword", result1);
    fragment1.put("commonkeyword", result1);
    index.addDocument(fragment1, strategy);

    assertEquals(ImmutableSet.of(result1), ImmutableSet.copyOf(index.search("keyword")));
    assertEquals(ImmutableSet.of(), ImmutableSet.copyOf(index.search("keyword2")));

    SearchResult result2 = createUniqueSearchResult();
    SetMultimap<String, SearchResult> fragment2 = HashMultimap.create();
    fragment2.put("keyword2", result2);
    fragment2.put("commonkeyword", result2);
    index.addDocument(fragment2, strategy);

    verifyMultipleDocumentQueries(result1, result2);
  }

  /** Test that querying an empty index works. */
  public void testEmptyIndex() {
    assertTrue(ImmutableSet.copyOf(index.search("keyword")).isEmpty());
  }

  /** Test that our keyword update callback works properly. */
  public void testKeywordUpdates() {
    final Set<String> lastKeywords = Sets.newHashSet();

    index.setKeywordCallback(new KeywordCallback() {
      @Override
      public void newKeywordsAdded(Set<String> keywords) {
        lastKeywords.clear();
        lastKeywords.addAll(keywords);
      }
    });

    assertTrue(lastKeywords.isEmpty());

    SearchResult result1 = createUniqueSearchResult();
    SetMultimap<String, SearchResult> fragment1 = HashMultimap.create();
    fragment1.put("keyword", result1);
    fragment1.put("commonkeyword", result1);
    index.addDocument(fragment1, strategy);

    assertEquals(ImmutableSet.of("keyword", "commonkeyword"), lastKeywords);

    SearchResult result2 = createUniqueSearchResult();
    SetMultimap<String, SearchResult> fragment2 = HashMultimap.create();
    fragment2.put("keyword2", result2);
    fragment2.put("commonkeyword", result2);
    index.addDocument(fragment2, strategy);

    assertEquals(ImmutableSet.of("keyword2"), lastKeywords);

    SearchResult result3 = createUniqueSearchResult();
    SetMultimap<String, SearchResult> fragment3 = HashMultimap.create();
    fragment3.put("commonkeyword", result3);
    index.addDocument(fragment3, strategy);

    assertTrue(lastKeywords.isEmpty());
  }

  private void verifyMultipleDocumentQueries(SearchResult result1, SearchResult result2) {
    // Test a few single word queries.
    assertEquals(ImmutableSet.of(result1), ImmutableSet.copyOf(index.search("keyword")));
    assertEquals(ImmutableSet.of(result2), ImmutableSet.copyOf(index.search("keyword2")));
    assertEquals(
        ImmutableSet.of(result1, result2), ImmutableSet.copyOf(index.search("commonkeyword")));

    // Test the valid multiple word queries.
    assertEquals(
        ImmutableSet.of(result1), ImmutableSet.copyOf(index.search("keyword commonkeyword")));
    assertEquals(
        ImmutableSet.of(result2), ImmutableSet.copyOf(index.search("keyword2 commonkeyword")));
    assertTrue(Iterables.isEmpty(index.search("commonkeyword keyword keyword2")));
  }

  private SearchResult createUniqueSearchResult() {
    SearchResult result = EasyMock.createMock(SearchResult.class);
    EasyMock.replay(result);
    return result;
  }
}
