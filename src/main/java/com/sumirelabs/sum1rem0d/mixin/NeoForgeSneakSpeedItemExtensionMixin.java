package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import com.sumirelabs.sum1rem0d.game.CombatEnchantRules;
import com.sumirelabs.sum1rem0d.game.SneakSpeedContent;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge's enchanting helper asks IItemExtension directly instead of Enchantment.isPrimaryItem. */
@Pseudo
@Mixin(targets = "net.neoforged.neoforge.common.extensions.IItemExtension", remap = false)
public interface NeoForgeSneakSpeedItemExtensionMixin {
    @Inject(method = "isPrimaryItemFor", at = @At("HEAD"), cancellable = true, remap = false)
    default void sum1rem0d$gateSwiftSneakTableCandidate(ItemStack stack, Holder<Enchantment> enchantment,
            CallbackInfoReturnable<Boolean> cir) {
        if (!SneakSpeedContent.isSwiftSneak(enchantment.value())) return;
        if (!Rules.on("sneak_speed") || !Rules.b("sneak_speed", "table_enabled")) {
            cir.setReturnValue(false);
            return;
        }
        if (Rules.b("sneak_speed", "boots_enabled") && SneakSpeedContent.isBoot(stack)) cir.setReturnValue(true);
    }

    @Inject(method = "isPrimaryItemFor", at = @At("HEAD"), cancellable = true, remap = false)
    default void sum1rem0d$allowKnockbackChestplate(ItemStack stack, Holder<Enchantment> enchantment,
            CallbackInfoReturnable<Boolean> cir) {
        if (CombatEnchantRules.isKnockback(enchantment.value()) && CombatEnchantRules.supportsChestplate(stack)) {
            cir.setReturnValue(true);
        }
    }
}
