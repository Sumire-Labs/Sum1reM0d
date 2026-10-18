package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Displays the exact vanilla daytime counter while a clock is held. */
public final class ClockHudOverlay {
    private ClockHudOverlay() {}
    private static long lastMessageTick = Long.MIN_VALUE;
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !PersonalConfig.store().enabled("clock_ticks")) {
            lastMessageTick = Long.MIN_VALUE;
            return;
        }
        if (!mc.player.getMainHandItem().is(Items.CLOCK) && !mc.player.getOffhandItem().is(Items.CLOCK)) return;
        long gameTick = mc.level.getGameTime();
        if (gameTick % 20 != 0 || gameTick == lastMessageTick) return;
        long ticks = Math.floorMod(mc.level.getOverworldClockTime(), 24_000L);
        mc.player.sendOverlayMessage(Component.translatable("gui.sum1rem0d.clock_ticks", ticks));
        lastMessageTick = gameTick;
    }
}
