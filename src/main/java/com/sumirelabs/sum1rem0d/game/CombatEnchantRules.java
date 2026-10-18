package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/** Narrow compatibility and defensive knockback rules for the requested enchantments. */
public final class CombatEnchantRules {
    private CombatEnchantRules() {}

    public static boolean damageEnchantmentsCompatible(Holder<Enchantment> first, Holder<Enchantment> second) {
        if (!Rules.on("compatible_damage_enchantments") || first.equals(second)) return false;
        return isDamageEnchantment(first) && isDamageEnchantment(second);
    }

    private static boolean isDamageEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.SHARPNESS)
                || enchantment.is(Enchantments.SMITE)
                || enchantment.is(Enchantments.BANE_OF_ARTHROPODS);
    }

    public static boolean isKnockback(Enchantment enchantment) {
        return enchantment.description().equals(Component.translatable("enchantment.minecraft.knockback"));
    }

    public static boolean supportsChestplate(ItemStack stack) {
        return Rules.on("chestplate_knockback") && stack.is(ItemTags.CHEST_ARMOR);
    }

    public static void afterDamage(LivingEntity victim, ServerLevel level, DamageSource source, float damage, boolean hit) {
        if (!hit || damage <= 0 || !Rules.on("chestplate_knockback")) return;
        ItemStack chestplate = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) return;
        var knockback = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
        int rank = EnchantmentHelper.getItemEnchantmentLevel(knockback, chestplate);
        if (rank <= 0) return;

        double chance = Math.clamp(Rules.n("chestplate_knockback", "chance"), 0, 100);
        if (chance <= 0 || victim.getRandom().nextFloat() * 100.0 >= chance) return;

        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        Entity target;
        if (direct instanceof Projectile projectile) {
            if (!Rules.b("chestplate_knockback", "affect_projectiles") || projectile.isRemoved()) return;
            target = projectile;
        } else {
            if (direct == null || direct != attacker || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)) return;
            if (attacker instanceof Mob mob && mob.isAlive()) target = mob;
            else if (attacker instanceof Player player && player.isAlive() && Rules.b("chestplate_knockback", "affect_players")) target = player;
            else return;
        }
        if (target == victim) return;

        double strength = Math.clamp(Rules.n("chestplate_knockback", "strength_per_level"), 0, 10) * rank;
        if (strength <= 0) return;
        Vec3 away = target.position().subtract(victim.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0e-8) away = new Vec3(victim.getRandom().nextDouble() - .5, 0, victim.getRandom().nextDouble() - .5);
        if (target instanceof LivingEntity living) {
            // LivingEntity.knockback subtracts the supplied source vector from motion.
            //? if >=26.2 {
            living.knockback(strength, -away.x, -away.z, source, damage);
            //?} else {
            /*living.knockback(strength, -away.x, -away.z);
            *///?}
        } else {
            Vec3 impulse = away.normalize().scale(strength);
            target.setDeltaMovement(target.getDeltaMovement().add(impulse.x, 0, impulse.z));
            //? if >=26.3 {
            target.syncPosition = true;
            //?} else {
            /*target.hurtMarked = true;
            *///?}
        }
    }
}
