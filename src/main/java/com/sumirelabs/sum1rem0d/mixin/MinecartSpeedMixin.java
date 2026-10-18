package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldMobility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OldMinecartBehavior.class, NewMinecartBehavior.class})
public abstract class MinecartSpeedMixin {
    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$configuredSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(WorldMobility.minecartCap(level, cir.getReturnValueD()));
    }
}
