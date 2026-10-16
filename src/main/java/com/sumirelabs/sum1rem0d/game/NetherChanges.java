package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class NetherChanges {
    private NetherChanges() {}
    public static List<ItemStack> loot(BlockState state, ServerLevel level, ItemStack tool, List<ItemStack> original) {
        if (Rules.on("nether_gold_blocks") && state.is(Blocks.NETHER_GOLD_ORE)) {
            var result = new ArrayList<ItemStack>();
            for (var stack : original) result.add(stack.is(Items.GOLD_NUGGET) ? new ItemStack(Items.GOLD_BLOCK, stack.getCount()) : stack);
            return result;
        }
        if (Rules.on("enchanted_leaf_apple") && (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)) && !tool.is(Items.SHEARS) && !Gameplay.silk(level, tool) && Rules.roll(level.getRandom(), "enchanted_leaf_apple")) {
            var result = new ArrayList<>(original); result.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)); return result;
        }
        return original;
    }
    public static ItemStack swapMaterial(ItemStack stack) {
        if (!CraftingMaterialSwap.ingredientSwap()) return stack;
        if (stack.is(Items.PAPER)) return stack.transmuteCopy(Items.LEATHER);
        if (stack.is(Items.LEATHER)) return stack.transmuteCopy(Items.PAPER);
        return stack;
    }
}
