package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.LifeBerryItem;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class LifeBerryRespawnMixin {
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void sum1rem0d$preserveLifeBerryHealth(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        LifeBerryItem.restoreAfterDeath((ServerPlayer) (Object) this, oldPlayer);
    }
}
