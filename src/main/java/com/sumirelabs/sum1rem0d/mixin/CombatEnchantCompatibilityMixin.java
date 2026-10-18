package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CombatEnchantRules;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the narrow config-controlled damage-enchantment pair and chestplate target. */
@Mixin(Enchantment.class)
public abstract class CombatEnchantCompatibilityMixin {
    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void sum1rem0d$compatibleDamageTypes(Holder<Enchantment> first, Holder<Enchantment> second,
            CallbackInfoReturnable<Boolean> callback) {
        if (CombatEnchantRules.damageEnchantmentsCompatible(first, second)) callback.setReturnValue(true);
    }

    @Inject(method = {"canEnchant", "isSupportedItem"}, at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$allowKnockbackOnChestplate(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (CombatEnchantRules.isKnockback((Enchantment) (Object) this) && CombatEnchantRules.supportsChestplate(stack))
            callback.setReturnValue(true);
    }

    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$makeChestplateEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (CombatEnchantRules.isKnockback((Enchantment) (Object) this) && CombatEnchantRules.supportsChestplate(stack))
            callback.setReturnValue(true);
    }
}
