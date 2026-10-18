package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Short lived, bounded attraction fields created by wind charges. */
public final class WindChargeBlackHole {
    private record Field(UUID projectile, UUID owner, Vec3 center, long expiresAt) {}
    private static final Map<ServerLevel, List<Field>> FIELDS = new WeakHashMap<>();
    private static final String USED_TAG = "sum1rem0d_black_hole_used";
    private WindChargeBlackHole() {}

    public static void impact(Projectile projectile) {
        if (!Rules.on("wind_black_hole") || !(projectile.level() instanceof ServerLevel level)) return;
        UUID id = projectile.getUUID();
        if (!projectile.addTag(USED_TAG)) return;
        Entity owner = projectile.getOwner();
        long life = Math.max(1, Math.min(1200, Rules.i("wind_black_hole", "duration_ticks")));
        FIELDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Field(id, owner == null ? null : owner.getUUID(), projectile.position(), level.getGameTime() + life));
    }

    public static void tick(ServerLevel level) {
        var fields = FIELDS.get(level);
        if (fields == null || fields.isEmpty()) return;
        if (!Rules.on("wind_black_hole")) { FIELDS.remove(level); return; }
        long now = level.getGameTime();
        fields.removeIf(field -> {
            if (now >= field.expiresAt()) return true;
            pull(level, field);
            return false;
        });
        if (fields.isEmpty()) FIELDS.remove(level);
    }

    private static void pull(ServerLevel level, Field field) {
        double radius = Math.max(1, Math.min(64, Rules.n("wind_black_hole", "radius")));
        var box = new AABB(field.center(), field.center()).inflate(radius);
        int limit = Math.max(1, Math.min(256, Rules.i("wind_black_hole", "max_targets")));
        int moved = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> !(e instanceof Player) && e.isAlive())) {
            if (moved >= limit) break;
            if (field.owner() != null && target.getUUID().equals(field.owner())) continue;
            Vec3 toward = field.center().subtract(target.position());
            double distance = toward.length();
            if (distance < 0.25 || distance > radius) continue;
            double scale = Math.min(2.5, Rules.n("wind_black_hole", "pull_strength") / Math.max(0.5, distance));
            target.setDeltaMovement(target.getDeltaMovement().add(toward.normalize().scale(scale)));
            //? if >=26.3 {
            target.syncPosition = true;
            //?} else {
            /*target.hurtMarked = true;
            *///?}
            moved++;
        }
        if (level.getGameTime() % 4 == 0) level.sendParticles(ParticleTypes.PORTAL, field.center().x, field.center().y, field.center().z, 6, 0.25, 0.25, 0.25, 0.03);
    }
}
