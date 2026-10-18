package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentMenu.class)
public abstract class ElytraEnchantingMixin {
    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void sum1rem0d$elytraEnchantability(Container inventory, CallbackInfo ci) {
        var stack = inventory.getItem(0);
        if (stack.is(Items.ELYTRA) && Rules.on("elytra_enchanting"))
            stack.set(DataComponents.ENCHANTABLE, new Enchantable(Rules.i("elytra_enchanting", "enchantability")));
    }
}
