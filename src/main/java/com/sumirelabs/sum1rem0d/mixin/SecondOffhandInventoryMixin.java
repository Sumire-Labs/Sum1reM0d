package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.SecondOffhandClient;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class SecondOffhandInventoryMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void sum1rem0d$secondOffhandSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ClientRules.enabled("second_offhand")) return;
        var access = (ContainerScreenAccess) this;
        int x = access.sum1rem0d$leftPos() + 77, y = access.sum1rem0d$topPos() + 82;
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF202020);
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        graphics.text(Minecraft.getInstance().font, Component.literal("2"), x + 2, y + 1, 0xFFDFDFDF, true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void sum1rem0d$secondOffhandItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ClientRules.enabled("second_offhand")) return;
        var access = (ContainerScreenAccess) this;
        int x = access.sum1rem0d$leftPos() + 78, y = access.sum1rem0d$topPos() + 83;
        Player player = Minecraft.getInstance().player;
        if (player != null) graphics.item(SecondOffhandClient.stack(player.getUUID()), x, y);
    }

}
