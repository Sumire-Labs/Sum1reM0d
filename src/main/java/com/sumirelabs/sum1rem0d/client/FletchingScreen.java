package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.FletchingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private Button craft;
    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 208); }
    @Override protected void init() {
        super.init();
        craft = addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.fletching.craft"), button -> { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); }).bounds(leftPos + 111, topPos + 20, 57, 20).build());
        craft.active = menu.canCraft();
    }
    @Override protected void containerTick() { super.containerTick(); craft.active = menu.canCraft(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        super.extractBackground(graphics, x, y, tick);
        VanillaGuiParts.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) VanillaGuiParts.slot(graphics, leftPos + slot.x, topPos + slot.y);
        VanillaGuiParts.arrow(graphics, leftPos + 78, topPos + 23);
        graphics.text(font, Component.translatable("gui.sum1rem0d.fletching.input"), leftPos + 8, topPos + 26, 0xFF404040, false);
        graphics.text(font, Component.translatable("gui.sum1rem0d.fletching.output"), leftPos + 8, topPos + 46, 0xFF404040, false);
    }
}
