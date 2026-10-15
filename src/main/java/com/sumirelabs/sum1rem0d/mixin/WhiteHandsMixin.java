package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.sumirelabs.sum1rem0d.client.AfkLights;

@Mixin(AvatarRenderer.class)
public abstract class WhiteHandsMixin {
    @ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true)
    private Identifier sum1rem0d$skin(Identifier skin) {
        var player = Minecraft.getInstance().player;
        return player != null && AfkLights.white(player.getUUID()) && com.sumirelabs.sum1rem0d.config.ClientRules.number("afk_white", "whiteness") >= .999 ? Identifier.withDefaultNamespace("textures/block/white_concrete.png") : skin;
    }
    @ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sum1rem0d$light(int light) {
        var player = Minecraft.getInstance().player;
        return player != null && AfkLights.white(player.getUUID()) && com.sumirelabs.sum1rem0d.config.ClientRules.number("afk_white", "whiteness") > 0 ? 0xF000F0 : light;
    }
    @ModifyArg(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;)V"), index = 4)
    private int sum1rem0d$overlay(int original) {
        var player = Minecraft.getInstance().player;
        return player != null && AfkLights.white(player.getUUID()) ? net.minecraft.client.renderer.texture.OverlayTexture.pack((float) com.sumirelabs.sum1rem0d.config.ClientRules.number("afk_white", "whiteness"), false) : original;
    }
}
