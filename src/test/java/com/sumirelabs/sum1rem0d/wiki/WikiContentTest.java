package com.sumirelabs.sum1rem0d.wiki;

import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikiContentTest {
    @Test void everyFeatureHasOneCategoryAndAnArticle() {
        var assigned = new HashSet<String>();
        for (var category : WikiContent.categories()) {
            assertFalse(category.features().isEmpty());
            for (String id : category.features()) {
                assertTrue(assigned.add(id)); assertFalse(WikiContent.article(id).paragraphs().isEmpty());
                assertEquals(category, WikiContent.categoryFor(id));
            }
        }
        assertEquals(new HashSet<>(ConfigSchema.features().stream().map(ConfigSchema.Feature::id).toList()), assigned);
        assertFalse(WikiContent.introduction().isEmpty());
    }
    @Test void searchFindsJapaneseTextAndIdsWithoutDependingOnCase() {
        assertTrue(WikiContent.search("石鹸").stream().anyMatch(article -> article.feature().equals("cleanliness")));
        assertTrue(WikiContent.search("SOAP").stream().anyMatch(article -> article.feature().equals("cleanliness")));
        assertEquals(List.of("ore_drops"), WikiContent.search("ORE_DROPS").stream().map(WikiContent.Article::feature).toList());
        assertTrue(WikiContent.search("nonexistent_feature_search_123").isEmpty());
    }
    @Test void welcomePreferencesDistinguishFirstVisitAndEveryJoin() {
        assertTrue(WikiContent.autoEligible(true, true, false, true));
        assertTrue(WikiContent.autoEligible(true, true, true, false));
        assertFalse(WikiContent.autoEligible(true, true, true, true));
        assertFalse(WikiContent.autoEligible(false, true, false, false));
        assertFalse(WikiContent.autoEligible(true, false, false, false));
    }
}
