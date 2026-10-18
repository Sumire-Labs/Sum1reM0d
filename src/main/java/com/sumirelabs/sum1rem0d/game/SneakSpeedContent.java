package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Extends vanilla Swift Sneak to boots and adds the configured step-height bonus. */
public final class SneakSpeedContent {
    private static final net.minecraft.resources.Identifier SNEAK = ModContent.id("boot_sneak_speed");
    private static final net.minecraft.resources.Identifier STEP = ModContent.id("boot_sneak_step");
    private SneakSpeedContent() {}

    public static boolean isSwiftSneak(net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment.description().equals(net.minecraft.network.chat.Component.translatable("enchantment.minecraft.swift_sneak"));
    }

    public static boolean isBoot(ItemStack stack) { return stack.is(net.minecraft.tags.ItemTags.FOOT_ARMOR); }

    public static void tick(LivingEntity entity) {
        var speed = entity.getAttribute(Attributes.SNEAKING_SPEED);
        var step = entity.getAttribute(Attributes.STEP_HEIGHT);
        if (speed == null || step == null) return;
        int level = 0;
        if (Rules.on("sneak_speed") && Rules.b("sneak_speed", "boots_enabled") && isBoot(entity.getItemBySlot(EquipmentSlot.FEET))) {
            var enchantment = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SWIFT_SNEAK);
            if (enchantment.isPresent()) level = EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), entity.getItemBySlot(EquipmentSlot.FEET));
        }
        if (level > 0) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SNEAK,
                    level * Rules.n("sneak_speed", "per_level_speed_bonus"), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            step.addOrUpdateTransientModifier(new AttributeModifier(STEP,
                    level * Rules.i("sneak_speed", "per_level_step_height"), AttributeModifier.Operation.ADD_VALUE));
        } else {
            speed.removeModifier(SNEAK);
            step.removeModifier(STEP);
        }
    }
}
