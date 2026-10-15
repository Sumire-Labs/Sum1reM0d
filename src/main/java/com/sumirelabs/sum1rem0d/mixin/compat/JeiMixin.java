package com.sumirelabs.sum1rem0d.mixin.compat;
import com.sumirelabs.sum1rem0d.client.ViewerBlocker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = {"mezz.jei.gui.overlay.IngredientListOverlay", "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay"}, remap = false)
public abstract class JeiMixin {
    @Inject(method = {"drawScreen", "drawBackground", "drawForeground", "drawTooltips", "drawOnForeground"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$render(CallbackInfo ci) { if (ViewerBlocker.blocked("jei")) ci.cancel(); }
    @Inject(method = {"isListDisplayed", "isMouseOver", "hasKeyboardFocus", "hasBookmarkDrag"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$visible(CallbackInfoReturnable<Boolean> cir) { if (ViewerBlocker.blocked("jei")) cir.setReturnValue(false); }
    @Inject(method = {"getIngredientUnderMouse(DD)Ljava/util/stream/Stream;", "getDraggableIngredientUnderMouse"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sum1rem0d$hover(CallbackInfoReturnable<java.util.stream.Stream<?>> cir) { if (ViewerBlocker.blocked("jei")) cir.setReturnValue(java.util.stream.Stream.empty()); }
}
