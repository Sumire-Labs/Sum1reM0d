package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Refresh existing circuits on activation and Config changes without scanning the world every tick. */
public final class RedstoneRefresh {
    private static final Map<ServerLevel, Sources> LEVELS = new IdentityHashMap<>();
    private record Settings(long revision, JsonObject values) {}
    private static final Map<MinecraftServer, Settings> SETTINGS = new IdentityHashMap<>();
    private static final class Sources {
        final Map<ChunkPos, Set<BlockPos>> chunks = new HashMap<>();
        final LinkedHashSet<BlockPos> pending = new LinkedHashSet<>();
    }
    private RedstoneRefresh() {}
    private static boolean relevant(BlockState state) { return state.is(Blocks.REDSTONE_WIRE) || state.isSignalSource(); }
    public static void activated(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) { level.getServer().execute(() -> activated(level, chunk)); return; }
        var data = LEVELS.computeIfAbsent(level, ignored -> new Sources()); var positions = new HashSet<BlockPos>();
        var sections = chunk.getSections(); var chunkPos = chunk.getPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex]; if (!section.maybeHas(RedstoneRefresh::relevant)) continue;
            int baseY = level.getMinY() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                if (relevant(section.getBlockState(x, y, z))) positions.add(new BlockPos(chunkPos.getMinBlockX() + x, baseY + y, chunkPos.getMinBlockZ() + z));
        }
        var old = data.chunks.remove(chunkPos); if (old != null) data.pending.removeAll(old);
        if (!positions.isEmpty()) { data.chunks.put(chunkPos, positions); data.pending.addAll(positions); }
    }
    public static void changed(ServerLevel level, BlockPos position, BlockState old, BlockState next) {
        if (!relevant(old) && !relevant(next)) return; var pos = position.immutable();
        if (!level.getServer().isSameThread()) { level.getServer().execute(() -> changed(level, pos, old, next)); return; }
        if (!level.hasChunkAt(pos)) return;
        var data = LEVELS.computeIfAbsent(level, ignored -> new Sources()); var chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        if (relevant(next)) {
            data.chunks.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(pos);
            if (old.getBlock() != next.getBlock()) data.pending.add(pos);
        } else {
            var sources = data.chunks.get(chunk); if (sources != null) { sources.remove(pos); if (sources.isEmpty()) data.chunks.remove(chunk); }
            data.pending.remove(pos);
        }
    }
    public static void unloaded(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) { level.getServer().execute(() -> unloaded(level, chunk)); return; }
        var data = LEVELS.get(level); if (data == null) return;
        var old = data.chunks.remove(chunk.getPos()); if (old != null) data.pending.removeAll(old);
    }
    public static void close(MinecraftServer server) { LEVELS.keySet().removeIf(level -> level.getServer() == server); SETTINGS.remove(server); }
    public static void tick(MinecraftServer server) {
        if (!Sum1reM0d.initialized()) return;
        var store = Sum1reM0d.config(); var previous = SETTINGS.get(server); long revision = store.revision();
        if (previous == null || previous.revision != revision) {
            var features = store.snapshot().getAsJsonObject("features"); var settings = new JsonObject();
            for (String feature : List.of("redstone_inversion", "comparator_division")) settings.add(feature, features.get(feature));
            SETTINGS.put(server, new Settings(revision, settings));
            if (previous == null || !settings.equals(previous.values)) for (var entry : LEVELS.entrySet()) if (entry.getKey().getServer() == server) entry.getValue().chunks.values().forEach(entry.getValue().pending::addAll);
        }
        int budget = 128;
        for (var entry : new ArrayList<>(LEVELS.entrySet())) {
            var level = entry.getKey(); if (level.getServer() != server) continue; var data = entry.getValue();
            while (budget > 0 && !data.pending.isEmpty()) {
                var pos = data.pending.iterator().next(); data.pending.remove(pos); budget--;
                if (!level.hasChunkAt(pos)) continue; var state = level.getBlockState(pos); if (!relevant(state)) continue;
                if (state.is(Blocks.REDSTONE_WIRE) && RedstoneGameplay.part("wire")) data.pending.removeAll(RedstoneGameplay.wire(level, pos));
                else level.neighborChanged(pos, state.getBlock(), null);
                level.updateNeighborsAt(pos, state.getBlock());
                for (Direction side : Direction.values()) level.updateNeighborsAt(pos.relative(side), state.getBlock());
            }
        }
    }
}
