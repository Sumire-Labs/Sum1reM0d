package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Only opened by the opt-in native visual verifier. */
final class AssetPreviewScreen extends Screen {
    AssetPreviewScreen() { super(Component.literal("Sum1reM0d / Assets")); }
    static void verify(Minecraft mc) {
        var render = new net.minecraft.client.renderer.item.ItemStackRenderState();
        new net.minecraft.client.renderer.item.ItemModelResolver(mc.getModelManager()).updateForTopItem(render, new ItemStack(ModContent.soap()), ItemDisplayContext.GUI, mc.level, null, 0);
        String texture = render.pickParticleMaterial(net.minecraft.util.RandomSource.create(42)).sprite().contents().name().toString();
        if (!texture.equals("sum1rem0d:item/soap")) throw new AssertionError("Native soap model must use its own sprite: " + texture);
        if (mc.getResourceManager().getResource(ModContent.id("icon.png")).isEmpty()) throw new AssertionError("Mod icon resource is missing");
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(0, 0, width, height, 0xFF191821);
        int left = width / 2 - 116, top = height / 2 - 48;
        graphics.centeredText(font, title, width / 2, top - 36, 0xFFFFFFFF);
        graphics.fill(left - 4, top - 4, left + 100, top + 100, 0xFF34323C);
        graphics.pose().pushMatrix(); graphics.pose().translate(left, top); graphics.pose().scale(6f);
        graphics.item(new ItemStack(ModContent.soap()), 0, 0); graphics.pose().popMatrix();
        graphics.blit(RenderPipelines.GUI_TEXTURED, ModContent.id("icon.png"), left + 136, top, 0, 0, 96, 96, 256, 256, 256, 256);
        graphics.centeredText(font, Component.translatable("item.sum1rem0d.soap"), left + 48, top + 110, 0xFFFFFFFF);
        graphics.centeredText(font, "Sum1reM0d", left + 184, top + 110, 0xFFFFFFFF);
    }
}
