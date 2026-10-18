package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldMobility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class BoatStepMixin {
    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$boatStep(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof AbstractBoat boat) cir.setReturnValue(WorldMobility.boatStep(boat, cir.getReturnValueF()));
    }
}
