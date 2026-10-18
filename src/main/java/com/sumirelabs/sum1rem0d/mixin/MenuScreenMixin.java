package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class, PauseScreen.class})
public abstract class MenuScreenMixin extends Screen {
    protected MenuScreenMixin(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void sum1rem0d$configButton(CallbackInfo callback) {
        if ((Object) this instanceof PauseScreen pause && !pause.showsPauseMenu()) return;
        addRenderableWidget(Button.builder(Component.literal("Sum1reM0d Config"),
            button -> minecraft.gui.setScreen(new ConfigScreen((Screen) (Object) this)))
            .bounds(5, 5, 120, 20).build());
        var wiki = addRenderableWidget(Button.builder(Component.literal("Sum1reM0d Wiki"),
            button -> minecraft.gui.setScreen(new com.sumirelabs.sum1rem0d.client.WikiScreen((Screen) (Object) this)))
            .bounds(5, 29, 100, 20).build());
        if ((Object)this instanceof PauseScreen && minecraft.player != null) {
            var wallet = addRenderableWidget(Button.builder(Component.literal("Sum1re 財布 / 取引"), button -> minecraft.gui.setScreen(new com.sumirelabs.sum1rem0d.client.WalletScreen(this))).bounds(5,53,100,20).build());
            wallet.active = com.sumirelabs.sum1rem0d.config.ClientRules.enabled("economy");
        }
        wiki.active = com.sumirelabs.sum1rem0d.client.WikiRuntime.enabled();
    }
}
