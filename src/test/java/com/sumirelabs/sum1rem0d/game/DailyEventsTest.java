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
        assertEquals(DailyEvents.FEATURES.size() + 1, reached.size());
    }
    @Test void zeroChanceEmptyPoolAndZeroWeightsCannotSelectAnEvent() {
        assertEquals("", DailyEvents.choose(1, 0, 0, Map.of("vegan", 100.0)));
        assertEquals("", DailyEvents.choose(1, 0, 100, Map.of()));
        assertEquals("", DailyEvents.choose(1, 0, 100, Map.of("vegan", 0.0)));
    }
    @Test void onlyEligiblePositiveWeightCandidateIsChosenAtOneHundredPercent() {
        for (int day = 0; day < 100; day++) assertEquals("growth_sleep", DailyEvents.choose(1, day, 100, Map.of("vegan", 0.0, "growth_sleep", 20.0)));
    }
    @Test void independentPoolsAlwaysSelectOneEligibleFeatureEach(){
        var weights=new LinkedHashMap<String,Double>();DailyEvents.FEATURES.forEach(id->weights.put(id,20.0));
        for(long day=0;day<1000;day++){
            var pair=DailyEvents.choosePools(8217,day,100,100,weights);
            assertTrue(DailyEvents.VISUAL.contains(pair.visual()));assertTrue(DailyEvents.GAMEPLAY.contains(pair.gameplay()));
            assertNotEquals(pair.visual(),pair.gameplay());assertEquals(pair,DailyEvents.choosePools(8217,day,100,100,weights));
        }
    }
    @Test void eachPoolCanBeDisabledWithoutSuppressingTheOther(){
        var weights=Map.of("vegan",10.0,"retro_cobblestone",10.0);
        assertEquals(new DailyEvents.Selection("","vegan"),DailyEvents.choosePools(1,1,0,100,weights));
        assertEquals(new DailyEvents.Selection("retro_cobblestone",""),DailyEvents.choosePools(1,1,100,0,weights));
        assertEquals(new DailyEvents.Selection("",""),DailyEvents.choosePools(1,1,100,100,Map.of()));
    }
}
