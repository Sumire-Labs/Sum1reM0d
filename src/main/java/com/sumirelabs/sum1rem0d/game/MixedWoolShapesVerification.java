package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Native recipe/placement/loot/save checks for A03 mixed wool slabs and stairs. */
final class MixedWoolShapesVerification {
    private MixedWoolShapesVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var level = server.overworld();
        BlockPos slabPos = new BlockPos(1860, 245, 40);
        BlockPos stairsPos = new BlockPos(1864, 245, 40);
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var slab = features.getAsJsonObject("mixed_wool_slab");
            slab.addProperty("enabled", true); slab.addProperty("recipe_enabled", true);
            slab.addProperty("recipe_output", 6); slab.addProperty("allow_mixed_colors", true);
            var stairs = features.getAsJsonObject("mixed_wool_stairs");
            stairs.addProperty("enabled", true); stairs.addProperty("recipe_enabled", true);
            stairs.addProperty("recipe_output", 4); stairs.addProperty("allow_mixed_colors", true);
            store.update(draft, store.revision());

            var slabRecipe = new ExtraCraftingRecipe("mixed_wool_slab");
            for (DyeColor color : DyeColor.values()) {
                var wool = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_wool"));
                var input = slabInput(new ItemStack(wool), new ItemStack(wool), new ItemStack(wool));
                var result = slabRecipe.assemble(input);
                require(slabRecipe.matches(input, level) && result.is(ExtraContent.item("mixed_wool_slab"))
                        && result.getCount() == 6
                        && result.get(DataComponents.DYED_COLOR).rgb() == (color.getTextureDiffuseColor() & 0xFFFFFF),
                        "Three " + color.getName() + " wool blocks make six slabs with the same RGB");
            }
            var mix = slabInput(
                    DyeTextiles.stack("mixed_wool", 0xFF0000),
                    wool(DyeColor.BLUE),
                    wool(DyeColor.WHITE));
            int expected = DyeTextiles.blend(List.of(0xFF0000, DyeColor.BLUE.getTextureDiffuseColor(), DyeColor.WHITE.getTextureDiffuseColor()));
            var slabResult = slabRecipe.assemble(mix);
            require(slabRecipe.matches(mix, level) && slabResult.getCount() == 6
                    && slabResult.get(DataComponents.DYED_COLOR).rgb() == expected,
                    "Mixed wool blocks and vanilla colors are averaged into the slab RGB");
            var slabGrind = AutomationProcessing.grindResult(slabResult);
            require(slabGrind.is(Items.STRING) && slabGrind.getCount() == 1,
                    "Jukebox grinder accepts a wool slab and returns one string");
            slab.addProperty("allow_mixed_colors", false); store.update(draft, store.revision());
            require(!slabRecipe.matches(mix, level), "Slab Config can reject mixed-color inputs");
            slab.addProperty("allow_mixed_colors", true); store.update(draft, store.revision());

            var stairsRecipe = new ExtraCraftingRecipe("mixed_wool_stairs");
            var left = woolStairInput();
            var right = woolStairInputMirrored();
            var stairResult = stairsRecipe.assemble(left);
            require(stairsRecipe.matches(left, level) && stairsRecipe.matches(right, level)
                    && stairResult.is(ExtraContent.item("mixed_wool_stairs")) && stairResult.getCount() == 4,
                    "Six wool blocks in either stair orientation make four stairs");
            require(stairResult.get(DataComponents.DYED_COLOR).rgb() == DyeTextiles.blend(woolColors(left)),
                    "Stair RGB averages all six input wool colors");
            var stairGrind = AutomationProcessing.grindResult(stairResult);
            require(stairGrind.is(Items.STRING) && stairGrind.getCount() == 2,
                    "Jukebox grinder accepts wool stairs and returns two strings");

            var invalidStairs = CraftingInput.of(3, 3, List.of(wool(DyeColor.WHITE), ItemStack.EMPTY, wool(DyeColor.WHITE),
                    wool(DyeColor.WHITE), ItemStack.EMPTY, ItemStack.EMPTY,
                    wool(DyeColor.WHITE), wool(DyeColor.WHITE), wool(DyeColor.WHITE)));
            require(!stairsRecipe.matches(invalidStairs, level), "Stair recipe rejects unrelated arrangements");
            stairs.addProperty("recipe_enabled", false); store.update(draft, store.revision());
            require(!stairsRecipe.matches(left, level), "Stair Config can disable its recipe independently");
            stairs.addProperty("recipe_enabled", true); store.update(draft, store.revision());

