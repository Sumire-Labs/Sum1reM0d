package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.*;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class VisualBlocks {
    private VisualBlocks() {}
    public static BlockState replace(BlockState state) {
        if (!ClientGameState.received()) return state;
        if (state.is(Blocks.GRASS_BLOCK) && ClientGameState.daily("upside_down_grass")) return ModContent.upsideDownGrass().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY, state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY));
        if (ClientGameState.daily("diamond_disguise") && Rules.matches(state, ClientRules.list("diamond_disguise", "target_blocks"))) {
            var id = Rules.id(ClientRules.text("diamond_disguise", "replacement"));
            if (id != null) return BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.DIAMOND_ORE).defaultBlockState();
        }
        if (ClientGameState.daily("furnace_textures") && (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER))) {
            List<String> choices = ClientRules.list("furnace_textures", "candidate_blocks");
            if (choices.isEmpty()) return state;
            int index = state.is(Blocks.FURNACE) ? 0 : state.is(Blocks.BLAST_FURNACE) ? 1 : 2;
            int[][] permutations = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
            if (choices.size() == 3) index = permutations[Math.floorMod((int) ClientGameState.number("furnacePermutation", 0), 6)][index];
            else {
                var shuffled = new java.util.ArrayList<>(choices);
                java.util.Collections.shuffle(shuffled, new java.util.Random((long) ClientGameState.number("day", 0) * 0x9E3779B97F4A7C15L ^ choices.hashCode() ^ (long) ClientGameState.number("furnacePermutation", 0)));
                choices = shuffled; index %= choices.size();
            }
            var id = Rules.id(choices.get(index));
            if (id == null) return state;
            var replacement = BuiltInRegistries.BLOCK.getOptional(id).orElse(state.getBlock());
            if (replacement == Blocks.FURNACE || replacement == Blocks.BLAST_FURNACE || replacement == Blocks.SMOKER)
                replacement = BuiltInRegistries.BLOCK.getValue(ModContent.id("visual_" + BuiltInRegistries.BLOCK.getKey(replacement).getPath()));
            BlockState result = replacement.defaultBlockState();
            if (result.hasProperty(AbstractFurnaceBlock.FACING)) result = result.setValue(AbstractFurnaceBlock.FACING, state.getValue(AbstractFurnaceBlock.FACING));
            if (result.hasProperty(AbstractFurnaceBlock.LIT)) result = result.setValue(AbstractFurnaceBlock.LIT, state.getValue(AbstractFurnaceBlock.LIT));
            return result;
        }
        return state;
    }
}
