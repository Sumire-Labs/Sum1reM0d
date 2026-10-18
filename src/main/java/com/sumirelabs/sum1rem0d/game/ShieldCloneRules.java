package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

/** Bounded clone-on-shield-block rule. All spawning stays server-side. */
public final class ShieldCloneRules {
    private static final String DEPTH_PREFIX = "sum1rem0d_shield_clone_depth_";
    private ShieldCloneRules() {}

    public static void blocked(Entity defender, Entity sourceEntity, net.minecraft.world.damagesource.DamageSource source) {
        if (!Rules.on("shield_mob_clone") || !(defender instanceof ServerPlayer player)
                || !(sourceEntity instanceof Mob attacker) || attacker.isRemoved()) return;
        int depth = depth(attacker);
        if (depth >= Rules.i("shield_mob_clone", "chain_depth")) return;
        var level = (ServerLevel) player.level();
        int count = Math.max(0, Math.min(8, Rules.i("shield_mob_clone", "clone_limit_per_hit")));
        for (int i = 0; i < count; i++) {
            Mob clone = (Mob) attacker.getType().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (clone == null) continue;
            clone.snapTo(attacker.getX() + (i + 1) * 0.8, attacker.getY(), attacker.getZ() + 0.8, attacker.getYRot(), attacker.getXRot());
            clone.addTag(DEPTH_PREFIX + (depth + 1));
            if (!level.addFreshEntity(clone)) clone.discard();
        }
    }

    private static int depth(Entity entity) {
        for (String tag : entity.entityTags()) if (tag.startsWith(DEPTH_PREFIX)) {
            try { return Integer.parseInt(tag.substring(DEPTH_PREFIX.length())); }
            catch (NumberFormatException ignored) { return Integer.MAX_VALUE; }
        }
        return 0;
    }
}