            ServerVerification.place(level, slabPos, ExtraContent.block("mixed_wool_slab").defaultBlockState());
            ServerVerification.place(level, stairsPos, ExtraContent.block("mixed_wool_stairs").defaultBlockState());
            var slabEntity = (DyedBlockEntity) level.getBlockEntity(slabPos);
            var stairsEntity = (DyedBlockEntity) level.getBlockEntity(stairsPos);
            require(slabEntity != null && stairsEntity != null, "Both custom shapes own persistent color block entities");
            slabEntity.applyComponentsFromItemStack(slabResult);
            stairsEntity.applyComponentsFromItemStack(stairResult);
            require(slabEntity.color() == expected && stairsEntity.color() == stairResult.get(DataComponents.DYED_COLOR).rgb(),
                    "Placing colored slab and stair items applies their RGB to the block entities");
            var savedSlab = slabEntity.saveWithoutMetadata(level.registryAccess());
            var loadedSlab = new TextileEntity("mixed_wool_slab", slabPos, level.getBlockState(slabPos));
            loadedSlab.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedSlab));
            require(loadedSlab.color() == expected, "Slab RGB survives native block-entity save/load");
            var slabDrops = Block.getDrops(level.getBlockState(slabPos), level, slabPos, slabEntity, null, ItemStack.EMPTY);
            var stairsDrops = Block.getDrops(level.getBlockState(stairsPos), level, stairsPos, stairsEntity, null, ItemStack.EMPTY);
            require(slabDrops.size() == 1 && slabDrops.getFirst().is(ExtraContent.item("mixed_wool_slab"))
                    && slabDrops.getFirst().get(DataComponents.DYED_COLOR).rgb() == expected,
                    "Breaking a dyed slab drops its exact RGB component");
            require(stairsDrops.size() == 1 && stairsDrops.getFirst().is(ExtraContent.item("mixed_wool_stairs"))
                    && stairsDrops.getFirst().get(DataComponents.DYED_COLOR).rgb() == stairResult.get(DataComponents.DYED_COLOR).rgb(),
                    "Breaking dyed stairs drops its exact RGB component");

            slab.addProperty("enabled", false); stairs.addProperty("enabled", false); store.update(draft, store.revision());
            require(!slabRecipe.matches(mix, level) && !stairsRecipe.matches(left, level)
                    && slabEntity.item().get(DataComponents.DYED_COLOR).rgb() == expected
                    && stairsEntity.item().get(DataComponents.DYED_COLOR).rgb() == stairResult.get(DataComponents.DYED_COLOR).rgb(),
                    "Config OFF blocks new shape recipes but preserves placed colors and itemization");
            passed.accept("Mixed wool slab/stairs recipes for all 16 colors, RGB mixing, both stair orientations, Config toggles, save/load and loot");
        } finally {
            store.update(original, store.revision());
            var slabEntity = level.getBlockEntity(slabPos); if (slabEntity != null) slabEntity.setRemoved();
            var stairsEntity = level.getBlockEntity(stairsPos); if (stairsEntity != null) stairsEntity.setRemoved();
        }
    }

    private static CraftingInput woolStairInput() {
        return CraftingInput.of(3, 3, List.of(wool(DyeColor.RED), ItemStack.EMPTY, ItemStack.EMPTY,
                wool(DyeColor.BLUE), wool(DyeColor.WHITE), ItemStack.EMPTY,
                wool(DyeColor.GREEN), wool(DyeColor.BLACK), wool(DyeColor.YELLOW)));
    }
    private static CraftingInput slabInput(ItemStack a, ItemStack b, ItemStack c) {
        return CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, a, b, c));
    }
    private static CraftingInput woolStairInputMirrored() {
        return CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ItemStack.EMPTY, wool(DyeColor.RED),
                ItemStack.EMPTY, wool(DyeColor.WHITE), wool(DyeColor.BLUE),
                wool(DyeColor.YELLOW), wool(DyeColor.BLACK), wool(DyeColor.GREEN)));
    }
    private static ItemStack wool(DyeColor color) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_wool")));
    }
    private static List<Integer> woolColors(CraftingInput input) {
        var colors = new ArrayList<Integer>();
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) colors.add(DyeTextiles.color(stack));
        return colors;
    }
}
