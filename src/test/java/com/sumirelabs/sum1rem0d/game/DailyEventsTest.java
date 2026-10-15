package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyEventsTest {
    @Test void exclusiveStableDrawReachesEveryCandidateAndAllowsNoEvent() {
        var weights = new LinkedHashMap<String, Double>(); DailyEvents.FEATURES.forEach(id -> weights.put(id, 20.0));
        var reached = new HashSet<String>();
        for (int day = 0; day < 10000; day++) {
            String result = DailyEvents.choose(91, day, 80, weights);
            assertTrue(result.isEmpty() || DailyEvents.FEATURES.contains(result));
            assertEquals(result, DailyEvents.choose(91, day, 80, weights)); reached.add(result);
        }
        assertEquals(9, reached.size());
    }
    @Test void zeroChanceEmptyPoolAndZeroWeightsCannotSelectAnEvent() {
        assertEquals("", DailyEvents.choose(1, 0, 0, Map.of("vegan", 100.0)));
        assertEquals("", DailyEvents.choose(1, 0, 100, Map.of()));
        assertEquals("", DailyEvents.choose(1, 0, 100, Map.of("vegan", 0.0)));
    }
    @Test void onlyEligiblePositiveWeightCandidateIsChosenAtOneHundredPercent() {
        for (int day = 0; day < 100; day++) assertEquals("growth_sleep", DailyEvents.choose(1, day, 100, Map.of("vegan", 0.0, "growth_sleep", 20.0)));
    }
}
