package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.Sum1reM0d;

import java.util.HashSet;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

/** Small native regression checks for cauldron bottles, merging, shared surfaces, and tool drops. */
final class CauldronFixVerification {
    private CauldronFixVerification() {}

    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config();
        var previous = config.snapshot();
        var draft = previous.deepCopy();
        var feature = draft.getAsJsonObject("features").getAsJsonObject("large_cauldron");
        feature.addProperty("enabled", true);
        feature.addProperty("auto_merge", true);
        config.update(draft, config.revision());
        try {
            ServerLevel level = server.overworld();
            var large = MobilityContent.largeCauldron();
            var huge = MobilityContent.hugeCauldron();
            require(large != null && huge != null, "Large and huge cauldron blocks are registered");
            verifyBottles(level, large, passed);
            verifyNativeExpansion(level, huge, passed);
            verifyToolAndDrops(server, level, large, passed);
        } finally {
            config.update(previous, config.revision());
        }
    }

    private static void verifyBottles(ServerLevel level, LargeCauldronBlock large, Consumer<String> passed) throws Exception {
        BlockPos pos = new BlockPos(3220, 240, 80);
        prepare(level, pos, 2);
        ServerVerification.place(level, pos, large.defaultBlockState());
        var actor = EconomyVerification.actor(level.getServer(), level, "CauldronBottle");
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try {
            large.setPlacedBy(level, pos, level.getBlockState(pos), actor, new ItemStack(MobilityContent.cauldronBlock("large_cauldron")));
            ItemStack waterPotion = new ItemStack(Items.POTION);
            waterPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            actor.setItemInHand(InteractionHand.MAIN_HAND, waterPotion);
            InteractionResult pour = large.useItemOn(actor.getMainHandItem(), level.getBlockState(pos), level, pos, actor, InteractionHand.MAIN_HAND, hit);
            require(pour.consumesAction() && actor.getMainHandItem().is(Items.GLASS_BOTTLE)
                    && large.availableWater(level, pos) == 1
                    && sharedLevel(level, pos, 1, 1, 1),
                    "A water potion fills the whole empty 2x1 cauldron by one shared unit without replacing either cell [result="+pour+", first="+level.getBlockState(pos)+", second="+level.getBlockState(pos.east())+"]");

            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
            InteractionResult draw = large.useItemOn(actor.getMainHandItem(), level.getBlockState(pos), level, pos, actor, InteractionHand.MAIN_HAND, hit);
            require(draw.consumesAction() && actor.getMainHandItem().is(Items.POTION)
                    && actor.getMainHandItem().get(DataComponents.POTION_CONTENTS).is(Potions.WATER)
                    && large.availableWater(level, pos) == 0
                    && level.getBlockState(pos).getBlock() == large && level.getBlockState(pos.east()).getBlock() == large,
                    "A glass bottle drains one shared water unit and keeps both large-cauldron cells [result="+draw+", first="+level.getBlockState(pos)+", second="+level.getBlockState(pos.east())+"]");
            passed.accept("Large cauldron routes native bottle interactions through the whole saved footprint");
        } finally { actor.discard(); }
    }

    private static void verifyNativeExpansion(ServerLevel level, LargeCauldronBlock huge, Consumer<String> passed) {
        BlockPos pos = new BlockPos(3240, 240, 80);
        int[] amounts = {2, 1, 3, 1};
        for (int index = 0; index < 4; index++) {
            BlockPos cell = pos.offset(index % 2, 0, index / 2);
            ServerVerification.place(level, cell, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, amounts[index]));
        }
        require(huge.availableWater(level, pos) == 7 && sharedLevel(level, pos, 2, 7, 7),
                "Expanding four water cauldrons preserves their summed contents and sets one matching water surface across all cells");
        BlockState first = level.getBlockState(pos);
        BlockState east = level.getBlockState(pos.east());
        boolean noInnerWall = !Shapes.joinIsNotEmpty(first.getShape(level, pos, CollisionContext.empty()),
                    Shapes.box(15/16.0,4/16.0,7/16.0,1,1,9/16.0), BooleanOp.AND)
                && !Shapes.joinIsNotEmpty(east.getShape(level, pos.east(), CollisionContext.empty()),
                    Shapes.box(0,4/16.0,7/16.0,1/16.0,1,9/16.0), BooleanOp.AND);
        require(noInnerWall, "Connected 2x2 cauldron geometry omits the inner east-west walls");
        passed.accept("Native 2x2 cauldron expansion preserves water and renders a connected basin at one height");
    }

    private static void verifyToolAndDrops(MinecraftServer server, ServerLevel level, LargeCauldronBlock large, Consumer<String> passed) {
        BlockPos pos = new BlockPos(3260, 240, 80);
        prepare(level, pos, 2);
        ServerVerification.place(level, pos, large.defaultBlockState());
        var player = EconomyVerification.actor(server, level, "CauldronTool");
        try {
            large.setPlacedBy(level, pos, level.getBlockState(pos), player, new ItemStack(MobilityContent.cauldronBlock("large_cauldron")));
            BlockState state = level.getBlockState(pos);
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            require(state.requiresCorrectToolForDrops() && pickaxe.isCorrectToolForDrops(state), "Large cauldron is mineable with a proper pickaxe");
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + 1.5);
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
            var prior = new HashSet<>(ServerVerification.spawnedIn(level));
            require(player.gameMode.destroyBlock(pos), "A survival player can break a large cauldron with a diamond pickaxe");
            var drops = ServerVerification.spawnedIn(level).stream().filter(entity -> !prior.contains(entity))
                    .filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast)
                    .filter(item -> item.position().distanceToSqr(Vec3.atCenterOf(pos)) < 16)
                    .filter(item -> item.getItem().is(MobilityContent.cauldronBlock("large_cauldron").asItem())).toList();
            require(level.getBlockState(pos).isAir() && level.getBlockState(pos.east()).isAir()
                    && drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                    "Breaking any footprint cell removes the whole cauldron and drops one item [drops="+drops.size()+"]");
            passed.accept("Large cauldron is pickaxe-mineable and drops once per complete footprint");
        } finally { player.discard(); }
    }

    private static boolean sharedLevel(ServerLevel level, BlockPos root, int depth, int expected, int minShared) {
        if (depth != 1 && depth != 2) return false;
        for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) {
            BlockState state = level.getBlockState(root.offset(x, 0, z));
            if (!(state.getBlock() instanceof LargeCauldronBlock) || state.getValue(LargeCauldronBlock.LEVEL) != expected
                    || !state.getValue(LargeCauldronBlock.VOLUME_SHARED)) return false;
        }
        return expected >= minShared;
    }

    private static void prepare(ServerLevel level, BlockPos root, int depth) {
        for (int z = 0; z < depth; z++) for (int x = 0; x < 2; x++) ServerVerification.place(level, root.offset(x, 0, z), Blocks.AIR.defaultBlockState());
    }

    private static void require(boolean condition, String message) { ServerVerification.require(condition, message); }
}
