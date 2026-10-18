package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.SecondOffhandClient;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SecondOffhandInventoryInputMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$swapSecondOffhand(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof InventoryScreen) || !ClientRules.enabled("second_offhand")) return;
        var access = (ContainerScreenAccess) this;
        int x = access.sum1rem0d$leftPos() + 77, y = access.sum1rem0d$topPos() + 82;
        if (event.x() >= x - 1 && event.x() < x + 19 && event.y() >= y - 1 && event.y() < y + 19) {
            SecondOffhandClient.requestSwap();
            cir.setReturnValue(true);
        }
    }
}
