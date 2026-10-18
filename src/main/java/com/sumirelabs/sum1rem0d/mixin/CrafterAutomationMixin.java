package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.AutomationProcessing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native crafter sample stays in place while its redstone pulse draws ingredients from side stores. */
@Mixin(CrafterBlock.class)
public abstract class CrafterAutomationMixin {
    @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true)
    private void sum1rem0d$sideBarrelInputs(BlockState state,ServerLevel level,BlockPos pos,CallbackInfo callback){
        if(!AutomationProcessing.enabled("automated_crafting"))return;
        if(level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter){
            AutomationProcessing.craftFromVanillaCrafter(level,pos,crafter);
            if(state.hasProperty(CrafterBlock.TRIGGERED)&&state.getValue(CrafterBlock.TRIGGERED))level.setBlock(pos,state.setValue(CrafterBlock.TRIGGERED,false),3);
            crafter.setTriggered(false);
        }
        callback.cancel();
    }
}
