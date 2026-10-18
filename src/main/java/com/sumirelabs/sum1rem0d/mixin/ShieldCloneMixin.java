package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ShieldCloneRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ShieldCloneMixin {
    @Inject(method = "blockUsingItem", at = @At("HEAD"), require = 0)
    //? if >=26.3 {
    private void sum1rem0d$cloneBlockedAttacker(ServerLevel level, LivingEntity attacker,
            net.minecraft.world.damagesource.DamageSource source, float damage, boolean fullyBlocked, CallbackInfo ci) {
        ShieldCloneRules.blocked((LivingEntity) (Object) this, attacker, source);
    }
    //?} else {
    //? if >=26.2 {
    /*private void sum1rem0d$cloneBlockedAttacker(ServerLevel level, LivingEntity attacker,
            net.minecraft.world.damagesource.DamageSource source, float damage, CallbackInfo ci) {
        ShieldCloneRules.blocked((LivingEntity) (Object) this, attacker, source);
    }*///?} else {
    /*private void sum1rem0d$cloneBlockedAttacker(ServerLevel level, LivingEntity attacker, CallbackInfo ci) {
        ShieldCloneRules.blocked((LivingEntity) (Object) this, attacker, null);
    }*///?}
    //?}
}
