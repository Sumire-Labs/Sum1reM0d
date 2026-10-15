package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class FletchingMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$fletch(Level level, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((BlockState) (Object) this).is(Blocks.FLETCHING_TABLE) || !Rules.on("fletching")) return;
        BlockPos pos = hit.getBlockPos();
        if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider((id, inv, p) -> new FletchingMenu(id, inv, ContainerLevelAccess.create(level, pos)), Component.literal("矢細工台")));
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
