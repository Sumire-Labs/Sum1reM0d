package com.sumirelabs.sum1rem0d.wiki;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bundled offline documentation, shared by the reader and coverage tests. */
public final class WikiContent {
    public record Category(String id, String title, String description, List<String> features) {}
    public record Article(String feature, List<String> paragraphs) {
        public String title() { return schema(feature).label(); }
    }
    private static final JsonObject ROOT;
    private static final List<Category> CATEGORIES;
    private static final Map<String, Article> ARTICLES;
    static {
        try (var input = WikiContent.class.getResourceAsStream("/assets/sum1rem0d/wiki/guide.json")) {
            if (input == null) throw new IOException("Missing Wiki content");
            ROOT = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var categories = new ArrayList<Category>(); var articles = new LinkedHashMap<String, Article>(); var assigned = new HashSet<String>();
            for (var raw : ROOT.getAsJsonArray("categories")) {
                var item = raw.getAsJsonObject(); var ids = strings(item.getAsJsonArray("features"));
                for (String id : ids) if (!assigned.add(id)) throw new IllegalStateException("Duplicate Wiki category entry: " + id);
                categories.add(new Category(item.get("id").getAsString(), item.get("title").getAsString(), item.get("description").getAsString(), ids));
            }
            for (var raw : ROOT.getAsJsonArray("articles")) {
                var item = raw.getAsJsonObject(); String id = item.get("feature").getAsString(); var paragraphs = strings(item.getAsJsonArray("paragraphs"));
                if (paragraphs.isEmpty() || articles.put(id, new Article(id, paragraphs)) != null) throw new IllegalStateException("Invalid Wiki article: " + id);
            }
            var expected = new HashSet<>(ConfigSchema.features().stream().map(ConfigSchema.Feature::id).toList());
            if (!expected.equals(assigned) || !expected.equals(articles.keySet())) throw new IllegalStateException("Wiki must cover every configured feature exactly once");
            CATEGORIES = List.copyOf(categories); ARTICLES = Collections.unmodifiableMap(articles);
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    private WikiContent() {}
    private static List<String> strings(JsonArray array) { var result = new ArrayList<String>(); array.forEach(value -> result.add(value.getAsString())); return List.copyOf(result); }
    public static List<String> introduction() { return strings(ROOT.getAsJsonArray("introduction")); }
    public static List<Category> categories() { return CATEGORIES; }
    public static Article article(String feature) { return Objects.requireNonNull(ARTICLES.get(feature)); }
    public static ConfigSchema.Feature schema(String id) { return ConfigSchema.features().stream().filter(feature -> feature.id().equals(id)).findFirst().orElseThrow(); }
    public static Category category(String id) { return CATEGORIES.stream().filter(category -> category.id().equals(id)).findFirst().orElseThrow(); }
    public static Category categoryFor(String feature) { return CATEGORIES.stream().filter(category -> category.features().contains(feature)).findFirst().orElseThrow(); }
    public static List<Article> search(String query) {
        String text = query.strip().toLowerCase(Locale.ROOT);
        return ARTICLES.values().stream().filter(article -> (article.feature() + " " + article.title() + " " + String.join(" ", article.paragraphs())).toLowerCase(Locale.ROOT).contains(text)).toList();
    }
    public static boolean autoEligible(boolean enabled, boolean autoShow, boolean firstOnly, boolean seen) { return enabled && autoShow && (!firstOnly || !seen); }
}
