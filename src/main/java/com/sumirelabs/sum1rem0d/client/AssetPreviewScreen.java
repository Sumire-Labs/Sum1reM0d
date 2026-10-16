package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Native item model preview in the disposable, opt-in services fixture. */
final class AssetPreviewScreen extends Screen {
    private static final String[] NAMES = {"soap", "coin", "coin_roll", "mud_ball"};
    AssetPreviewScreen() { super(Component.literal("Sum1reM0d / 16 x 16")); }
    private static Item item(String name) { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ModContent.id(name)); }
    static void verify(Minecraft mc) {
        var resolver = new net.minecraft.client.renderer.item.ItemModelResolver(mc.getModelManager());
        for (String name : NAMES) {
            var render = new net.minecraft.client.renderer.item.ItemStackRenderState();
            resolver.updateForTopItem(render, new ItemStack(item(name)), ItemDisplayContext.GUI, mc.level, null, 0);
            var sprite = render.pickParticleMaterial(net.minecraft.util.RandomSource.create(42)).sprite().contents();
            if (!sprite.name().toString().equals("sum1rem0d:item/" + name) || sprite.width() != 16 || sprite.height() != 16)
                throw new AssertionError("Native 16x16 model missing for " + name + ": " + sprite.name());
        }
        if (mc.getResourceManager().getResource(ModContent.id("icon.png")).isEmpty()) throw new AssertionError("Mod icon resource is missing");
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        g.fill(0,0,width,height,0xFF22242A); int left=(width-320)/2,top=38;
        g.centeredText(font,title,width/2,12,0xFFFFFFFF);
        for(int i=0;i<NAMES.length;i++) {
            int x=left+(i%2)*120+60,y=top+(i/2)*84;
            g.fill(x-28,y-4,x+28,y+52,0xFFB5B5B5);
            g.pose().pushMatrix();g.pose().translate(x-24,y);g.pose().scale(3F);g.item(new ItemStack(item(NAMES[i])),0,0);g.pose().popMatrix();
            var label = Component.translatable("item.sum1rem0d."+NAMES[i]);
            if (font.width(label) <= 112) g.centeredText(font,label,x,y+56,0xFFFFFFFF);
            else g.textWithWordWrap(font,label,x-56,y+56,112,0xFFFFFFFF);
        }
        g.blit(RenderPipelines.GUI_TEXTURED,ModContent.id("icon.png"),left+256,top+40,0,0,48,48,16,16,16,16);
        g.centeredText(font,"Mod icon",left+280,top+96,0xFFFFFFFF);
    }
}
