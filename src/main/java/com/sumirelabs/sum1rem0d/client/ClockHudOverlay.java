package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Items;

/** Displays the exact vanilla daytime counter while a clock is held. */
public final class ClockHudOverlay {
    private ClockHudOverlay() {}
    public static void render(GuiGraphicsExtractor graphics) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()
                || !PersonalConfig.store().enabled("clock_ticks")) return;
        if (!mc.player.getMainHandItem().is(Items.CLOCK) && !mc.player.getOffhandItem().is(Items.CLOCK)) return;
        long ticks = Math.floorMod(mc.level.getOverworldClockTime(), 24_000L);
        String label = "Daytime tick: " + ticks;
        int x = 8, y = 8, width = mc.font.width(label) + 8;
        graphics.fill(x, y, x + width, y + 15, 0xA010141B);
        graphics.text(mc.font, label, x + 4, y + 3, 0xFFFFFFFF, false);
    }
}
