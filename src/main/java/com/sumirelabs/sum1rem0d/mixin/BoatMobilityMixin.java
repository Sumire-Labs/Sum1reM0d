package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.MobilityBoat;
import com.sumirelabs.sum1rem0d.game.WorldMobility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBoat.class)
public abstract class BoatMobilityMixin implements MobilityBoat {
    @Unique private boolean sum1rem0d$blueIceBoost;
    @Override public boolean sum1rem0d$blueIceBoost() { return sum1rem0d$blueIceBoost; }
    @Override public void sum1rem0d$blueIceBoost(boolean value) { sum1rem0d$blueIceBoost = value; }

    @Inject(method = "getGroundFriction", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$blueIceFriction(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(WorldMobility.boatGroundFriction((AbstractBoat) (Object) this, cir.getReturnValueF()));
    }

    @ModifyConstant(method = "controlBoat", constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 0.04F))
    private float sum1rem0d$configuredBoatAcceleration(float vanilla) { return WorldMobility.boatAcceleration((AbstractBoat) (Object) this, vanilla); }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sum1rem0d$clampSpeed(CallbackInfo ci) { WorldMobility.clampBoat((AbstractBoat) (Object) this); }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$saveBoost(ValueOutput output, CallbackInfo ci) { output.putBoolean("sum1rem0d_blue_ice_boost", sum1rem0d$blueIceBoost); }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$loadBoost(ValueInput input, CallbackInfo ci) { sum1rem0d$blueIceBoost = input.getBooleanOr("sum1rem0d_blue_ice_boost", false); }
}
