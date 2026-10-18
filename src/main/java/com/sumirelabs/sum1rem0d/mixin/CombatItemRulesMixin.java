package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CombatItemRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class CombatItemRulesMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$combatDurability(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(CombatItemRules.maxDamage((ItemStack) (Object) this, cir.getReturnValueI()));
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$woodToolMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(CombatItemRules.destroySpeed((ItemStack) (Object) this, state, cir.getReturnValueF()));
    }
}
