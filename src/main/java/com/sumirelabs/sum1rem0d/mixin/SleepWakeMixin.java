package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class SleepWakeMixin {
    @Inject(method = "startSleeping", at = @At("RETURN"))
    //? if >=26.3 {
    private void sum1rem0d$start(net.minecraft.core.BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !com.sumirelabs.sum1rem0d.Sum1reM0d.initialized()) return;
    //?} else {
    /*private void sum1rem0d$start(net.minecraft.core.BlockPos pos, CallbackInfo ci) {
        if (!com.sumirelabs.sum1rem0d.Sum1reM0d.initialized()) return;
    *///?}
        var player = (ServerPlayer) (Object) this;
        var data = com.sumirelabs.sum1rem0d.game.WorldState.get(player.level().getServer()).player(player);
        data.sleepTicks = 0; data.sleepAwarded = false;
    }
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void sum1rem0d$wake(boolean forceful, boolean updateLevel, CallbackInfo ci) { Gameplay.completeRest((ServerPlayer) (Object) this); }
}
