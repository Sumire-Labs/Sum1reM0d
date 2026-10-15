package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.WhiteRenderState;
import com.sumirelabs.sum1rem0d.config.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntityRenderer.class)
public abstract class WhiteRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void sum1rem0d$extract(LivingEntity entity, LivingEntityRenderState state, float tick, CallbackInfo ci) {
        ((WhiteRenderState) state).sum1rem0d$setWhite(com.sumirelabs.sum1rem0d.client.AfkLights.white(entity.getUUID()));
        if (((WhiteRenderState) state).sum1rem0d$isWhite() && ClientRules.number("afk_white", "whiteness") > 0) state.lightCoords = 0xF000F0;
    }
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$render(LivingEntityRenderState state, boolean visible, boolean transparent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (((WhiteRenderState) state).sum1rem0d$isWhite() && visible && ClientRules.number("afk_white", "whiteness") >= .999) cir.setReturnValue(RenderTypes.entityTranslucentEmissive(Identifier.withDefaultNamespace("textures/block/white_concrete.png")));
    }
    @Inject(method = "getWhiteOverlayProgress", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$overlay(LivingEntityRenderState state, CallbackInfoReturnable<Float> cir) {
        if (((WhiteRenderState) state).sum1rem0d$isWhite()) cir.setReturnValue((float) ClientRules.number("afk_white", "whiteness"));
    }
}
