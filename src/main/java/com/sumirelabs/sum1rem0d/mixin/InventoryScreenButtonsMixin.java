package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.WalletScreen;
import com.sumirelabs.sum1rem0d.client.WikiRuntime;
import com.sumirelabs.sum1rem0d.client.WikiScreen;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryScreen.class,CreativeModeInventoryScreen.class})
public abstract class InventoryScreenButtonsMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageHeight;

    protected InventoryScreenButtonsMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void sum1rem0d$navigationButtons(CallbackInfo callback) {
        var screen = (Screen) (Object) this;
        int x = leftPos + 4;
        int y = screen instanceof CreativeModeInventoryScreen ? Math.min(height-20,topPos+imageHeight+4) : Math.max(2, topPos-20);
        var wiki = addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.inventory_wiki"),
            button -> minecraft.gui.setScreen(new WikiScreen(screen)))
            .bounds(x, y, 48, 18).build());
        wiki.active = WikiRuntime.enabled();
        var wallet = addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.inventory_wallet"),
            button -> minecraft.gui.setScreen(new WalletScreen(screen)))
            .bounds(x + 52, y, 52, 18).build());
        wallet.active = ClientRules.enabled("economy");
    }
}
