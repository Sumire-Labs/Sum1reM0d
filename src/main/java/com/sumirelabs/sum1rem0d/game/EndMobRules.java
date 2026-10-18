package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class EndMobRules {
    private EndMobRules() {}
    public static void tick(Mob mob) {
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).equals(net.minecraft.resources.Identifier.withDefaultNamespace("enderman")) || !(mob.level() instanceof ServerLevel level) || !Rules.on("enderman_laser")) return;
        var target = mob.getTarget();
        if (target == null || !target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator())
            || mob.distanceToSqr(target) > Math.pow(Rules.n("enderman_laser", "range"), 2) || !mob.hasLineOfSight(target)) return;
        int interval = Rules.i("enderman_laser", "interval_ticks");
        int phase = Math.floorMod(mob.tickCount, interval);
        int charge = Math.min(interval - 1, Rules.i("enderman_laser", "charge_ticks"));
        if (phase != 0 && (phase < interval - charge || phase % 2 != 0)) return;
        var start = mob.getEyePosition();
        var end = target.getEyePosition();
        var delta = end.subtract(start);
        var right = new Vec3(-delta.z, 0, delta.x).normalize().scale(.13);
        var dust = new DustParticleOptions(0xBD39FF, phase == 0 ? 1.6f : .8f);
        int steps = Math.clamp((int) (delta.length() / .3), 1, 120);
        for (int eye : new int[]{-1, 1}) {
            Vec3 origin = start.add(right.scale(eye));
            for (int i = 0; i <= steps; i++) {
                var point = origin.lerp(end, (double) i / steps);
                level.sendParticles(dust, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
        if (phase == 0) target.hurtServer(level, mob.damageSources().mobAttack(mob), (float) Rules.n("enderman_laser", "damage"));
    }
}
