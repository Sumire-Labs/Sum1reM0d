package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LiquidBlock.class)
public abstract class CryingObsidianMixin {
 @Redirect(method="shouldSpreadLiquid",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean sum1rem0d$crying(Level level,BlockPos pos,BlockState result){
  if(Rules.on("crying_obsidian_mix")&&result.is(Blocks.OBSIDIAN)){
   boolean water=!Rules.b("crying_obsidian_mix","flowing_only");
   for(var direction:Direction.values()){var fluid=level.getFluidState(pos.relative(direction));water|=fluid.is(FluidTags.WATER)&&!fluid.isSource();}
   if(water)result=Blocks.CRYING_OBSIDIAN.defaultBlockState();
  }
  return level.setBlockAndUpdate(pos,result);
 }
}
