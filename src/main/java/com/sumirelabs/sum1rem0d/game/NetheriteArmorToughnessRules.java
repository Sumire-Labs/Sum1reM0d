package com.sumirelabs.sum1rem0d.game;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Adds a transient, per-equipped-piece toughness bonus to vanilla netherite armor. */
public final class NetheriteArmorToughnessRules {
    private static final List<EquipmentSlot> ARMOR = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private NetheriteArmorToughnessRules() {}

    public static void tick(ServerPlayer player) {
        var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness == null) return;
        double bonus = Rules.on("netherite_upgrade")
                ? Math.max(0, Rules.n("netherite_upgrade", "armor_toughness_bonus")) : 0;
        for (EquipmentSlot slot : ARMOR) {
            var id = ModContent.id("netherite_toughness_" + slot.getName());
            ItemStack stack = player.getItemBySlot(slot);
            if (bonus > 0 && isMatchingArmor(slot, stack)) {
                var current = toughness.getModifier(id);
                if (current == null || current.amount() != bonus
                        || current.operation() != AttributeModifier.Operation.ADD_VALUE)
                    toughness.addOrUpdateTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE));
            } else toughness.removeModifier(id);
        }
    }

    private static boolean isMatchingArmor(EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getNamespace().equals("minecraft") || !key.getPath().startsWith("netherite_")) return false;
        return switch (slot) {
            case HEAD -> key.getPath().endsWith("_helmet");
            case CHEST -> key.getPath().endsWith("_chestplate");
            case LEGS -> key.getPath().endsWith("_leggings");
            case FEET -> key.getPath().endsWith("_boots");
            default -> false;
        };
    }
}
