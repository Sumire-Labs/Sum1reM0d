package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreDropMappingTest {
    private static final List<String> ENTRIES = List.of("coal=redstone", "copper=lapis", "iron=gold", "gold=copper", "diamond=iron", "redstone=emerald", "lapis=coal", "emerald=diamond");
    private static List<String> permutation(String mode, long seed, long day) {
        return ENTRIES.stream().map(entry -> OreDropMapping.output(ENTRIES, entry.split("=")[0], mode, seed, day, 0)).toList();
    }
    @Test void dailyIsStableAndAlwaysPreservesAllOutputs() {
        var expected = new HashSet<>(permutation("fixed", 0, 0));
        for (long day = -2; day < 30; day++) {
            var first = permutation("daily", 123, day);
            assertEquals(first, permutation("daily", 123, day));
            assertEquals(expected, new HashSet<>(first));
        }
        assertNotEquals(permutation("daily", 123, 0), permutation("daily", 123, 1));
    }
    @Test void worldModeIgnoresDayButUsesWorldSeed() {
        assertEquals(permutation("world", 123, 0), permutation("world", 123, 99));
        assertNotEquals(permutation("world", 123, 0), permutation("world", 456, 0));
    }
    @Test void miningCanSelectEveryCandidateAndUnknownBlocksStayUnchanged() {
        for (int i = 0; i < ENTRIES.size(); i++) assertEquals(ENTRIES.get(i).split("=")[1], OreDropMapping.output(ENTRIES, "coal", "mining", 0, 0, i));
        for (String mode : OreDropMapping.MODES) assertNull(OreDropMapping.output(ENTRIES, "stone", mode, 0, 0, 0));
        assertNull(OreDropMapping.output(List.of(), "coal", "mining", 0, 0, 0));
    }
}
