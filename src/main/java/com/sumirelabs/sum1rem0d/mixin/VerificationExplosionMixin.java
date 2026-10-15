package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ServerVerification;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records actual native explosions only in opt-in development verification. */
@Mixin(ServerExplosion.class)
public abstract class VerificationExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"))
    private void sum1rem0d$explosion(CallbackInfoReturnable<Integer> ci) {
        if (Boolean.getBoolean("sum1rem0d.serverVerify")) ServerVerification.exploded((ServerExplosion) (Object) this);
    }
}
