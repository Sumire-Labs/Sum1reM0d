package com.sumirelabs.sum1rem0d.mixin.compat;

import com.sumirelabs.sum1rem0d.client.BrowserOverlays;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional JEI area exclusion; no hard dependency on JEI in the mod jar. */
@Pseudo
@Mixin(targets = "mezz.jei.library.gui.helpers.ScreenHelper", remap = false)
public abstract class JeiBrowserAreaMixin {
    @Inject(method = "getGuiExclusionAreas", at = @At("RETURN"), cancellable = true, require = 0)
    private void sum1rem0d$browser(Screen screen, CallbackInfoReturnable<Stream<Rect2i>> cir) {
        int[] area = BrowserOverlays.exclusion();
        if (area == null) return;
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), Stream.of(new Rect2i(area[0], area[1], area[2], area[3]))));
    }
}
