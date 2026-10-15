package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.BrowserOverlays;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class BrowserInitMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void sum1rem0d$render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, float partial, CallbackInfo ci) {
        graphics.nextStratum();
        BrowserOverlays.render((Screen) (Object) this, graphics, x, y);
    }
    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void sum1rem0d$init(int width, int height, CallbackInfo ci) { BrowserOverlays.initialize((Screen) (Object) this); }
}
