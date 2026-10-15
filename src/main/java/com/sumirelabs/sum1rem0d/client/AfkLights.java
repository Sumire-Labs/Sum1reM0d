package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.WorldState;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.util.LightCoordsUtil;

/** Client rendering only. Neither light-engine data nor world blocks are changed. */
public final class AfkLights {
    private record Snapshot(ClientLevel level, List<BlockPos> sources, Map<Long, List<BlockPos>> sections, int radius, int brightness) {}
    private static volatile Snapshot snapshot = new Snapshot(null, List.of(), Map.of(), 0, 0);
    private AfkLights() {}
    public static boolean white(UUID id) { return ClientGameState.white(id) && ClientRules.enabled("afk_white"); }
    public static void clear() { snapshot = new Snapshot(null, List.of(), Map.of(), 0, 0); }
    public static void tick() {
        var mc = Minecraft.getInstance(); var level = mc.level;
        if (level == null) { clear(); return; }
        int radius = ClientRules.integer("afk_white", "light_radius"), brightness = ClientRules.integer("afk_white", "light_level");
        List<BlockPos> sources = new ArrayList<>();
        if (ClientRules.enabled("afk_white") && ClientRules.bool("afk_white", "dynamic_light") && brightness > 0)
            for (var player : level.players()) if (player.isAlive() && !player.isSpectator() && white(player.getUUID())) sources.add(player.blockPosition().above().immutable());
        sources.sort(Comparator.comparingLong(BlockPos::asLong));
        Snapshot previous = snapshot;
        if (previous.level == level && previous.radius == radius && previous.brightness == brightness && previous.sources.equals(sources)) return;
        Map<Long, List<BlockPos>> sections = new HashMap<>();
        for (BlockPos source : sources) for (int x = SectionPos.blockToSectionCoord(source.getX() - radius); x <= SectionPos.blockToSectionCoord(source.getX() + radius); x++)
            for (int y = SectionPos.blockToSectionCoord(source.getY() - radius); y <= SectionPos.blockToSectionCoord(source.getY() + radius); y++)
                for (int z = SectionPos.blockToSectionCoord(source.getZ() - radius); z <= SectionPos.blockToSectionCoord(source.getZ() + radius); z++)
                    sections.computeIfAbsent(SectionPos.asLong(x, y, z), key -> new ArrayList<>()).add(source);
        sections.replaceAll((key, value) -> List.copyOf(value));
        snapshot = new Snapshot(level, List.copyOf(sources), Map.copyOf(sections), radius, brightness);
        if (previous.level == level) invalidate(level, previous.sources, previous.radius);
        invalidate(level, sources, radius);
    }
    private static void invalidate(ClientLevel level, List<BlockPos> sources, int radius) {
        for (BlockPos source : sources) level.setSectionRangeDirty(
            SectionPos.blockToSectionCoord(source.getX() - radius - 1), SectionPos.blockToSectionCoord(source.getY() - radius - 1), SectionPos.blockToSectionCoord(source.getZ() - radius - 1),
            SectionPos.blockToSectionCoord(source.getX() + radius + 1), SectionPos.blockToSectionCoord(source.getY() + radius + 1), SectionPos.blockToSectionCoord(source.getZ() + radius + 1));
    }
    public static int light(BlockPos pos, int original) {
        Snapshot current = snapshot;
        if (current.sections.isEmpty() || WorldState.serverThread()) return original;
        var sources = current.sections.get(SectionPos.asLong(pos)); if (sources == null) return original;
        int brightness = LightCoordsUtil.block(original);
        for (BlockPos source : sources) {
            double distance = Math.sqrt(source.distSqr(pos));
            if (distance <= current.radius) brightness = Math.max(brightness, (int) Math.ceil(current.brightness * (1 - distance / (current.radius + 1.0))));
        }
        return brightness > LightCoordsUtil.block(original) ? LightCoordsUtil.withBlock(original, brightness) : original;
    }
}
