package com.sumirelabs.sum1rem0d.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class DeathBoxContent {
    private DeathBoxContent() {}
    public static Block block() { return BuiltInRegistries.BLOCK.getValue(ModContent.id("death_box")); }
    @SuppressWarnings("unchecked") public static BlockEntityType<DeathBoxEntity> type() { return (BlockEntityType<DeathBoxEntity>) BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(ModContent.id("death_box")); }
    public static Block createBlock() { return new DeathBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f, 3600000f).setId(ResourceKey.create(Registries.BLOCK, ModContent.id("death_box")))); }
    public static BlockEntityType<DeathBoxEntity> createType() {
        //? if fabric {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(DeathBoxEntity::new, block()).build();
        //?} elif neoforge {
        /*return new BlockEntityType<>(DeathBoxEntity::new, Set.of(block()));
        *///?}
    }
    public static void registerFabric() {
        Registry.register(BuiltInRegistries.BLOCK, ModContent.id("death_box"), createBlock());
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModContent.id("death_box"), createType());
    }
    public static boolean capture(ServerPlayer player, ServerLevel level) {
        if (!Rules.on("death_box") || player.isSpectator() || level.getGameRules().get(GameRules.KEEP_INVENTORY)) return false;
        var items = new ArrayList<ItemStack>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) if (!inventory.getItem(i).isEmpty()) items.add(inventory.getItem(i).copy());
        var extra = (SecondOffhandAccess) player;
        if (!extra.sum1rem0d$secondOffhand().isEmpty()) items.add(extra.sum1rem0d$secondOffhand().copy());
        var carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) items.add(carried.copy());
        if (items.isEmpty()) return false;
        BlockPos origin = new BlockPos(player.blockPosition().getX(), Math.clamp(player.blockPosition().getY(), level.getMinY(), level.getMaxY()), player.blockPosition().getZ());
        int radius = Rules.i("death_box", "search_radius");
        BlockPos target = null;
        for (var candidate : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level.isOutsideBuildHeight(candidate) || !level.hasChunkAt(candidate) || !level.getWorldBorder().isWithinBounds(candidate)) continue;
            if (!level.getBlockState(candidate).canBeReplaced() || level.getBlockEntity(candidate) != null) continue;
            if (target == null || candidate.distSqr(origin) < target.distSqr(origin)) target = candidate.immutable();
        }
        if (target == null || !level.setBlock(target, block().defaultBlockState(), 3)) return false;
        if (!(level.getBlockEntity(target) instanceof DeathBoxEntity box)) { level.removeBlock(target, false); return false; }
        box.fill(items);
        inventory.clearContent();
        extra.sum1rem0d$secondOffhand(ItemStack.EMPTY);
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
        SecondOffhandService.syncOwner(player);
        if (Rules.b("death_box", "announce")) player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Death Box: " + target.getX() + ", " + target.getY() + ", " + target.getZ()));
        return true;
    }
    public static void release(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DeathBoxEntity box)) return;
        List<ItemStack> items = box.takeContents();
        level.removeBlock(pos, false);
        for (var stack : items) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
    }
}
