package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class SleepEnoughMixin {
    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$enough(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && Gameplay.timedSleep(player)) cir.setReturnValue(Gameplay.sleepingLongEnough(player));
    }
}
