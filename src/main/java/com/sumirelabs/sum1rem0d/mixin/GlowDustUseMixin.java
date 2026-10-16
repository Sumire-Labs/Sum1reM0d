package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class GlowDustUseMixin {
 @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$place(UseOnContext context,CallbackInfoReturnable<InteractionResult> cir){
  if(!context.getItemInHand().is(Items.GLOWSTONE_DUST) || !Rules.on("glow_wire"))return;var place=new BlockPlaceContext(context);var level=place.getLevel();var pos=place.getClickedPos();var player=place.getPlayer();
  var old=level.getBlockState(pos);if(old.is(Blocks.REDSTONE_WIRE) || old.getBlock() instanceof GlowWireBlock || !old.canBeReplaced(place) || !level.getFluidState(pos).isEmpty())return;
  if(player!=null && (!player.mayUseItemAt(pos,place.getClickedFace(),context.getItemInHand()) || !level.mayInteract(player,pos)))return;
  var block=ExtraContent.block("glow_wire");var state=block.getStateForPlacement(place);if(state==null || !state.canSurvive(level,pos))return;
  if(level.setBlock(pos,state,3)){context.getItemInHand().consume(1,player);level.playSound(player,pos,SoundType.STONE.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,pos,net.minecraft.world.level.gameevent.GameEvent.Context.of(player,state));cir.setReturnValue(InteractionResult.SUCCESS);}
 }
}
