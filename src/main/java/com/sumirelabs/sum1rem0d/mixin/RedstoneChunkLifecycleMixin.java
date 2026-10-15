package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.RedstoneRefresh;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class RedstoneChunkLifecycleMixin {
    @Inject(method = "startTickingChunk", at = @At("TAIL"))
    private void sum1rem0d$activate(LevelChunk chunk, CallbackInfo ci) { RedstoneRefresh.activated((ServerLevel) (Object) this, chunk); }
    @Inject(method = "unload", at = @At("HEAD"))
    private void sum1rem0d$unload(LevelChunk chunk, CallbackInfo ci) { RedstoneRefresh.unloaded((ServerLevel) (Object) this, chunk); }
}
