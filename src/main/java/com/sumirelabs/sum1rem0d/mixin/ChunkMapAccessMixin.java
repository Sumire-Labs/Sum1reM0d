package com.sumirelabs.sum1rem0d.mixin;

import java.util.function.Consumer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses the loaded ticking-chunk iterator across Minecraft versions where it is package-private. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessMixin {
    @Invoker("forEachBlockTickingChunk")
    void sum1rem0d$forEachBlockTickingChunk(Consumer<LevelChunk> consumer);
}
