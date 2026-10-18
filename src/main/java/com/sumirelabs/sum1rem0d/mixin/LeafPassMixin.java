package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class LeafPassMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$passThroughLeaves(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.getBlock() instanceof LeavesBlock && com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("leaf_pass")) cir.setReturnValue(Shapes.empty());
    }

    @Inject(method = "getEntityInsideCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$keepLeafIntersection(BlockState state, BlockGetter level, BlockPos pos, Entity entity, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.getBlock() instanceof LeavesBlock && com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("leaf_pass")) cir.setReturnValue(Shapes.block());
    }

    @Inject(method = "entityInside", at = @At("TAIL"))
    private void sum1rem0d$slowInsideLeaves(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean movedByPiston, CallbackInfo ci) {
        if (state.getBlock() instanceof LeavesBlock && com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("leaf_pass")) {
            double speed = Math.max(0, Math.min(1, Rules.n("leaf_pass", "inside_speed")));
            entity.makeStuckInBlock(state, new Vec3(speed, speed, speed));
        }
    }
}
