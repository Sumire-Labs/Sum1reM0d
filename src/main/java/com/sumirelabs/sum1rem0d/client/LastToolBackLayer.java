package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Renders the player's last held tool across the back during third-person player rendering. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class LastToolBackLayer extends RenderLayer {
    private static long cachedOptionsRevision = Long.MIN_VALUE;
    private static JsonObject cachedOptions;
    public LastToolBackLayer(RenderLayerParent parent) { super(parent); }

    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, EntityRenderState state, float limbSwing, float limbAmount) {
        var item = LastToolBack.renderState();
        var player = Minecraft.getInstance().player;
        if (item == null || player == null) return;
        double dx = state.x - player.getX(), dy = state.y - player.getY(), dz = state.z - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 0.04) return;
        if (!(getParentModel() instanceof HumanoidModel<?> humanoid)) return;
        var options = options();
        pose.pushPose();
        humanoid.body.translateAndRotate(pose);
        pose.translate(option(options,"offset_x",0.0), option(options,"offset_y",0.35), option(options,"offset_z",0.18));
        //? if >=26.3 {
        pose.rotateDegrees(Axis.ZP, (float) option(options,"rotation",100));
        //?} else {
        /*pose.mulPose(Axis.ZP.rotationDegrees((float) option(options,"rotation",100)));
        *///?}
        float scale = (float) Math.clamp(option(options,"scale",0.60), 0.2, 0.8);
        pose.scale(scale, scale, scale);
        item.submit(pose, collector, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private static double option(JsonObject section,String key,double fallback) {
        return section != null && section.has(key) && section.get(key).isJsonPrimitive()
                ? section.get(key).getAsDouble() : fallback;
    }

    private static JsonObject options() {
        var store = com.sumirelabs.sum1rem0d.config.PersonalConfig.store();
        long revision = store.revision();
        if (cachedOptions == null || cachedOptionsRevision != revision) {
            cachedOptions = store.snapshot().getAsJsonObject("features").getAsJsonObject("last_tool_back");
            cachedOptionsRevision = revision;
        }
        return cachedOptions;
    }
}
