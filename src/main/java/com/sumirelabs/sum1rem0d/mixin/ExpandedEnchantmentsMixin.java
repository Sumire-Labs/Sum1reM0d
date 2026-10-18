package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.DimensionCombatRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class ExpandedEnchantmentsMixin {
    @Inject(method = {"canEnchant", "isSupportedItem", "isPrimaryItem"}, at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$expandedTargets(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var enchantment = (Enchantment) (Object) this;
        if (enchantment.description().equals(net.minecraft.network.chat.Component.translatable("enchantment.sum1rem0d.fragility_curse")) && !com.sumirelabs.sum1rem0d.game.Rules.on("fragility_curse")) { cir.setReturnValue(false); return; }
        if (DimensionCombatRules.disabled(enchantment)) cir.setReturnValue(false);
        else if (DimensionCombatRules.allows(enchantment, stack)) cir.setReturnValue(true);
    }
}
