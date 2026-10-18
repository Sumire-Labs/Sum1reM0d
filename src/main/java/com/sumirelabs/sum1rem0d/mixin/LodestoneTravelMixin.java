package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.LodestoneTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class LodestoneTravelMixin {
 @Inject(method="use",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$travel(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
  if(player instanceof ServerPlayer server){var result=LodestoneTravel.use(server,player.getItemInHand(hand));if(result!=null)cir.setReturnValue(result);}
 }
}
