package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.AutomationProcessing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player insertion into a vanilla jukebox is the input transaction for grinding. */
@Mixin(JukeboxBlock.class)
public abstract class JukeboxGrinderMixin {
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void sum1rem0d$grind(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> callback){
        if(level.isClientSide()||!AutomationProcessing.enabled("jukebox_grinder"))return;
        if(AutomationProcessing.grindResult(stack).isEmpty())return;
        if(player instanceof ServerPlayer server&&AutomationProcessing.grindFromHand(server,pos,hand))callback.setReturnValue(InteractionResult.CONSUME);
        else callback.setReturnValue(InteractionResult.FAIL);
    }
}
