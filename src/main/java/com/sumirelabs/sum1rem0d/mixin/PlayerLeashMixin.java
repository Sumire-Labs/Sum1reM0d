package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerLeashMixin implements Leashable {
    @Unique private Leashable.LeashData sum1rem0d$leashData;
    @Override public Leashable.LeashData getLeashData() { return sum1rem0d$leashData; }
    @Override public void setLeashData(Leashable.LeashData value) { sum1rem0d$leashData = value; }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$savePlayerLeash(ValueOutput output, CallbackInfo ci) {
        ((Leashable) this).writeLeashData(output, sum1rem0d$leashData);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$loadPlayerLeash(ValueInput input, CallbackInfo ci) {
        ((Leashable) this).readLeashData(input);
    }
}
