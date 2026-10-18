package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
public final class FragilityCurse {
    private FragilityCurse() {}
    public static int apply(ItemStack stack, ServerLevel level, int wear) {
        if (wear <= 0 || !Rules.on("fragility_curse")) return wear;
        var curse = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("fragility_curse")));
        if (EnchantmentHelper.getItemEnchantmentLevel(curse, stack) == 0) return wear;
        return (int) Math.min((long) Integer.MAX_VALUE - stack.getDamageValue(), (long) wear * Rules.i("fragility_curse", "durability_multiplier"));
    }
}
