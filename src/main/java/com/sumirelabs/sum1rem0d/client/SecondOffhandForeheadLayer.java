package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.player.Player;

/** Renders the extra item centered on the forehead for the owner and all tracking clients. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class SecondOffhandForeheadLayer extends RenderLayer {
    public SecondOffhandForeheadLayer(RenderLayerParent parent) { super(parent); }

    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
            EntityRenderState renderState, float limbSwing, float limbAmount) {
        if (!(renderState instanceof AvatarRenderState avatar) || !com.sumirelabs.sum1rem0d.config.ClientRules.enabled("second_offhand")
                || !com.sumirelabs.sum1rem0d.config.ClientRules.bool("second_offhand", "forehead_display")) return;
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(avatar.id) instanceof Player player)) return;
        ItemStackRenderState item = SecondOffhandClient.renderState(player);
        if (item == null || !(getParentModel() instanceof HumanoidModel<?> humanoid)) return;
        pose.pushPose();
        humanoid.head.translateAndRotate(pose);
        pose.translate(0, -0.28, -0.42);
        pose.scale(0.35f, 0.35f, 0.35f);
        item.submit(pose, collector, light, 0, 0);
        pose.popPose();
    }
}
