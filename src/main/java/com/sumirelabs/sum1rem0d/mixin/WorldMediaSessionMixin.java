package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldMediaRules;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep media event counters monotonic when respawn replaces the server-side player. */
@Mixin(ServerPlayer.class)
public abstract class WorldMediaSessionMixin {
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void sum1rem0d$copyMediaSession(ServerPlayer previous, boolean restoreAll, CallbackInfo ci) {
        WorldMediaRules.copySession(previous, (ServerPlayer) (Object) this);
    }
}
