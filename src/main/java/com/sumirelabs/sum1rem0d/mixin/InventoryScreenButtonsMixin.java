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

    protected InventoryScreenButtonsMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void sum1rem0d$navigationButtons(CallbackInfo callback) {
        var screen = (Screen) (Object) this;
        var access=(ContainerScreenAccess)this;
        int x=access.sum1rem0d$leftPos()+4, top=access.sum1rem0d$topPos(), bottom=top+access.sum1rem0d$imageHeight();
        int y=screen instanceof CreativeModeInventoryScreen ? (bottom+50<=height?bottom+32:Math.max(2,top-50)) : (top>=22?top-20:Math.min(height-20,bottom+4));
        var wiki = addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.inventory_wiki"),
            button -> minecraft.gui.setScreen(new WikiScreen(screen)))
            .bounds(x, y, 32, 18).build());
        wiki.active = WikiRuntime.enabled();
        var wallet = addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.inventory_wallet"),
            button -> minecraft.gui.setScreen(new WalletScreen(screen)))
            .bounds(x + 36, y, 76, 18).build());
        wallet.active = ClientRules.enabled("economy");
        var shop=addRenderableWidget(Button.builder(Component.translatable("gui.sum1rem0d.inventory_shop"),button->minecraft.gui.setScreen(new com.sumirelabs.sum1rem0d.client.ShopScreen(screen))).bounds(x+116,y,52,18).build());
        shop.active=ClientRules.enabled("economy")&&ClientRules.enabled("sumzon");
    }
}
