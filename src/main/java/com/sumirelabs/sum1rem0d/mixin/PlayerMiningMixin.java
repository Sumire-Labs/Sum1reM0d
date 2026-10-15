package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerMiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockState sum1rem0d$broken;
    @Unique private ItemStack sum1rem0d$tool;
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void sum1rem0d$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        sum1rem0d$broken = player.level().getBlockState(pos); sum1rem0d$tool = player.getMainHandItem().copy();
    }
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void sum1rem0d$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && sum1rem0d$broken != null) Gameplay.broke(player, sum1rem0d$broken, sum1rem0d$tool);
    }
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void sum1rem0d$attack(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int height, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && player.isWithinBlockInteractionRange(pos, 0))
            Gameplay.bedrock(player, player.level().getBlockState(pos));
    }
}
