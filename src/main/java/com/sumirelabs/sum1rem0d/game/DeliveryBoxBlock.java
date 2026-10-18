package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Parcel endpoint; use is delegated to the server side order ledger. */
public final class DeliveryBoxBlock extends Block implements EntityBlock {
    //? if <26.3 {
    private static final com.mojang.serialization.MapCodec<DeliveryBoxBlock> CODEC = simpleCodec(DeliveryBoxBlock::new);
    @Override public com.mojang.serialization.MapCodec<DeliveryBoxBlock> codec() { return CODEC; }
    //?}
    public DeliveryBoxBlock(Properties props) { super(props); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeliveryBoxEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        DeliveryBoxEntity.placed(level, pos, placer);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeliveryBoxEntity box)) return InteractionResult.FAIL;
        if (!box.mayAccess(player)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.literal("この宅配BOXを利用する権限がありません"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (box.owner() == null) box.setOwner(player.getUUID(), player.getUUID());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                DeliveryBoxAccess.open(serverPlayer, pos, box);
                serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, who) -> new DeliveryBoxMenu(id, inventory, box),
                    Component.translatable("block.sum1rem0d.sumzon_delivery_box")));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
