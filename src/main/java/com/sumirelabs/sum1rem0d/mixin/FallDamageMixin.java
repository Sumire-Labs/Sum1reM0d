package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class FallDamageMixin {
    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sum1rem0d$fallDistance(double distance) {
        if ((Object) this instanceof Player && Rules.on("early_fall")) {
            // Vanilla floors (distance - safe distance); shift the first positive
            // point from 4m to the configured height, preserving vanilla modifiers.
            return distance + 4.0 - Rules.i("early_fall", "start_height");
        }
        return distance;
    }
}
