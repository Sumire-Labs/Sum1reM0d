package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientGameState;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess;
import java.util.ArrayList;
import java.util.List;

/** Compact live fuel summary for vanilla furnaces and the mod's instant smelting mode. */
public final class FurnaceHudOverlay {
    private FurnaceHudOverlay() {}

    public static void render(Screen screen, GuiGraphicsExtractor graphics) {
        if (!PersonalConfig.store().enabled("furnace_info") || !(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) return;
        var access = (ContainerScreenAccess) screen;
        if (!(access.sum1rem0d$menu() instanceof AbstractFurnaceMenu menu) || menu.slots.size() < 3) return;

        double efficiency = Math.max(0.01, ClientGameState.number("fuelEfficiency", 1));
        int fuelCount = menu.slots.get(1).getItem().getCount();
        List<String> lines = new ArrayList<>();
        lines.add("燃料残量: " + fuelCount + " 個");
        if (ClientGameState.daily("daily_fuel")) {
            lines.add(String.format(java.util.Locale.ROOT, "今日の燃料効率: %.0f%%", efficiency * 100));
            lines.add(String.format(java.util.Locale.ROOT, "消費倍率: %.2f倍", 1.0 / efficiency));
        }
        if (ClientRules.enabled("instant_smelting")) {
            double baseCost = Math.max(0, ClientRules.number("instant_smelting", "base_cost"));
            lines.add(String.format(java.util.Locale.ROOT, "即時精錬の目安: %.2f燃料/個", baseCost / efficiency));
            ItemStack input = menu.slots.get(0).getItem();
            if (!input.isEmpty()) lines.add("入力: " + input.getCount() + " 個");
        }
        if (lines.isEmpty()) return;

        int left = access.sum1rem0d$leftPos(), top = access.sum1rem0d$topPos(), imageWidth = access.sum1rem0d$imageWidth();
        int x = left + imageWidth + 5;
        if (x + 120 > graphics.guiWidth()) x = Math.max(4, left - 124);
        int y = top + 5, width = Math.min(150, graphics.guiWidth() - x - 4), height = lines.size() * 11 + 6;
        if (width < 70 || y + height > graphics.guiHeight()) return;
        graphics.fill(x - 3, y - 3, x + width + 3, y + height, 0xC810141B);
        for (int i = 0; i < lines.size(); i++) {
            String clipped = net.minecraft.client.Minecraft.getInstance().font.plainSubstrByWidth(lines.get(i), width);
            graphics.text(net.minecraft.client.Minecraft.getInstance().font, clipped, x, y + i * 11, 0xFFE9F1FF, false);
        }
    }
}
