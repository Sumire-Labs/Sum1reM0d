package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ServerVerification;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No work in normal sessions; captures real entities added during native verification. */
@Mixin(ServerLevel.class)
public abstract class VerificationSpawnMixin {
    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void sum1rem0d$spawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.getBoolean("sum1rem0d.serverVerify") && cir.getReturnValueZ()) ServerVerification.spawned(entity);
    }
}
