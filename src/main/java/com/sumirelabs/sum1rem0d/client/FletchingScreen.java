package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.FletchingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private Button craft;
    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 202); }
    @Override protected void init() {
        super.init();
        craft = addRenderableWidget(Button.builder(Component.literal("作成"), button -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }).bounds(leftPos + 113, topPos + 16, 55, 20).build());
    }
    @Override protected void containerTick() { super.containerTick(); craft.active = menu.canCraft(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        graphics.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF333333);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF373737);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B8B8B);
        }
        graphics.text(font, "材料", leftPos + 45, topPos + 22, 0xFF333333, false);
        graphics.text(font, "完成品", leftPos + 8, topPos + 38, 0xFF333333, false);
    }
}
