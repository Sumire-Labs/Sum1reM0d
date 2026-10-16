package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TintedGlassCollisionMixin {
 @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at=@At("HEAD"), cancellable=true)
 private void sum1rem0d$empty(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) { if (((BlockState)(Object)this).is(Blocks.TINTED_GLASS) && level != net.minecraft.world.level.EmptyBlockGetter.INSTANCE && Rules.on("passable_tinted_glass")) cir.setReturnValue(Shapes.empty()); }
 @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at=@At("HEAD"), cancellable=true)
 private void sum1rem0d$context(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) { if (((BlockState)(Object)this).is(Blocks.TINTED_GLASS) && level != net.minecraft.world.level.EmptyBlockGetter.INSTANCE && Rules.on("passable_tinted_glass")) cir.setReturnValue(Shapes.empty()); }
 @Inject(method="isCollisionShapeFullBlock",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$full(BlockGetter level,BlockPos pos,CallbackInfoReturnable<Boolean> cir) { if (((BlockState)(Object)this).is(Blocks.TINTED_GLASS) && level != net.minecraft.world.level.EmptyBlockGetter.INSTANCE && Rules.on("passable_tinted_glass")) cir.setReturnValue(false); }
}
