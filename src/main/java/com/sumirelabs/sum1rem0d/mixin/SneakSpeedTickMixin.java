package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.SneakSpeedContent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SneakSpeedTickMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void sum1rem0d$bootsSneakSpeed(CallbackInfo ci) {
        if ((Object) this instanceof Player player) SneakSpeedContent.tick(player);
    }
}
