package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Native verifier for all-category tool rules, order independence, material matching and Config OFF. */
final class PaxelVerification {
    private PaxelVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var actor = EconomyVerification.actor(server, server.overworld(), "PaxelInteractions");
        var level = server.overworld();
        var logPos = new BlockPos(1810, 245, 0);
        var pathPos = new BlockPos(1812, 245, 0);
        var oldLog = level.getBlockState(logPos);
        var oldPath = level.getBlockState(pathPos);
        try {
            PaxelContent.refreshDefaults();
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            features.getAsJsonObject("material_paxel").addProperty("enabled", true);
            store.update(draft, store.revision());

            var ids = PaxelRecipe.selectedIds(server.overworld().getSeed(), "diamond");
            require(ids.size() == 3, "Seeded paxel inputs have three selected tool ids");
            require(PaxelRecipe.selectionTable(server.overworld().getSeed()).size() == PaxelContent.MATERIALS.size() * 3
                    && PaxelContent.MATERIALS.contains("copper"), "World-seed recipe map includes every material without exposing its seed");
            var axe = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(ids.get(0))));
            var shovel = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(ids.get(1))));
            var pickaxe = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(ids.get(2))));
            var input = CraftingInput.of(2, 2, List.of(axe, ItemStack.EMPTY, shovel, pickaxe));
            var reverse = CraftingInput.of(2, 2, List.of(pickaxe, shovel, ItemStack.EMPTY, axe));
            var recipe = new PaxelRecipe("diamond");
            require(recipe.matches(input, server.overworld()) && recipe.matches(reverse, server.overworld()), "Seed-selected tool trio forms a shapeless material paxel");
            ItemStack paxel = recipe.assemble(input);
            Tool tool = paxel.get(DataComponents.TOOL);
            require(!paxel.isEmpty() && paxel.is(PaxelContent.item("diamond")) && tool != null, "Paxel has a material-specific item identity");
            require(tool.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState())
                    && tool.isCorrectForDrops(Blocks.OAK_LOG.defaultBlockState())
                    && tool.isCorrectForDrops(Blocks.DIRT.defaultBlockState()), "Paxel preserves pickaxe, axe and shovel harvest rules");
            require(paxel.getMaxDamage() == Items.DIAMOND_PICKAXE.getDefaultInstance().getMaxDamage(), "Paxel takes material durability");
            require(PaxelContent.ITEMS.stream().allMatch(EquipmentDefaults.itemNames()::contains), "Equipment default payload includes every material paxel");
            for (String material : PaxelContent.MATERIALS) {
                var materialIds = PaxelRecipe.selectedIds(server.overworld().getSeed(), material);
                require(materialIds.size() == 3 && PaxelContent.item(material) != Items.AIR,
                        "Registered material paxel and randomized recipe inputs exist for " + material);
                var materialInput = CraftingInput.of(2, 2, List.of(
                        new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(materialIds.get(0)))),
                        ItemStack.EMPTY,
                        new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(materialIds.get(1)))),
                        new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(materialIds.get(2))))));
                var materialRecipe = new PaxelRecipe(material);
                require(materialRecipe.matches(materialInput, server.overworld())
                        && materialRecipe.assemble(materialInput).is(PaxelContent.item(material)),
                        "Seed-selected input trio crafts its exact material output for " + material);
            }
            var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var encoded = DataComponentMap.CODEC.encodeStart(ops, PaxelContent.item("diamond").components()).getOrThrow();
            var decoded = DataComponentMap.CODEC.parse(ops, encoded).getOrThrow();
            require(decoded.equals(PaxelContent.item("diamond").components()), "Server-authoritative paxel defaults serialize and decode intact");
            actor.setItemInHand(InteractionHand.MAIN_HAND, paxel);
            level.setBlockAndUpdate(logPos, Blocks.OAK_LOG.defaultBlockState());
            var axeHit = new BlockHitResult(Vec3.atCenterOf(logPos), Direction.UP, logPos, false);
            var axeResult = ((PaxelItem) paxel.getItem()).useOn(new net.minecraft.world.item.context.UseOnContext(actor, InteractionHand.MAIN_HAND, axeHit));
            require(axeResult.consumesAction() && level.getBlockState(logPos).is(Blocks.STRIPPED_OAK_LOG), "Paxel delegates vanilla axe stripping: result=" + axeResult + ", block=" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(logPos).getBlock()) + ", held=" + BuiltInRegistries.ITEM.getKey(actor.getItemInHand(InteractionHand.MAIN_HAND).getItem()));
            level.setBlockAndUpdate(pathPos, Blocks.GRASS_BLOCK.defaultBlockState());
            var shovelHit = new BlockHitResult(Vec3.atCenterOf(pathPos), Direction.UP, pathPos, false);
            var shovelResult = ((PaxelItem) paxel.getItem()).useOn(new net.minecraft.world.item.context.UseOnContext(actor, InteractionHand.MAIN_HAND, shovelHit));
            require(shovelResult.consumesAction() && level.getBlockState(pathPos).is(Blocks.DIRT_PATH), "Paxel delegates vanilla shovel path creation: result=" + shovelResult + ", block=" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pathPos).getBlock()) + ", held=" + BuiltInRegistries.ITEM.getKey(actor.getItemInHand(InteractionHand.MAIN_HAND).getItem()));
            var mismatched = CraftingInput.of(2, 2, List.of(axe, ItemStack.EMPTY, shovel, new ItemStack(Items.DIAMOND_SWORD)));
            require(!recipe.matches(mismatched, server.overworld()), "Non-tool ingredients are rejected");
            passed.accept("Paxel deterministic world-seed tool trio, material output, combined harvest rules and axe/shovel interactions");

            features.getAsJsonObject("material_paxel").addProperty("enabled", false);
            store.update(draft, store.revision());
            require(!recipe.matches(input, server.overworld()) && recipe.assemble(input).isEmpty(), "Config OFF blocks new paxel crafting");
            require(tool.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Config OFF preserves an already crafted paxel");
            passed.accept("Paxel Config OFF preserves existing item behavior");
        } finally {
            store.update(original, store.revision());
            actor.discard();
            level.setBlockAndUpdate(logPos, oldLog);
            level.setBlockAndUpdate(pathPos, oldPath);
        }
    }
}
