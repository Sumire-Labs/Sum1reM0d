package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invisible, nonpersistent passenger anchor. Its EntityType is registered by the parent. */
public final class SeatEntity extends Entity {
    private BlockPos anchor = BlockPos.ZERO;
    public SeatEntity(EntityType<?> type, Level level) { super(type, level); noPhysics = true; setNoGravity(true); }
    public void anchor(BlockPos pos, double x, double y, double z) { anchor = pos.immutable(); setPos(x, y, z); }
    public BlockPos anchorPos() { return anchor; }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty() || !level().hasChunkAt(anchor)
                || !WorldMobility.validSeatBlock(level().getBlockState(anchor)))) discard();
    }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position(); }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {}
}
