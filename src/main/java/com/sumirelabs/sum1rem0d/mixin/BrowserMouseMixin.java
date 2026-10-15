package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.*;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class BrowserMouseMixin {
    @Unique private long sum1rem0d$lastClick;
    @Unique private net.minecraft.client.gui.screens.Screen sum1rem0d$lastScreen;
    @Unique private int sum1rem0d$lastButton;
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$button(long window, net.minecraft.client.input.MouseButtonInfo button, int action, CallbackInfo ci) {
        var mc = Minecraft.getInstance(); var screen = mc.gui.screen();
        if (window != mc.getWindow().handle() || screen == null || mc.gui.overlay() != null) return;
        var mouse = (MouseHandler) (Object) this;
        var event = new net.minecraft.client.input.MouseButtonEvent(mouse.getScaledXPos(mc.getWindow()), mouse.getScaledYPos(mc.getWindow()), button);
        if (action == 1) {
            ClientRuntime.input(); long now = System.nanoTime();
            boolean doubled = screen == sum1rem0d$lastScreen && button.button() == sum1rem0d$lastButton && now - sum1rem0d$lastClick < MouseHandler.DOUBLE_CLICK_THRESHOLD_MS * 1_000_000L;
            if (BrowserOverlays.click(screen, event, doubled)) { sum1rem0d$lastScreen = screen; sum1rem0d$lastButton = button.button(); sum1rem0d$lastClick = now; ci.cancel(); }
            else sum1rem0d$lastScreen = null;
        } else if (action == 0 && BrowserOverlays.release(screen, event)) ci.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        var mc = Minecraft.getInstance(); var screen = mc.gui.screen();
        if (window != mc.getWindow().handle() || screen == null || mc.gui.overlay() != null) return;
        var mouse = (MouseHandler) (Object) this;
        if (BrowserOverlays.scroll(screen, mouse.getScaledXPos(mc.getWindow()), mouse.getScaledYPos(mc.getWindow()), vertical)) { ClientRuntime.input(); ci.cancel(); }
    }
    @Inject(method = "onMove", at = @At("HEAD"))
    //? if >=26.3 {
    private void sum1rem0d$move(long window, double x, double y, double dx, double dy, CallbackInfo ci) {
        if (dx != 0 || dy != 0) ClientRuntime.input();
    //?} else {
    /*private void sum1rem0d$move(long window, double x, double y, CallbackInfo ci) {
        var mouse = (MouseHandler) (Object) this;
        if (x != mouse.xpos() || y != mouse.ypos()) ClientRuntime.input();
    *///?}
        var mc = Minecraft.getInstance(); var screen = mc.gui.screen();
        if (screen != null) BrowserOverlays.moved(x * screen.width / mc.getWindow().getScreenWidth(), y * screen.height / mc.getWindow().getScreenHeight());
    }
}
