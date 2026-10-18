package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.game.CrossbowVolley;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** A particle-rendered rotating rune ring; it creates no projectile entities. */
public final class CrossbowMagicCircle {
    private static int ticks;
    private CrossbowMagicCircle() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientRules.enabled("crossbow_circle") || mc.level.getGameTime() % 3 != 0) return;
        for (var player : mc.level.players()) {
            var hand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!hand.is(Items.CROSSBOW)) hand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!hand.is(Items.CROSSBOW) || CrossbowVolley.queuedCount(mc.level, hand) == 0) continue;
            double yaw = Math.toRadians(player.getYRot());
            double backX = Math.sin(yaw), backZ = -Math.cos(yaw);
            double sideX = Math.cos(yaw), sideZ = Math.sin(yaw);
            double phase = ticks * 0.08;
            for (int i = 0; i < 16; i++) {
                double angle = phase + (Math.PI * 2 * i / 16.0);
                double radius = 0.56;
                double x = player.getX() + backX * 0.38 + sideX * Math.cos(angle) * radius;
                double y = player.getY() + 1.0 + Math.sin(angle) * radius;
                double z = player.getZ() + backZ * 0.38 + sideZ * Math.cos(angle) * radius;
                mc.level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.012, 0);
            }
        }
        ticks++;
    }
}
