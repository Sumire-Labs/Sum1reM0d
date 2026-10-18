package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import com.sumirelabs.sum1rem0d.game.SneakSpeedContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class SneakSpeedEnchantmentMixin {
    @Inject(method = {"canEnchant", "isSupportedItem"}, at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$allowSwiftSneakBoots(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (SneakSpeedContent.isSwiftSneak((Enchantment) (Object) this) && Rules.on("sneak_speed")
                && Rules.b("sneak_speed", "boots_enabled") && SneakSpeedContent.isBoot(stack)) cir.setReturnValue(true);
    }

    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$makeSwiftSneakEnchantableOnBoots(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!SneakSpeedContent.isSwiftSneak((Enchantment) (Object) this)) return;
        if (!Rules.on("sneak_speed") || !Rules.b("sneak_speed", "table_enabled")) {
            cir.setReturnValue(false);
            return;
        }
        if (Rules.b("sneak_speed", "boots_enabled") && SneakSpeedContent.isBoot(stack)) cir.setReturnValue(true);
    }
}
