package com.sumirelabs.sum1rem0d.wiki;

import java.text.Normalizer;
import java.util.*;

/** Shared deterministic matching for the Wiki and Config; no client/bootstrap dependency. */
public final class SearchSuggestions {
  private SearchSuggestions() {}

  public record Entry(String id, String label, String details) {}

  private static String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
  }

  public static List<Entry> find(Collection<Entry> entries, String query, int limit) {
    String text = normalize(query);
    if (text.isEmpty() || limit <= 0) return List.of();
    return entries.stream()
        .filter(e -> rank(e, text) < 5)
        .sorted(Comparator.comparingInt((Entry e) -> rank(e, text)).thenComparing(Entry::id))
        .limit(limit)
        .toList();
  }

  private static int rank(Entry e, String q) {
    String id = normalize(e.id()), label = normalize(e.label());
    if (id.equals(q) || label.equals(q)) return 0;
    if (id.startsWith(q) || label.startsWith(q)) return 1;
    if (id.contains(q) || label.contains(q)) return 2;
    if (normalize(e.details()).contains(q)) return 3;
    return 5;
  }
}
