package com.sumirelabs.sum1rem0d.wiki;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class SearchSuggestionsTest {
  private final List<SearchSuggestions.Entry> entries =
      List.of(
          new SearchSuggestions.Entry("soap", "石鹸", "清潔度を回復"),
          new SearchSuggestions.Entry("soap_extra", "追加石鹸", ""),
          new SearchSuggestions.Entry("cleanliness", "清潔度", "石鹸を使います"));

  @Test
  void ranksExactThenPrefixThenDescription() {
    assertEquals(
        List.of("soap", "soap_extra"),
        SearchSuggestions.find(entries, "ＳＯＡＰ", 5).stream()
            .map(SearchSuggestions.Entry::id)
            .toList());
    assertEquals("soap", SearchSuggestions.find(entries, "石鹸", 5).getFirst().id());
  }

  @Test
  void whitespaceEmptyAndLimit() {
    assertTrue(SearchSuggestions.find(entries, "  ", 5).isEmpty());
    assertTrue(SearchSuggestions.find(entries, "missing", 5).isEmpty());
    assertEquals(1, SearchSuggestions.find(entries, "石鹸", 1).size());
    assertEquals("cleanliness", SearchSuggestions.find(entries, " 清潔度 ", 5).getFirst().id());
  }
}
