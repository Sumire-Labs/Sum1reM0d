package com.sumirelabs.sum1rem0d.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Connection-scoped recipe preview mapping synced by ID; the world seed remains server-only. */
public final class PaxelRecipeMapState {
    private static volatile Map<String, List<String>> selected = Map.of();
    private PaxelRecipeMapState() {}

    public static synchronized void accept(List<String> ids) {
        if (ids.size() != PaxelContent.MATERIALS.size() * 3) { clear(); return; }
        var next = new HashMap<String, List<String>>();
        int cursor = 0;
        for (String material : PaxelContent.MATERIALS) next.put(material, List.copyOf(ids.subList(cursor, cursor += 3)));
        selected = Map.copyOf(next);
    }

    public static List<String> ids(String material) { return selected.getOrDefault(material, List.of()); }
    public static boolean received() { return !selected.isEmpty(); }
    public static synchronized void clear() { selected = Map.of(); }
}
