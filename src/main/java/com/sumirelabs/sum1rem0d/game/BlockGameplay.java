package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

public final class BlockGameplay {
    private BlockGameplay() {}
    public static List<ItemStack> drops(BlockState state, ServerLevel level, Entity entity, ItemStack tool, List<ItemStack> original) {
        if (Rules.on("log_loss") && entity instanceof ServerPlayer && Rules.matches(state, Rules.list("log_loss", "target_blocks")) && Rules.roll(level.getRandom(), "log_loss")) return List.of();
        if (!Rules.on("ore_drops") || Rules.b("ore_drops", "silk_original") && Gameplay.silk(level, tool)) return original;
        String block = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (Rules.b("ore_drops", "deepslate")) block = block.replace(":deepslate_", ":");
        var entries = Rules.list("ore_drops", "mapping");
        String source = block;
        if (entries.stream().noneMatch(entry -> entry.split("=", 2)[0].trim().equals(source))) return original;
        String mode = Rules.s("ore_drops", "mode");
        var overworld = level.getServer().overworld();
        String output = OreDropMapping.output(entries, block, mode, overworld.getSeed(),
                Math.floorDiv(overworld.getOverworldClockTime(), 24000),
                mode.equals("mining") && !entries.isEmpty() ? level.getRandom().nextInt(entries.size()) : 0);
        if (output != null) {
            Identifier id = Rules.id(output);
            if (id == null) return original;
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) return original;
            int count = Rules.i("ore_drops", "base_count");
            if (Rules.b("ore_drops", "fortune")) {
                int fortune = EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool);
                count *= Math.max(1, level.getRandom().nextInt(fortune + 2));
            }
            List<ItemStack> result = new ArrayList<>();
            while (count > 0) { int size = Math.min(count, item.getDefaultInstance().getMaxStackSize()); result.add(new ItemStack(item, size)); count -= size; }
            return result;
        }
        return original;
    }
    public static boolean growthSleeps(ServerLevel level, BlockState state) {
        if (!Rules.on("growth_sleep") || !Rules.matches(state, Rules.list("growth_sleep", "target_blocks"))) return false;
        return WorldState.get(level.getServer()).daily("growth_sleep");
    }
    public static boolean dropper(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos target = pos.relative(state.getValue(DropperBlock.FACING));
        if (level.getBlockState(target).isAir() || level.getBlockEntity(target) instanceof Container) return false;
        if (level.getBlockEntity(pos) instanceof Container inventory) breakInto(level, target, inventory, "dropper_breaker");
        return true;
    }
    public static void hopper(ServerLevel level, BlockPos pos, BlockState state, HopperBlockEntity inventory) {
        if (!Rules.on("hopper_breaker") || !state.getValue(HopperBlock.ENABLED) || level.getGameTime() % Rules.i("hopper_breaker", "interval") != 0) return;
        BlockPos target = pos.above();
        if (!(level.getBlockEntity(target) instanceof Container)) breakInto(level, target, inventory, "hopper_breaker");
    }
    private static boolean breakInto(ServerLevel level, BlockPos pos, Container inventory, String feature) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || Rules.matches(state, Rules.list(feature, "excluded_blocks"))) return false;
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        if (Rules.b(feature, "require_tool") && state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return false;
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, tool);
        List<ItemStack> simulated = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) simulated.add(inventory.getItem(slot).copy());
        for (ItemStack drop : drops) {
            int remaining = drop.getCount();
            for (int slot = 0; slot < simulated.size() && remaining > 0; slot++) {
                ItemStack current = simulated.get(slot);
                if (!inventory.canPlaceItem(slot, drop) || !current.isEmpty() && !ItemStack.isSameItemSameComponents(current, drop)) continue;
                int capacity = Math.min(inventory.getMaxStackSize(), drop.getMaxStackSize()) - current.getCount();
                int moved = Math.min(remaining, Math.max(0, capacity));
                if (moved > 0) { if (current.isEmpty()) simulated.set(slot, drop.copyWithCount(moved)); else current.grow(moved); remaining -= moved; }
            }
            if (remaining > 0) return false;
        }
        if (!level.destroyBlock(pos, false)) return false;
        for (int slot = 0; slot < simulated.size(); slot++) inventory.setItem(slot, simulated.get(slot));
        inventory.setChanged(); return true;
    }
    public static boolean collectLava(ServerPlayer player, InteractionHand hand) {
        if (!Rules.on("melting_bucket") || !Gameplay.eligible(player) || !Rules.matches(player.getItemInHand(hand), Rules.list("melting_bucket", "target_items"))) return false;
        var level = player.level(); var start = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(player.getLookAngle().scale(player.blockInteractionRange())), ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !level.mayInteract(player, hit.getBlockPos()) || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), player.getItemInHand(hand))) return false;
        var source = level.getFluidState(hit.getBlockPos());
        if (!source.isSource() || source.getType() != Fluids.LAVA) return false;
        BlockState block = level.getBlockState(hit.getBlockPos());
        if (!(block.getBlock() instanceof BucketPickup pickup) || pickup.pickupBlock(player, level, hit.getBlockPos(), block).isEmpty()) return false;
        player.getItemInHand(hand).consume(1, player);
        BlockPos target = Rules.b("melting_bucket", "place_below") ? player.blockPosition().below() : player.blockPosition();
        if (!level.getBlockState(target).isAir()) level.destroyBlock(target, true, player);
        level.setBlockAndUpdate(target, Blocks.LAVA.defaultBlockState());
        return true;
    }
}
