package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;

/** Focused native-server coverage for the two free-color concrete shapes. */
final class MixedConcreteShapesVerification {
    private MixedConcreteShapesVerification() {}

    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        if (!ExtraContent.MIXED_SHAPES_AVAILABLE) {
            passed.accept("Mixed shape blocks are unavailable before Minecraft 26.3");
            return;
        }
        var config = Sum1reM0d.config();
        var original = config.snapshot();
        var draft = original.deepCopy();
        var features = draft.getAsJsonObject("features");
        for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
        for (String feature : List.of("mixed_concrete_slab", "mixed_concrete_stairs")) {
            var values = features.getAsJsonObject(feature);
            values.addProperty("enabled", true);
            values.addProperty("recipe_enabled", true);
            values.addProperty("allow_mixed_colors", true);
        }
        config.update(draft, config.revision());
        var level = server.overworld();
        var slabPos = new BlockPos(24, 245, 10);
        var stairsPos = new BlockPos(28, 245, 10);
        try {
            var red = concrete("red_concrete");
            var blue = concrete("blue_concrete");
            var custom = MixedConcrete.stack(0x13579B);
            var slabInput = CraftingInput.of(3, 1, List.of(red, blue, custom));
            var slabResult = craft(server, level, slabInput);
            int slabRgb = DyeTextiles.blend(List.of(MixedConcrete.color(red).rgb(), MixedConcrete.color(blue).rgb(), MixedConcrete.color(custom).rgb()));
            require(slabResult.is(ExtraContent.item("mixed_concrete_slab")) && slabResult.getCount() == 6
                            && slabResult.get(DataComponents.DYED_COLOR).rgb() == slabRgb,
                    "Three differently colored concrete blocks craft six slabs with their blended RGB");

            var stairsInput = CraftingInput.of(3, 3, List.of(red, ItemStack.EMPTY, ItemStack.EMPTY,
                    blue, custom, ItemStack.EMPTY,
                    concrete("green_concrete"), concrete("yellow_concrete"), concrete("white_concrete")));
            var stairsResult = craft(server, level, stairsInput);
            int stairsRgb = DyeTextiles.blend(List.of(MixedConcrete.color(red).rgb(), MixedConcrete.color(blue).rgb(),
                    MixedConcrete.color(custom).rgb(), MixedConcrete.color(concrete("green_concrete")).rgb(),
                    MixedConcrete.color(concrete("yellow_concrete")).rgb(), MixedConcrete.color(concrete("white_concrete")).rgb()));
            require(stairsResult.is(ExtraContent.item("mixed_concrete_stairs")) && stairsResult.getCount() == 4
                            && stairsResult.get(DataComponents.DYED_COLOR).rgb() == stairsRgb,
                    "Six differently colored concrete blocks craft four stairs with their blended RGB");

            ServerVerification.place(level, slabPos, ExtraContent.block("mixed_concrete_slab").defaultBlockState());
            ServerVerification.place(level, stairsPos, ExtraContent.block("mixed_concrete_stairs").defaultBlockState());
            var slabEntity = (DyedBlockEntity) level.getBlockEntity(slabPos);
            var stairsEntity = (DyedBlockEntity) level.getBlockEntity(stairsPos);
            require(slabEntity != null && stairsEntity != null, "Both concrete shape block entities are created");
            slabEntity.applyComponentsFromItemStack(slabResult);
            stairsEntity.applyComponentsFromItemStack(stairsResult);
            require(slabEntity.color() == slabRgb && stairsEntity.color() == stairsRgb,
                    "Placing both shape items stores their RGB in the block entities");

            var savedSlab = slabEntity.saveWithoutMetadata(level.registryAccess());
            var loadedSlab = new TextileEntity("mixed_concrete_slab", slabPos, level.getBlockState(slabPos));
            loadedSlab.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedSlab));
            var savedStairs = stairsEntity.saveWithoutMetadata(level.registryAccess());
            var loadedStairs = new TextileEntity("mixed_concrete_stairs", stairsPos, level.getBlockState(stairsPos));
            loadedStairs.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedStairs));
            require(loadedSlab.color() == slabRgb && loadedStairs.color() == stairsRgb,
                    "Slab and stairs RGB survive native block entity save/load");

            var slabDrops = Block.getDrops(level.getBlockState(slabPos), level, slabPos, slabEntity, null, ItemStack.EMPTY);
            var stairDrops = Block.getDrops(level.getBlockState(stairsPos), level, stairsPos, stairsEntity, null, ItemStack.EMPTY);
            require(slabDrops.size() == 1 && slabDrops.getFirst().is(ExtraContent.item("mixed_concrete_slab"))
                            && slabDrops.getFirst().get(DataComponents.DYED_COLOR).rgb() == slabRgb,
                    "Concrete slab loot returns its shape item with the saved RGB");
            require(stairDrops.size() == 1 && stairDrops.getFirst().is(ExtraContent.item("mixed_concrete_stairs"))
                            && stairDrops.getFirst().get(DataComponents.DYED_COLOR).rgb() == stairsRgb,
                    "Concrete stair loot returns its shape item with the saved RGB");
            passed.accept("Mixed concrete slab and stair recipes, RGB persistence, and component-preserving block loot");
        } finally {
            config.update(original, config.revision());
        }
    }

    private static ItemStack concrete(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)));
    }

    private static ItemStack craft(MinecraftServer server, net.minecraft.world.level.Level level, CraftingInput input) {
        var recipe = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        require(recipe != null, "Concrete shape input matches a live server crafting recipe");
        return recipe.value().assemble(input);
    }
}
