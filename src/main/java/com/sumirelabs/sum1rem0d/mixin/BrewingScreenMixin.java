package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BrewingStandScreen.class)
public abstract class BrewingScreenMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void sum1rem0d$slots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        if (!ClientRules.enabled("single_brewing")) return;
        var access = (ContainerScreenAccess) this;
        int left = access.sum1rem0d$leftPos(); int top = (((BrewingStandScreen) (Object) this).height - 166) / 2;
        graphics.fill(left + 77, top + 56, left + 97, top + 77, 0xFFC6C6C6);
        graphics.fill(left + 100, top + 49, left + 120, top + 70, 0xFFC6C6C6);
        graphics.fill(left + 17, top + 29, left + 35, top + 33, 0xFFC6C6C6);
    }
}
