package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.game.SeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Registers the seat anchor as renderable while deliberately drawing no entity geometry. */
public final class SeatEntityRenderer extends EntityRenderer<SeatEntity, EntityRenderState> {
    public SeatEntityRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; shadowStrength = 0; }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
    //? if >=26.3 {
    @Override public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z, float partialTick) { return false; }
    //?} else {
    /*@Override public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z) { return false; }
    *///?}
}
