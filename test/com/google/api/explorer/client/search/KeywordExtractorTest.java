// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * Tests for the keyword extractor.
 *
 */
public class KeywordExtractorTest extends TestCase {
  private static final boolean NO_STRIP_PUNCTUATION = false;

  private final KeywordExtractor extractor = new KeywordExtractor();

  /**
   * Tests the keyword extractor that splits a string of text into a list of viable search keywords.
   */
  public void testKeywordExtractor() {
    assertParses(ImmutableSet.of("an", "indexable", "string"), "an indexable string");
    assertParses(ImmutableSet.of("a.method.name"), "a.method.name");
    assertParses(ImmutableSet.of("word"), ";;;;;;word;;;;;???");
    assertParses(ImmutableSet.of("lll", "word", "help"), "lll;;;word???help!");
    assertParses(ImmutableSet.of("case", "insensitivity", "test"), "CaSe InsenSITIVItY tEsT");
    assertParses(ImmutableSet.of("v1.2"), "!!!!!v1.2?????");
    assertParses(ImmutableSet.of("end", "sentence", "punctuation"), "End sentence punctuation.");
    assertParses(ImmutableSet.of("middle", "punctuation"), "Middle. punctuation");
    assertParses(ImmutableSet.<String>of(), "::;;''';';';;'");
    assertParses(ImmutableSet.<String>of(), "");
  }

  /** Tests the keyword extractor that is used to split up search query strings. */
  public void testQueryKeywordExtractor() {
    assertToComplete(ImmutableList.of("query", "for", "urlshortener."), "query for urlshortener.");
    assertToComplete(ImmutableList.<String>of(), "");
    assertToComplete(ImmutableList.of("url", "short"), "url short");
    assertToComplete(
        ImmutableList.of("url", "short", "punctuation.", ""), "url short punctuation. ");
    assertToComplete(ImmutableList.of(".capture", "leading", "punctuation.", ""),
        ".capture leading punctuation. ");
  }

  private void assertParses(Set<String> expected, String input) {
    assertEquals(expected, extractor.asSet(input));
  }

  private void assertToComplete(List<String> expectedFragments, String input) {
    List<String> fragments = ImmutableList.copyOf(extractor.split(input, NO_STRIP_PUNCTUATION));
    assertEquals(expectedFragments, fragments);
  }
}
