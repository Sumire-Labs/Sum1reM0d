package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.LastToolBackLayer;
import java.util.List;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class LastToolBackRendererMixin {
    @Shadow @Final protected List<RenderLayer> layers;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void sum1rem0d$addBackToolLayer(CallbackInfo ci) {
        if (!((Object) this instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer)) return;
        layers.add(new LastToolBackLayer((RenderLayerParent) (Object) this));
    }
}
