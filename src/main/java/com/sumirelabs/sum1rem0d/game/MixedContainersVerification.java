package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Native verifier for mixed bundle dyeing and content preservation. */
final class MixedContainersVerification {
    private MixedContainersVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var cfg = features.getAsJsonObject("mixed_containers");
            cfg.addProperty("enabled", true);
            cfg.addProperty("bundle_enabled", true);
            cfg.addProperty("shulker_enabled", true);
            cfg.addProperty("maximum_dyes", 8);
            store.update(draft, store.revision());

            BundleContents.Mutable contents;
            //? if >=26.3 {
            contents = new BundleContents.Mutable();
            //?} else {
            contents = new BundleContents.Mutable(BundleContents.EMPTY);
            //?}
            contents.tryInsert(new ItemStack(Items.DIAMOND, 2));
            var redDye = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_dye")));
            var blueDye = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("blue_dye")));
            var recipe = new MixedContainersRecipe();
            var bundleIds = new java.util.ArrayList<String>();
            bundleIds.add("bundle");
            for (var color : net.minecraft.world.item.DyeColor.values()) bundleIds.add(color.getName() + "_bundle");
            int testedBundles = 0;
            CraftingInput lastInput=null;ItemStack lastResult=ItemStack.EMPTY;
            for (String bundleId : bundleIds) {
                var item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(bundleId));
                if (item == Items.AIR) continue;
                var bundle = new ItemStack(item);
                bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
                var input = CraftingInput.of(3, 1, List.of(bundle, redDye, blueDye));
                require(recipe.matches(input, server.overworld()), "Every vanilla bundle variant accepts arbitrary mixed dyes: " + bundleId);
                var result = recipe.assemble(input);lastInput=input;lastResult=result;
                require(result.is(item) && result.get(DataComponents.DYED_COLOR) != null,
                        "Dye output keeps the original bundle variant and receives a tint component: " + bundleId);
                require(result.get(DataComponents.BUNDLE_CONTENTS).equals(bundle.get(DataComponents.BUNDLE_CONTENTS)),
                        "Dyeing preserves the exact contents of " + bundleId);
                testedBundles++;
            }
            require(testedBundles > 0, "At least the native base bundle is registered");
            verifyBundleItemDefinitions(bundleIds);
            passed.accept("All " + testedBundles + " registered vanilla bundle variants accept mixed dyes, preserve contents, and have conditional color models");

            var pos = new net.minecraft.core.BlockPos(1790, 245, 0);
            var shulker = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_shulker_box")));
            int rgb = 0x3456AB;
            shulker.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("red_shulker_box"));
            ServerVerification.place(server.overworld(),pos,block.defaultBlockState());
            var placed = (ShulkerBoxBlockEntity)server.overworld().getBlockEntity(pos);
            placed.applyComponentsFromItemStack(shulker);
            placed.setItem(0, new ItemStack(Items.DIAMOND, 3));
            require(((ShulkerBoxColorAccess) placed).sum1rem0d$mixedRgb() == rgb,
                    "Placing a dyed shulker stores custom RGB on its block entity");
            var diskTag = placed.saveWithoutMetadata(server.registryAccess());
            var reloaded = new ShulkerBoxBlockEntity(pos, block.defaultBlockState());
            reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), diskTag));
            require(((ShulkerBoxColorAccess) reloaded).sum1rem0d$mixedRgb() == rgb,
                    "Custom shulker RGB survives full disk save/load");
            require(reloaded.getItem(0).is(Items.DIAMOND) && reloaded.getItem(0).getCount() == 3,
                    "Shulker inventory contents survive full disk save/load alongside mixed RGB");
            var updateTag = placed.getUpdateTag(server.registryAccess());
            var clientSync = new ShulkerBoxBlockEntity(pos, block.defaultBlockState());
            clientSync.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), updateTag));
            require(updateTag.getIntOr("sum1rem0d_mixed_rgb", -1) == rgb
                    && ((ShulkerBoxColorAccess) clientSync).sum1rem0d$mixedRgb() == rgb
                    && clientSync.getItem(0).isEmpty() && placed.getUpdatePacket() != null,
                    "Shulker update tag/packet synchronizes only custom RGB, not the container inventory");
            require(reloaded.collectComponents().get(DataComponents.DYED_COLOR).equals(new DyedItemColor(rgb)),
                    "Breaking the mixed shulker can collect its RGB item component");
            passed.accept("Mixed shulker placement, custom RGB persistence, packet data and content-preserving loot component");

            cfg.addProperty("enabled", false);
            store.update(draft, store.revision());
            require(!recipe.matches(lastInput, server.overworld()) && recipe.assemble(lastInput).isEmpty(), "Config OFF blocks new mixed-container dye recipes");
            require(lastResult.get(DataComponents.BUNDLE_CONTENTS).equals(contents.toImmutable()), "Config OFF retains already dyed container contents");
            passed.accept("Mixed-container Config OFF preserves existing bundle contents");
        } finally {
            store.update(original, store.revision());
        }
    }

    private static void verifyBundleItemDefinitions(List<String> bundleIds) throws Exception {
        for (String id : bundleIds) {
            String resource = "/assets/minecraft/items/" + id + ".json";
            try (var stream = MixedContainersVerification.class.getResourceAsStream(resource)) {
                require(stream != null, "The native bundle item model override exists: " + id);
                var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                var condition = root.getAsJsonObject("model");
                require("minecraft:condition".equals(condition.get("type").getAsString())
                                && "minecraft:has_component".equals(condition.get("property").getAsString())
                                && "minecraft:dyed_color".equals(condition.get("component").getAsString()),
                        "Bundle model branches on the dyed color component: " + id);
                var nativeBranch = condition.getAsJsonObject("on_false");
                String nativeModel = "minecraft:item/" + id;
                require(nativeModel.equals(fallbackModel(nativeBranch))
                                && nativeModel.equals(selectedModel(nativeBranch, "on_false"))
                                && !hasTint(nativeBranch),
                        "Undyed " + id + " keeps its native model and has no tint overlay");
                var dyedBranch = condition.getAsJsonObject("on_true");
                require("minecraft:item/white_bundle".equals(fallbackModel(dyedBranch))
                                && "minecraft:item/white_bundle".equals(selectedModel(dyedBranch, "on_false"))
                                && "minecraft:item/white_bundle_open_back".equals(compositeModel(dyedBranch, 0))
                                && "minecraft:item/white_bundle_open_front".equals(compositeModel(dyedBranch, 2))
                                && "minecraft:bundle/selected_item".equals(compositeModel(dyedBranch, 1))
                                && hasTint(dyedBranch),
                        "Dyed " + id + " uses white base/open models with dye tint and preserves selected content display");
            }
        }
    }

    private static String fallbackModel(JsonObject select) {
        return select.getAsJsonObject("fallback").get("model").getAsString();
    }

    private static JsonObject guiCondition(JsonObject select) {
        JsonArray cases = select.getAsJsonArray("cases");
        for (var entry : cases) if ("gui".equals(entry.getAsJsonObject().get("when").getAsString()))
            return entry.getAsJsonObject().getAsJsonObject("model");
        throw new IllegalStateException("Bundle item definition has no GUI display case");
    }

    private static String selectedModel(JsonObject select, String branch) {
        return guiCondition(select).getAsJsonObject(branch).get("model").getAsString();
    }

    private static String compositeModel(JsonObject select, int index) {
        var composite = guiCondition(select).getAsJsonObject("on_true").getAsJsonArray("models").get(index).getAsJsonObject();
        return composite.has("model") ? composite.get("model").getAsString()
                : composite.has("type") ? composite.get("type").getAsString() : null;
    }

    private static boolean hasTint(JsonObject select) {
        if (hasDyeTint(select.getAsJsonObject("fallback"))) return true;
        var gui = guiCondition(select);
        if (hasDyeTint(gui.getAsJsonObject("on_false"))) return true;
        var models = gui.getAsJsonObject("on_true").getAsJsonArray("models");
        return hasDyeTint(models.get(0).getAsJsonObject()) && hasDyeTint(models.get(2).getAsJsonObject());
    }

    private static boolean hasDyeTint(JsonObject model) {
        if (!model.has("tints")) return false;
        for (var tint : model.getAsJsonArray("tints"))
            if ("minecraft:dye".equals(tint.getAsJsonObject().get("type").getAsString())) return true;
        return false;
    }
}
