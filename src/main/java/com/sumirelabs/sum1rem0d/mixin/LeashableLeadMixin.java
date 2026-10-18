package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldMobility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tracks server-player holders so disconnect/Config OFF can release their entity leashes safely. */
@Mixin(Leashable.class)
public interface LeashableLeadMixin {
    @Inject(method = "setLeashedTo", at = @At("TAIL"))
    private void sum1rem0d$trackPlayerHolder(Entity holder, boolean sendPacket, CallbackInfo ci) {
        WorldMobility.recordLeash((Entity) this, holder);
    }
}
