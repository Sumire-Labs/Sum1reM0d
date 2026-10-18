package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Item level rules for the configurable combat rebalance. Called by CombatItemRulesMixin. */
public final class CombatItemRules {
    private CombatItemRules() {}

    public static boolean isShelf(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = key.toString();
        var configured = Rules.list("shelf_weapon", "target_item_ids");
        return configured.isEmpty()
                ? key.getNamespace().equals("minecraft") && key.getPath().equals("poplar_shelf")
                : configured.contains(id);
    }

    public static int maxDamage(ItemStack stack, int vanilla) {
        if (vanilla <= 0) return vanilla;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean paxel = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("sum1rem0d") && PaxelContent.ITEMS.contains(path);
        if (Rules.on("gold_equipment") && (id.startsWith("minecraft:golden_") || id.startsWith("minecraft:gold_") || paxel && path.equals("golden_paxel")))
            return scaled(vanilla, Rules.i("gold_equipment", "durability_multiplier"));
        if (Rules.on("chainmail_equipment") && path.startsWith("chainmail_"))
            return scaled(vanilla, Rules.i("chainmail_equipment", "durability_multiplier"));
        if (Rules.on("netherite_upgrade") && (id.startsWith("minecraft:netherite_") || paxel && path.equals("netherite_paxel")))
            return scaled(vanilla, Rules.n("netherite_upgrade", "durability_multiplier"));
        return vanilla;
    }

    public static float destroySpeed(ItemStack stack, BlockState state, float vanilla) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean woodenPaxel = id.equals("sum1rem0d:wooden_paxel");
        if (Rules.on("wooden_nerf") && (id.startsWith("minecraft:wooden_") || woodenPaxel))
            return vanilla * (float) Rules.n("wooden_nerf", "mining_speed_multiplier");
        return vanilla;
    }

    /** Adjusts direct player melee damage before armor and resistance are applied. */
    public static float incomingDamage(LivingEntity victim, DamageSource source, float damage) {
        if (!(source.getEntity() instanceof Player attacker) || source.getDirectEntity() != attacker) return damage;
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) weapon = attacker.getOffhandItem();
        String id = BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        String path = BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath();
        boolean paxel = BuiltInRegistries.ITEM.getKey(weapon.getItem()).getNamespace().equals("sum1rem0d") && PaxelContent.ITEMS.contains(path);
        damage = WhaleEnchantmentRules.consumeBonus(attacker, damage);
        if (Rules.on("shelf_weapon") && isShelf(weapon))
            return (float) Rules.n("shelf_weapon", "damage");
        if (!hasMeleeDamage(weapon)) return damage;
        if (Rules.on("wooden_nerf") && (id.startsWith("minecraft:wooden_") || paxel && path.equals("wooden_paxel")))
            return damage * (float) Rules.n("wooden_nerf", "attack_multiplier");
        if (Rules.on("netherite_upgrade") && (id.startsWith("minecraft:netherite_") || paxel && path.equals("netherite_paxel")))
            return damage + (float) Rules.n("netherite_upgrade", "attack_bonus");
        return damage;
    }

    private static boolean hasMeleeDamage(ItemStack weapon) {
        if (weapon.isEmpty()) return false;
        var modifiers = weapon.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        return modifiers != null && modifiers.modifiers().stream().anyMatch(entry ->
                entry.attribute().value().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.value()));
    }

    public static float shelfGuard(LivingEntity victim, DamageSource source, float damage) {
        if (!Rules.on("shelf_weapon") || !Rules.b("shelf_weapon", "guard_enabled") || damage <= 0
                || !victim.isUsingItem() || !isShelf(victim.getUseItem()) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) return damage;
        var origin = source.getSourcePosition();
        if (origin == null) return damage;
        var toward = origin.subtract(victim.position()).multiply(1, 0, 1).normalize();
        var facing = net.minecraft.world.phys.Vec3.directionFromRotation(0, victim.getYRot());
        if (toward.lengthSqr() == 0 || facing.dot(toward) < 0.25) return damage;
        double reduction = Math.max(0, Math.min(1, Rules.n("shelf_weapon", "guard_damage_reduction")));
        return damage * (float) (1 - reduction);
    }

    private static int scaled(int value, int multiplier) {
        return scaled(value, (double) Math.max(1, multiplier));
    }

    private static int scaled(int value, double multiplier) {
        double result = value * Math.max(1, multiplier);
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(result));
    }
}
