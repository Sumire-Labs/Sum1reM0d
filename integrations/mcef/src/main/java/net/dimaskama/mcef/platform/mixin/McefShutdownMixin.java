package net.dimaskama.mcef.platform.mixin;

import net.dimaskama.mcef.impl.MCEFModern;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class McefShutdownMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void mcef$close(CallbackInfo ci) { MCEFModern.shutdown(); }
}
