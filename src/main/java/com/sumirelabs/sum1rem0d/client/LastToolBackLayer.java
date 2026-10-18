package com.sumirelabs.sum1rem0d.client;

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
    public LastToolBackLayer(RenderLayerParent parent) { super(parent); }

    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, EntityRenderState state, float limbSwing, float limbAmount) {
        var item = LastToolBack.renderState();
        var player = Minecraft.getInstance().player;
        if (item == null || player == null) return;
        double dx = state.x - player.getX(), dy = state.y - player.getY(), dz = state.z - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 0.04) return;
        if (!(getParentModel() instanceof HumanoidModel<?> humanoid)) return;
        pose.pushPose();
        humanoid.body.translateAndRotate(pose);
        pose.translate(0.0, -0.12, 0.18);
        //? if >=26.3 {
        pose.rotateDegrees(Axis.ZP, 135);
        //?} else {
        /*pose.mulPose(Axis.ZP.rotationDegrees(135));
        *///?}
        item.submit(pose, collector, light, 0, 0);
        pose.popPose();
    }
}
