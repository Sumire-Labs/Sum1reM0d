package com.sumirelabs.sum1rem0d.game;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Dimension slayers use explicit species, rather than the victim's current dimension. */
public final class DimensionCombatRules {
    private static final Set<String> NETHER = Set.of("blaze", "wither_skeleton", "hoglin", "zoglin", "piglin", "zombified_piglin", "strider", "ghast");
    private static final Set<String> END = Set.of("enderman", "ender_dragon", "shulker", "endermite");
    private static final Set<String> DAMAGE = Set.of("sharpness", "smite", "bane_of_arthropods", "impaling", "density", "breach", "nether_slayer", "end_slayer");
    private DimensionCombatRules() {}

    public static String name(Enchantment enchantment) {
        for (String id : DAMAGE) {
            String namespace = id.endsWith("_slayer") ? "sum1rem0d" : "minecraft";
            if (enchantment.description().equals(Component.translatable("enchantment." + namespace + "." + id))) return id;
        }
        return "";
    }

    public static boolean allows(Enchantment enchantment, ItemStack stack) {
        String name = name(enchantment);
        if (name.endsWith("_slayer") && !Rules.on(name)) return false;
        if (Rules.on("expanded_weapon_enchantments") && (stack.is(Items.MACE) || stack.is(Items.TRIDENT)) && DAMAGE.contains(name)) return true;
        return Rules.on("elytra_enchanting") && stack.is(Items.ELYTRA)
            && (enchantment.description().equals(Component.translatable("enchantment.minecraft.unbreaking"))
                || enchantment.description().equals(Component.translatable("enchantment.minecraft.protection")));
    }

    public static boolean disabled(Enchantment enchantment) {
        String id = name(enchantment);
        return id.endsWith("_slayer") && !Rules.on(id);
    }

    public static float damage(LivingEntity victim, ServerLevel level, DamageSource source, float original) {
        if (original <= 0 || !(source.getEntity() instanceof LivingEntity)) return original;
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return original;
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (!type.getNamespace().equals("minecraft")) return original;
        String feature = NETHER.contains(type.getPath()) ? "nether_slayer" : END.contains(type.getPath()) ? "end_slayer" : "";
        if (feature.isEmpty() || !Rules.on(feature)) return original;
        var holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id(feature)));
        return original + EnchantmentHelper.getItemEnchantmentLevel(holder, weapon) * (float) Rules.n(feature, "damage_per_level");
    }
}
