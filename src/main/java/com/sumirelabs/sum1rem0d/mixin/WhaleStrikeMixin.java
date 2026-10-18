package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WhaleEnchantmentRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WhaleStrikeMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void sum1rem0d$whaleSpout(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof Player attacker)
            WhaleEnchantmentRules.afterAttack(attacker, source, cir.getReturnValueZ());
    }
}
