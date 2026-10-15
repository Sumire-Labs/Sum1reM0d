package com.sumirelabs.sum1rem0d.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Assemble vanilla container parts without shipping copies of Minecraft assets. */
final class VanillaGuiParts {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier CRAFTING = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    private VanillaGuiParts() {}
    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        // Nine-slice only the blank beveled frame, not the chest's embedded slots.
        patch(graphics, x + 7, y + 7, width - 14, height - 14, 7, 7, 1, 1);
        patch(graphics, x + 7, y, width - 14, 7, 7, 0, 1, 7);
        patch(graphics, x + 7, y + height - 7, width - 14, 7, 7, 215, 1, 7);
        patch(graphics, x, y + 7, 7, height - 14, 0, 7, 7, 1);
        patch(graphics, x + width - 7, y + 7, 7, height - 14, 169, 7, 7, 1);
        patch(graphics, x, y, 7, 7, 0, 0, 7, 7);
        patch(graphics, x + width - 7, y, 7, 7, 169, 0, 7, 7);
        patch(graphics, x, y + height - 7, 7, 7, 0, 215, 7, 7);
        patch(graphics, x + width - 7, y + height - 7, 7, 7, 169, 215, 7, 7);
    }
    private static void patch(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, u, v, width, height, sourceWidth, sourceHeight, 256, 256);
    }
    static void slot(GuiGraphicsExtractor graphics, int x, int y) { graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x - 1, y - 1, 18, 18); }
    static void arrow(GuiGraphicsExtractor graphics, int x, int y) { graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING, x, y, 90, 35, 22, 15, 256, 256); }
}
