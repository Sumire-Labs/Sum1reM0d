package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in native verification, run in a development world with no connected users. */
public final class ServerVerification {
    private static boolean started;
    private static boolean running;
    private static final List<net.minecraft.world.entity.Entity> SPAWNED = new ArrayList<>();
    private static final List<net.minecraft.world.level.ServerExplosion> EXPLOSIONS = new ArrayList<>();
    public static void exploded(net.minecraft.world.level.ServerExplosion explosion) { if (running) EXPLOSIONS.add(explosion); }
    static List<net.minecraft.world.level.ServerExplosion> explosions() { return List.copyOf(EXPLOSIONS); }
    public static void spawned(net.minecraft.world.entity.Entity entity) { if (running) SPAWNED.add(entity); }
    static List<net.minecraft.world.entity.Entity> spawnedIn(ServerLevel level) { return SPAWNED.stream().filter(entity -> entity.level() == level && !entity.isRemoved()).toList(); }
    private static final List<String> PASSED = new ArrayList<>();
    private record Fixture(ServerLevel level, BlockPos pos) {}
    private static final Map<Fixture, BlockState> ORIGINAL = new LinkedHashMap<>();
    private ServerVerification() {}
    public static void tick(MinecraftServer server) {
        if (!Boolean.getBoolean("sum1rem0d.serverVerify") || started) return;
        started = true;
        if (!server.getPlayerList().getPlayers().isEmpty()) throw new IllegalStateException("Verification requires an empty development server");
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        var state = WorldState.get(server); var checkpoint = state.checkpoint();
        ServerLevel level = server.overworld();
        JsonObject report = new JsonObject();
        try {
            running = true;
            var candidate = config.snapshot();
            candidate.getAsJsonObject("features").getAsJsonObject("hopper_breaker").addProperty("interval", 1);
            config.update(candidate, config.revision());
            testRegisteredConfig(server);
            testSmelting(level, state);
            testSmeltingTransfers(server, level, state);
            testBrewing(level);
            testCrafting(level);
            testLoot(level);
            testMachines(level);
            testSignals(level);
            testSourceDirections(level);
            testCircuitRefresh(level);
            testEcology(level, state);
            testFletching(server, level);
            PlayerVerification.verify(server, level, state, ServerVerification::passed);
            report.addProperty("success", true);
            Sum1reM0d.LOGGER.info("SERVER_VERIFY: all {} checks passed", PASSED.size());
        } catch (Throwable failure) {
            report.addProperty("success", false); report.addProperty("failure", failure.toString());
            Sum1reM0d.LOGGER.error("SERVER_VERIFY: failed after {} checks", PASSED.size(), failure);
        } finally {
            for (var entry : new ArrayList<>(ORIGINAL.entrySet()).reversed()) {
                var fixture = entry.getKey();
                if (fixture.level.getBlockEntity(fixture.pos) instanceof Container container) container.clearContent();
                fixture.level.setBlock(fixture.pos, entry.getValue(), 3);
            }
            running = false;
            SPAWNED.forEach(net.minecraft.world.entity.Entity::discard);
            SPAWNED.clear();
            EXPLOSIONS.clear();
            try { config.update(previous, config.revision()); } catch (Exception failure) { Sum1reM0d.LOGGER.error("Cannot restore verification config", failure); }
            state.restore(checkpoint); state.save();
            report.add("passed", new Gson().toJsonTree(PASSED));
            try { Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
            catch (Exception failure) { Sum1reM0d.LOGGER.error("Cannot save verification report", failure); }
            server.halt(false);
        }
    }
    public static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void passed(String name) { PASSED.add(name); Sum1reM0d.LOGGER.info("SERVER_VERIFY: {}", name); }
    private static BlockPos pos(int index) { return new BlockPos(index * 4, 240, 0); }
    static void place(ServerLevel level, BlockPos pos, BlockState state) {
        var fixture = new Fixture(level, pos.immutable());
        if (!ORIGINAL.containsKey(fixture)) { level.getChunkAt(pos); require(level.getBlockState(pos).isAir(), "Fixture position must be empty: " + pos); ORIGINAL.put(fixture, level.getBlockState(pos)); }
        if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
        level.setBlock(pos, state, 3);
    }
    private static AbstractFurnaceBlockEntity furnace(ServerLevel level, BlockState block) {
        place(level, pos(0), Blocks.AIR.defaultBlockState()); place(level, pos(0), block);
        var result = (AbstractFurnaceBlockEntity) level.getBlockEntity(pos(0)); require(result instanceof InstantFurnace, "Instant furnace mixin applied"); return result;
    }
    private static void testSmelting(ServerLevel level, WorldState state) throws Exception {
        state.fuelEfficiency(.2);
        var furnace = furnace(level, Blocks.FURNACE.defaultBlockState());
        furnace.setItem(0, new ItemStack(Items.SAND)); furnace.setItem(1, new ItemStack(Items.COAL, 4));
        require(furnace.getItem(2).isEmpty(), "-80% efficiency must reject four coal");
        furnace.setItem(1, new ItemStack(Items.COAL, 5));
        require(furnace.getItem(2).is(Items.GLASS), "Five coal must offer glass");
        require(furnace.getItem(0).getCount() == 1 && furnace.getItem(1).getCount() == 5, "Preview must not consume inputs");
        require(furnace.removeItem(2, 1).is(Items.GLASS), "Taking output must succeed");
        require(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "Taking glass must consume one sand and five coal");
        passed("smelting -80%: five coal per item, charged at extraction");
        state.fuelEfficiency(1.8);
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND, 9)); furnace.setItem(1, new ItemStack(Items.COAL, 5));
        for (int craft = 0; craft < 9; craft++) require(furnace.removeItem(2, 1).is(Items.GLASS), "+80% output " + craft);
        require(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "+80% must make nine glass with five coal");
        passed("smelting +80%: nine items per five coal");
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND, 9)); furnace.setItem(1, new ItemStack(Items.COAL, 5));
        require(furnace.getItem(2).is(Items.GLASS), "Preview before zero-sized extraction");
        require(furnace.removeItem(2, 0).isEmpty() && furnace.removeItem(2, -1).isEmpty(), "Nonpositive extraction returns nothing");
        require(furnace.getItem(0).getCount() == 9 && furnace.getItem(1).getCount() == 5 && ((InstantFurnace) furnace).sum1rem0d$snapshotCosts().credit() == 0, "Nonpositive extraction must not charge input, fuel or credit");
        require(furnace.removeItem(2, 1).is(Items.GLASS), "First prepaid output");
        double credit = ((InstantFurnace) furnace).sum1rem0d$snapshotCosts().credit();
        require(credit > 0 && furnace.getItem(2).is(Items.GLASS), "Fractional credit and preview before save");
        var saved = furnace.saveWithFullMetadata(level.registryAccess());
        var restored = (AbstractFurnaceBlockEntity) BlockEntity.loadStatic(pos(0), level.getBlockState(pos(0)), saved, level.registryAccess());
        require(restored != null, "Native furnace NBT reload"); restored.setLevel(level);
        require(!((InstantFurnace) restored).sum1rem0d$isPreview() && Math.abs(((InstantFurnace) restored).sum1rem0d$snapshotCosts().credit() - credit) < 1e-9, "Reload preserves credit and discards saved virtual output");
        for (int craft = 0; craft < 8; craft++) require(restored.removeItem(2, 1).is(Items.GLASS), "Prepaid output after reload " + craft);
        require(restored.getItem(0).isEmpty() && restored.getItem(1).isEmpty() && restored.getItem(2).isEmpty(), "Reload still totals nine glass per five coal without duplicated preview");
        passed("nonpositive extraction is free of side effects and native NBT preserves prepaid credit");
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND)); furnace.setItem(1, new ItemStack(Items.COAL));
            require(furnace.getItem(2).is(Items.GLASS), "Preview before disabling instant smelting");
            var draft = config.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("instant_smelting").addProperty("enabled", false); config.update(draft, config.revision());
            require(furnace.getItem(2).isEmpty() && furnace.getItem(0).getCount() == 1 && furnace.getItem(1).getCount() == 1, "Disabling discards unpaid preview without consuming inputs");
            config.update(previous, config.revision());
            require(furnace.removeItem(2, 1).is(Items.GLASS) && furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "Reenabled smelting charges exactly once");
            passed("instant smelting disable and reenable preserves inputs and prevents free output");
        } finally { config.update(previous, config.revision()); }
        state.fuelEfficiency(1);
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.RAW_IRON)); furnace.setItem(1, new ItemStack(Items.COAL));
        require(furnace.getItem(2).isEmpty(), "Ordinary furnace must exclude blast furnace recipes");
        furnace = furnace(level, Blocks.BLAST_FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.RAW_IRON)); furnace.setItem(1, new ItemStack(Items.COAL));
        require(furnace.removeItem(2, 1).is(Items.IRON_INGOT), "Blast furnace must smelt iron");
        furnace = furnace(level, Blocks.SMOKER.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.PORKCHOP)); furnace.setItem(1, new ItemStack(Items.CHARCOAL));
        require(furnace.removeItem(2, 1).is(Items.COOKED_PORKCHOP), "Smoker must cook food");
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.OAK_LOG)); furnace.setItem(1, new ItemStack(Items.COAL));
        require(furnace.removeItem(2, 1).is(Items.CHARCOAL) && furnace.getItem(1).isEmpty(), "Charcoal still costs fuel");
        passed("exclusive recipes, smoker, blast furnace and charcoal");
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND)); furnace.setItem(1, new ItemStack(Items.COAL));
        require(furnace.getItem(2).is(Items.GLASS), "Output preview available");
        furnace.preRemoveSideEffects(pos(0), level.getBlockState(pos(0)));
        require(furnace.getItem(2).isEmpty(), "Breaking furnace must not drop unpaid preview");
        passed("unpaid preview is discarded on block removal");
        state.fuelEfficiency(.2);
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState());
        if (NeoTransactionChecks.verify(furnace)) passed("NeoForge transaction rollback, commit and duplicate prevention");
        state.fuelEfficiency(1);
    }
    private static void testSmeltingTransfers(MinecraftServer server, ServerLevel level, WorldState state) throws Exception {
        var player = new PlayerVerification.TestPlayer(server, level, "SumireTransfer");
        var inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        state.fuelEfficiency(1.8);
        var furnace = furnace(level, Blocks.FURNACE.defaultBlockState());
        furnace.setItem(0, new ItemStack(Items.SAND, 9)); furnace.setItem(1, new ItemStack(Items.COAL, 5));
        var menu = new net.minecraft.world.inventory.FurnaceMenu(1, inventory, furnace, new net.minecraft.world.inventory.SimpleContainerData(4));
        require(menu.quickMoveStack(player, 2).isEmpty(), "Full inventory refuses shift-click output");
        require(furnace.getItem(0).getCount() == 9 && furnace.getItem(1).getCount() == 5 && ((InstantFurnace) furnace).sum1rem0d$snapshotCosts().credit() == 0, "Rejected shift-click must not charge materials or fuel");
        inventory.setItem(0, new ItemStack(Items.GLASS, 63));
        require(menu.quickMoveStack(player, 2).is(Items.GLASS), "Shift-click uses a partially filled output stack");
        require(inventory.getItem(0).getCount() == 64 && furnace.getItem(0).getCount() == 8 && furnace.getItem(1).getCount() == 4, "Only one available inventory space charges one operation");
        passed("native furnace shift-click refuses full inventory and fills exactly one remaining stack space without overcharging");
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            var draft = config.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("instant_smelting").addProperty("batch_per_tick", 2); config.update(draft, config.revision());
            inventory.clearContent(); state.fuelEfficiency(1);
            furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND, 8)); furnace.setItem(1, new ItemStack(Items.COAL, 8));
            menu = new net.minecraft.world.inventory.FurnaceMenu(2, inventory, furnace, new net.minecraft.world.inventory.SimpleContainerData(4));
            require(menu.quickMoveStack(player, 2).is(Items.GLASS), "Shift-click produces a bounded batch");
            require(inventory.countItem(Items.GLASS) == 2 && furnace.getItem(0).getCount() == 6 && furnace.getItem(1).getCount() == 6, "Batch limit charges exactly two operations");
            require(menu.quickMoveStack(player, 2).isEmpty() && inventory.countItem(Items.GLASS) == 2, "Repeated shift-click in the same tick cannot exceed the batch limit");
            passed("furnace shift-click batch cap also applies to repeated actions within the same game tick");
        } finally { config.update(previous, config.revision()); }
        state.fuelEfficiency(.2);
        furnace = furnace(level, Blocks.FURNACE.defaultBlockState()); furnace.setItem(0, new ItemStack(Items.SAND)); furnace.setItem(1, new ItemStack(Items.COAL, 5));
        var hopperPos = pos(0).below(); place(level, hopperPos, Blocks.HOPPER.defaultBlockState());
        var hopper = (HopperBlockEntity) level.getBlockEntity(hopperPos);
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) hopper.setItem(slot, new ItemStack(Items.STONE, 64));
        // The public pull helper expects its caller to check fullness. Exercise
        // the actual tick entry point, which owns that vanilla precondition.
        HopperBlockEntity.pushItemsTick(level, hopperPos, level.getBlockState(hopperPos), hopper);
        require(furnace.getItem(0).getCount() == 1 && furnace.getItem(1).getCount() == 5, "Full hopper does not consume preview costs");
        hopper.setItem(0, ItemStack.EMPTY);
        require(HopperBlockEntity.suckInItems(level, hopper), "Hopper pulls paid instant output");
        require(hopper.getItem(0).is(Items.GLASS) && hopper.getItem(0).getCount() == 1 && furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "Hopper extraction costs exactly one sand and five coal");
        require(!HopperBlockEntity.suckInItems(level, hopper) && hopper.getItem(0).getCount() == 1, "Hopper cannot duplicate previously extracted preview");
        passed("native hopper refuses full destination without charging and extracts instant output exactly once at minus eighty percent efficiency");
        state.fuelEfficiency(1);
    }
    private static void testBrewing(ServerLevel level) {
        place(level, pos(1), Blocks.BREWING_STAND.defaultBlockState()); var brewing = (BrewingStandBlockEntity) level.getBlockEntity(pos(1));
        var potion = new ItemStack(Items.POTION); potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        brewing.setItem(0, potion); brewing.setItem(3, new ItemStack(Items.NETHER_WART)); brewing.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        require(!brewing.canPlaceItem(1, potion) && !brewing.canPlaceItem(2, potion), "Only one potion slot may accept bottles");
        for (int tick = 0; tick <= Rules.i("single_brewing", "seconds") * 20; tick++) BrewingStandBlockEntity.serverTick(level, pos(1), level.getBlockState(pos(1)), brewing);
        var contents = brewing.getItem(0).get(DataComponents.POTION_CONTENTS);
        require(contents != null && contents.potion().orElseThrow().equals(Potions.AWKWARD), "Single potion must brew");
        require(brewing.getItem(3).isEmpty() && brewing.getItem(4).isEmpty(), "Brewing consumes reagent and powder");
        passed("single-bottle brewing and one powder per operation");
    }
    private static void testCrafting(ServerLevel level) throws Exception {
        var grass = CraftingInput.of(2, 2, List.of(new ItemStack(Items.SHORT_GRASS), new ItemStack(Items.SHORT_GRASS), new ItemStack(Items.SHORT_GRASS), new ItemStack(Items.SHORT_GRASS)));
        var grassRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grass, level);
        require(grassRecipe.isPresent() && grassRecipe.get().value().assemble(grass).is(Items.COAL), "Grass recipe registered and matches");
        var soap = CraftingInput.of(3, 1, List.of(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.SUGAR), new ItemStack(Items.WATER_BUCKET)));
        var soapRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, soap, level);
        require(soapRecipe.isPresent() && soapRecipe.get().value().assemble(soap).getItem() == ModContent.soap(), "Soap recipe registered");
        require(soapRecipe.get().value().getRemainingItems(soap).get(2).is(Items.BUCKET), "Soap must return empty bucket");
        passed("configured crafting recipes and bucket remainder");
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            var draft = config.snapshot(); var recipe = draft.getAsJsonObject("features").getAsJsonObject("grass_coal");
            recipe.addProperty("output_item", "minecraft:ender_pearl"); recipe.addProperty("output_count", 16);
            config.update(draft, config.revision());
            require(grassRecipe.get().value().matches(grass, level), "Configured low-stack output must match");
            var result = grassRecipe.get().value().assemble(grass);
            require(result.is(Items.ENDER_PEARL) && result.getCount() == 16, "Valid low-stack output preserves exact quantity");
            long revision = config.revision(); var saved = config.snapshot();
            recipe.addProperty("output_count", 17);
            rejectCraftingConfig(config, draft, revision, saved, "Stack overflow must be rejected");
            recipe.addProperty("output_count", 1); recipe.addProperty("output_item", "sum1rem0d:missing_item");
            rejectCraftingConfig(config, draft, revision, saved, "Missing output must be rejected");
            recipe.addProperty("output_item", "minecraft:coal"); recipe.addProperty("input_item", "minecraft:air");
            rejectCraftingConfig(config, draft, revision, saved, "Air ingredient must be rejected");
            recipe.addProperty("input_item", "minecraft:short_grass");
            draft.getAsJsonObject("features").getAsJsonObject("cleanliness").addProperty("soap_materials", "sum1rem0d:missing_item");
            rejectCraftingConfig(config, draft, revision, saved, "Missing soap material must be rejected");
            passed("configured recipe exact stack size, missing IDs and rejected edits preserve state");
        } finally { config.update(previous, config.revision()); }
    }
    private static void testRegisteredConfig(MinecraftServer server) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); long revision = config.revision();
        try {
            String[][] invalid = {
                {"glass_damage", "target_blocks", "sum1rem0d:missing_block"},
                {"wooden_pickaxe", "target_items", "sum1rem0d:missing_item"},
                {"damage_effects", "buff_effects", "sum1rem0d:missing_effect"},
                {"knockback_mob", "mob_type", "sum1rem0d:missing_entity"},
                {"knockback_mob", "mob_type", "minecraft:arrow"},
                {"glass_damage", "target_blocks", "#sum1rem0d:missing_tag"},
                {"sleep_deprivation", "dimensions", "sum1rem0d:missing_dimension"},
                {"ore_drops", "mapping", "minecraft:coal_ore=sum1rem0d:missing_item"},
                {"ore_drops", "mapping", "sum1rem0d:missing_block=minecraft:coal"}
            };
            for (var change : invalid) {
                var draft = previous.deepCopy(); draft.getAsJsonObject("features").getAsJsonObject(change[0]).addProperty(change[1], change[2]);
                rejectCraftingConfig(config, draft, revision, previous, "Registered validation rejects " + change[0] + "." + change[1]);
            }
            passed("registered configuration rejects missing items, blocks, effects, entities, tags, dimensions, mappings and non-mob entities");
            var difficulty = server.overworld().getDifficulty();
            try {
                server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
                RegisteredConfigValidation.validate(previous, server);
                var nonMob = previous.deepCopy(); nonMob.getAsJsonObject("features").getAsJsonObject("knockback_mob").addProperty("mob_type", "minecraft:arrow");
                boolean rejectedNonMob = false;
                try { RegisteredConfigValidation.validate(nonMob, server); } catch (IllegalArgumentException expected) { rejectedNonMob = true; }
                require(rejectedNonMob, "Peaceful validation still rejects non-mob entities");
            } finally { server.setDifficulty(difficulty, true); }
            passed("peaceful difficulty accepts a valid hostile mob configuration while rejecting non-mob entities");
            var draft = previous.deepCopy(); var glass = draft.getAsJsonObject("features").getAsJsonObject("glass_damage");
            glass.addProperty("enabled", false); glass.addProperty("target_blocks", "sum1rem0d:not_installed_yet");
            config.update(draft, config.revision()); require(config.text("glass_damage", "target_blocks").equals("sum1rem0d:not_installed_yet"), "Disabled feature preserves IDs for later installation");
            glass.addProperty("enabled", true); boolean rejected = false; try { config.update(draft, config.revision()); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected && !config.enabled("glass_damage"), "Enabling feature validates its preserved IDs before saving");
            var offline = previous.deepCopy(); offline.getAsJsonObject("features").getAsJsonObject("sleep_deprivation").addProperty("dimensions", "example:future_world"); offline.getAsJsonObject("features").getAsJsonObject("glass_damage").addProperty("target_blocks", "#example:future_tag");
            RegisteredConfigValidation.validate(offline, null);
            rejected = false; try { RegisteredConfigValidation.validate(offline, server); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "World-dependent IDs allowed before world load are validated in the loaded server");
            passed("disabled feature IDs are preserved and checked on enable; world-dependent validation waits for world context");
        } finally { config.update(previous, config.revision()); }
    }
    private static void rejectCraftingConfig(com.sumirelabs.sum1rem0d.config.ConfigStore config, JsonObject draft, long revision, JsonObject saved, String message) throws Exception {
        boolean rejected = false;
        try { config.update(draft, revision); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, message);
        require(config.revision() == revision && config.snapshot().equals(saved), "Rejected config must preserve values and revision");
    }
    private static void testEcology(ServerLevel level, WorldState state) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var checkpoint = state.checkpoint();
        var server = level.getServer(); long clock = level.getOverworldClockTime(); var crop = new BlockPos(326, 241, 70);
        try {
            var draft = config.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("growth_sleep");
            options.addProperty("enabled", true); options.addProperty("moon_phases", "0,4"); options.addProperty("bonemeal", true); config.update(draft, config.revision());
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000"); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness();
            place(level, crop.below(), Blocks.FARMLAND.defaultBlockState()); place(level, crop, Blocks.WHEAT.defaultBlockState());
            var meal = new ItemStack(Items.BONE_MEAL, 2);
            require(!BoneMealItem.growCrop(meal, level, crop) && meal.getCount() == 2 && level.getBlockState(crop).getValue(CropBlock.AGE) == 0, "Resting moon prevents native bone meal without consuming it");
            require(BlockGameplay.growthSleeps(level, Blocks.OAK_SAPLING.defaultBlockState()) && !BlockGameplay.growthSleeps(level, Blocks.STONE.defaultBlockState()), "Default sapling target sleeps while unrelated blocks do not");
            options.addProperty("bonemeal", false); config.update(draft, config.revision());
            require(BoneMealItem.growCrop(meal, level, crop) && meal.getCount() == 1 && level.getBlockState(crop).getValue(CropBlock.AGE) > 0, "Bone meal override permits actual growth during resting moon");
            options.addProperty("bonemeal", true); config.update(draft, config.revision());
            var cane = crop.east(4); place(level, cane.below(), Blocks.SAND.defaultBlockState()); place(level, cane.below().north(), Blocks.WATER.defaultBlockState()); place(level, cane.above(), Blocks.AIR.defaultBlockState()); place(level, cane, Blocks.SUGAR_CANE.defaultBlockState().setValue(SugarCaneBlock.AGE, 15));
            level.getBlockState(cane).randomTick(level, cane, net.minecraft.util.RandomSource.create(42));
            require(level.getBlockState(cane.above()).isAir() && level.getBlockState(cane).getValue(SugarCaneBlock.AGE) == 15, "Native sugar cane random tick is suspended during resting moon");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 30000"); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness();
            level.getBlockState(cane).randomTick(level, cane, net.minecraft.util.RandomSource.create(42)); require(level.getBlockState(cane.above()).is(Blocks.SUGAR_CANE), "Native random growth resumes on an active moon");
            require(BoneMealItem.growCrop(meal, level, crop) && meal.isEmpty(), "Active moon permits bone meal with suspension enabled");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000"); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness();
            options.addProperty("enabled", false); config.update(draft, config.revision());
            place(level, crop, Blocks.WHEAT.defaultBlockState()); require(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, crop), "Disabling suspension restores bone meal on resting moon");
            passed("native moon-based random growth, bone meal consumption, override, sapling targeting and disable");
            var hardness = draft.getAsJsonObject("features").getAsJsonObject("hardness_swap"); hardness.addProperty("enabled", false); config.update(draft, config.revision());
            float stone = Blocks.STONE.defaultBlockState().getDestroySpeed(level, crop), deep = Blocks.DEEPSLATE.defaultBlockState().getDestroySpeed(level, crop), sand = Blocks.SAND.defaultBlockState().getDestroySpeed(level, crop);
            hardness.addProperty("enabled", true); config.update(draft, config.revision()); var daily = state.checkpoint(); daily.getAsJsonObject("daily").addProperty("hardness_swap", true); state.restore(daily);
            require(Blocks.STONE.defaultBlockState().getDestroySpeed(level, crop) == deep && Blocks.DEEPSLATE.defaultBlockState().getDestroySpeed(level, crop) == stone && Blocks.SAND.defaultBlockState().getDestroySpeed(level, crop) == sand, "Native stone and deepslate hardness swap while unrelated hardness stays unchanged");
            hardness.addProperty("second_block", "minecraft:sand"); config.update(draft, config.revision());
            require(Blocks.STONE.defaultBlockState().getDestroySpeed(level, crop) == sand && Blocks.SAND.defaultBlockState().getDestroySpeed(level, crop) == stone, "Configured replacement pair changes actual hardness");
            daily = state.checkpoint(); daily.getAsJsonObject("daily").addProperty("hardness_swap", false); state.restore(daily);
            require(Blocks.STONE.defaultBlockState().getDestroySpeed(level, crop) == stone, "Inactive day restores vanilla hardness");
            daily.getAsJsonObject("daily").addProperty("hardness_swap", true); state.restore(daily); hardness.addProperty("enabled", false); config.update(draft, config.revision());
            require(Blocks.STONE.defaultBlockState().getDestroySpeed(level, crop) == stone, "Disabled swap overrides stale daily state");
            passed("native daily hardness exchange, configurable pair, unrelated blocks and vanilla restoration");
        } finally {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness();
            config.update(previous, config.revision()); state.restore(checkpoint);
        }
    }
    private static void testLoot(ServerLevel level) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            var draft = config.snapshot();
            draft.getAsJsonObject("features").add("ore_drops", com.sumirelabs.sum1rem0d.config.ConfigSchema.defaults().getAsJsonObject("features").get("ore_drops").deepCopy());
            var options = draft.getAsJsonObject("features").getAsJsonObject("ore_drops");
            config.update(draft, config.revision());
            var pick = new ItemStack(Items.DIAMOND_PICKAXE);
            for (String pair : Rules.list("ore_drops", "mapping")) {
                var names = pair.split("="); var expected = BuiltInRegistries.ITEM.getValue(Rules.id(names[1]));
                for (String name : List.of(names[0], names[0].replace("minecraft:", "minecraft:deepslate_"))) {
                    var block = BuiltInRegistries.BLOCK.getValue(Rules.id(name));
                    require(block != Blocks.AIR, "Ore fixture exists: " + name);
                    var drops = Block.getDrops(block.defaultBlockState(), level, pos(2), null, null, pick);
                    require(drops.size() == 1 && drops.getFirst().is(expected) && drops.getFirst().getCount() == 1, "Ore swap: " + name);
                }
            }
            passed("all eight ore substitutions and their deepslate variants");
            var silk = pick.copy(); silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
            var drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, silk);
            require(drops.size() == 1 && drops.getFirst().is(Items.DIAMOND_ORE), "Silk touch retains original ore");
            options.addProperty("silk_original", false); config.update(draft, config.revision());
            drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, silk);
            require(drops.size() == 1 && drops.getFirst().is(Items.RAW_IRON), "Silk override applies configured swap");
            var fortune = pick.copy(); fortune.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
            level.getRandom().setSeed(93217L); boolean increased = false;
            for (int attempt = 0; attempt < 32; attempt++) {
                drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, fortune);
                int count = drops.stream().mapToInt(ItemStack::getCount).sum();
                require(drops.stream().allMatch(stack -> stack.is(Items.RAW_IRON)) && count >= 1 && count <= 4, "Fortune III preserves substituted type and multiplier bounds");
                increased |= count > 1;
            }
            require(increased, "Seeded fortune run must include an increased drop");
            options.addProperty("fortune", false); config.update(draft, config.revision());
            drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, fortune);
            require(drops.size() == 1 && drops.getFirst().getCount() == 1, "Fortune can be disabled");
            options.addProperty("deepslate", false); config.update(draft, config.revision());
            drops = Block.getDrops(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, pick);
            require(drops.size() == 1 && drops.getFirst().is(Items.DIAMOND), "Deepslate can retain vanilla drops");
            options.addProperty("enabled", false); config.update(draft, config.revision());
            drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos(2), null, null, pick);
            require(drops.size() == 1 && drops.getFirst().is(Items.DIAMOND), "Disabled substitution restores vanilla");
            passed("ore silk-touch policy, fortune multiplier, deepslate switch and vanilla restoration");
        } finally { config.update(previous, config.revision()); }
    }
    private static void testMachines(ServerLevel level) {
        place(level, pos(3), Blocks.DROPPER.defaultBlockState().setValue(DropperBlock.FACING, Direction.EAST));
        place(level, pos(3).east(), Blocks.STONE.defaultBlockState());
        require(BlockGameplay.dropper(level, pos(3), level.getBlockState(pos(3))), "Dropper handles non-inventory block");
        var dropper = (DropperBlockEntity) level.getBlockEntity(pos(3));
        require(level.getBlockState(pos(3).east()).isAir() && dropper.getItem(0).is(Items.COBBLESTONE), "Dropper collects broken block");
        place(level, pos(4), Blocks.HOPPER.defaultBlockState()); place(level, pos(4).above(), Blocks.STONE.defaultBlockState());
        var hopper = (HopperBlockEntity) level.getBlockEntity(pos(4)); BlockGameplay.hopper(level, pos(4), level.getBlockState(pos(4)), hopper);
        require(level.getBlockState(pos(4).above()).isAir() && hopper.getItem(0).is(Items.COBBLESTONE), "Hopper collects upper block");
        passed("dropper and hopper block breaking");
    }
    private static void testSignals(ServerLevel level) throws Exception {
        var lever = Blocks.LEVER.defaultBlockState();
        require(lever.getSignal(level, pos(5), Direction.NORTH) == 15, "Unpowered lever must emit under inversion");
        require(lever.setValue(LeverBlock.POWERED, true).getSignal(level, pos(5), Direction.NORTH) == 0, "Powered lever must stop emitting");
        require(Blocks.REDSTONE_BLOCK.defaultBlockState().getSignal(level, pos(5), Direction.UP) == 0, "Redstone block must be off");
        passed("redstone source inversion");
        for (int offset = 0; offset < 3; offset++) { place(level, pos(5).east(offset).below(), Blocks.STONE.defaultBlockState()); place(level, pos(5).east(offset), Blocks.REDSTONE_WIRE.defaultBlockState()); }
        RedstoneGameplay.wire(level, pos(5));
        for (int offset = 0; offset < 3; offset++) require(level.getBlockState(pos(5).east(offset)).getValue(RedstoneWireBlock.POWER) == 15, "Unpowered wire network must be on");
        passed("wire network inversion without self-feedback");
        var config = Sum1reM0d.config(); var before = config.snapshot();
        try {
            BlockPos floor = pos(6), wall = pos(7);
            place(level, floor.below(), Blocks.STONE.defaultBlockState()); place(level, floor, Blocks.REDSTONE_TORCH.defaultBlockState());
            place(level, wall.west(), Blocks.STONE.defaultBlockState()); place(level, wall, Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(RedstoneWallTorchBlock.FACING, Direction.EAST));
            level.getBlockState(floor).tick(level, floor, level.getRandom()); level.getBlockState(wall).tick(level, wall, level.getRandom());
            require(!level.getBlockState(floor).getValue(RedstoneTorchBlock.LIT) && !level.getBlockState(wall).getValue(RedstoneTorchBlock.LIT), "Floor and wall torches default to off");
            var candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("redstone_inversion").addProperty("blocks", false); config.update(candidate, config.revision());
            place(level, floor.below(), Blocks.REDSTONE_BLOCK.defaultBlockState()); place(level, wall.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            level.getBlockState(floor).tick(level, floor, level.getRandom()); level.getBlockState(wall).tick(level, wall, level.getRandom());
            require(level.getBlockState(floor).getValue(RedstoneTorchBlock.LIT) && level.getBlockState(wall).getValue(RedstoneTorchBlock.LIT), "External input turns inverted floor and wall torches on");
            passed("floor and wall torch inversion with per-component configuration");
            BlockPos repeater = pos(12);
            place(level, repeater.below(), Blocks.STONE.defaultBlockState()); place(level, repeater.north().below(), Blocks.STONE.defaultBlockState());
            place(level, repeater, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.WEST));
            place(level, repeater.north(), Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH).setValue(RepeaterBlock.POWERED, true));
            require(level.getBlockState(repeater).is(Blocks.BEDROCK), "Attempting to lock a repeater replaces it with bedrock");
            passed("repeater lock transition replaces the block with bedrock");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("redstone_inversion").addProperty("enabled", false); config.update(candidate, config.revision());
            BlockPos comparator = pos(14);
            place(level, comparator.below(), Blocks.STONE.defaultBlockState()); place(level, comparator.north().below(), Blocks.STONE.defaultBlockState());
            place(level, comparator.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            place(level, comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST).setValue(ComparatorBlock.MODE, net.minecraft.world.level.block.state.properties.ComparatorMode.SUBTRACT));
            place(level, comparator.north(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH).setValue(ComparatorBlock.POWERED, true));
            var side = (ComparatorBlockEntity) level.getBlockEntity(comparator.north()); side.setOutputSignal(4);
            var result = (ComparatorBlockEntity) level.getBlockEntity(comparator);
            level.getBlockState(comparator).tick(level, comparator, level.getRandom()); require(result.getOutputSignal() == 3, "Comparator division floors 15 / 4 to 3");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("comparator_division").addProperty("round_up", true); config.update(candidate, config.revision());
            level.getBlockState(comparator).tick(level, comparator, level.getRandom()); require(result.getOutputSignal() == 4, "Comparator division optionally rounds up");
            side.setOutputSignal(0); level.getBlockState(comparator).tick(level, comparator, level.getRandom()); require(result.getOutputSignal() == 15, "Zero denominator passes input through");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("comparator_division").addProperty("zero_passthrough", false); config.update(candidate, config.revision());
            level.getBlockState(comparator).tick(level, comparator, level.getRandom()); require(result.getOutputSignal() == 0, "Zero denominator optionally emits zero");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("comparator_division").addProperty("enabled", false); config.update(candidate, config.revision());
            side.setOutputSignal(4); level.getBlockState(comparator).tick(level, comparator, level.getRandom()); require(result.getOutputSignal() == 11, "Disabling division restores vanilla subtraction");
            passed("comparator division, rounding, zero denominator and vanilla restoration");
        } finally { config.update(before, config.revision()); }
    }
    private static void testSourceDirections(ServerLevel level) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        String[][] cases = {{"lever", "levers"}, {"stone_button", "buttons"}, {"stone_pressure_plate", "plates"}, {"light_weighted_pressure_plate", "plates"}, {"observer", "observers"}, {"daylight_detector", "other_sources"}, {"tripwire_hook", "other_sources"}, {"detector_rail", "other_sources"}, {"comparator", "comparators"}};
        try {
            for (int index = 0; index < cases.length; index++) {
                var kind = cases[index]; var location = pos(20 + index);
                var draft = config.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("redstone_inversion"); options.addProperty("enabled", false); options.addProperty(kind[1], true); config.update(draft, config.revision());
                var block = BuiltInRegistries.BLOCK.getValue(Rules.id("minecraft:" + kind[0])); var off = block.defaultBlockState(); var on = off;
                if (on.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) on = on.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true);
                if (on.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER)) on = on.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER, 15);
                place(level, location.below(), Blocks.STONE.defaultBlockState()); place(level, location.south(), Blocks.STONE.defaultBlockState()); place(level, location, off);
                var entity = level.getBlockEntity(location); if (entity instanceof ComparatorBlockEntity comparator) comparator.setOutputSignal(15);
                int[] weak = new int[6], direct = new int[6];
                for (Direction side : Direction.values()) { weak[side.ordinal()] = on.getSignal(level, location, side); direct[side.ordinal()] = on.getDirectSignal(level, location, side); }
                require(java.util.Arrays.stream(weak).anyMatch(value -> value > 0), "Source fixture has a vanilla powered output: " + kind[0]);
                options.addProperty("enabled", true); config.update(draft, config.revision());
                for (Direction side : Direction.values()) {
                    require(on.getSignal(level, location, side) == 0, "Powered source weak output inverts: " + kind[0] + "/" + side);
                    require(on.getDirectSignal(level, location, side) == 0, "Powered source direct output inverts: " + kind[0] + "/" + side);
                }
                if (entity instanceof ComparatorBlockEntity comparator) comparator.setOutputSignal(0);
                for (Direction side : Direction.values()) {
                    require(off.getSignal(level, location, side) == (weak[side.ordinal()] > 0 ? 15 : 0), "Unpowered source weak output preserves emission directions: " + kind[0] + "/" + side);
                    require(off.getDirectSignal(level, location, side) == (direct[side.ordinal()] > 0 ? 15 : 0), "Unpowered source direct output preserves emission directions: " + kind[0] + "/" + side);
                }
                options.addProperty(kind[1], false); config.update(draft, config.revision()); if (entity instanceof ComparatorBlockEntity comparator) comparator.setOutputSignal(15);
                for (Direction side : Direction.values()) require(on.getSignal(level, location, side) == weak[side.ordinal()] && on.getDirectSignal(level, location, side) == direct[side.ordinal()], "Component switch restores vanilla output: " + kind[0] + "/" + side);
            }
            passed("nine native redstone source types: weak/direct output on all six faces and individual switches");
        } finally { config.update(previous, config.revision()); }
    }
    private static void testCircuitRefresh(ServerLevel level) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var location = pos(32); var server = level.getServer();
        try {
            var draft = config.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("redstone_inversion"); options.addProperty("enabled", false); config.update(draft, config.revision());
            place(level, location.below(), Blocks.STONE.defaultBlockState()); place(level, location, Blocks.REDSTONE_WIRE.defaultBlockState());
            for (int tick = 0; tick < 8; tick++) RedstoneRefresh.tick(server);
            require(level.getBlockState(location).getValue(RedstoneWireBlock.POWER) == 0, "Vanilla isolated wire begins off");
            options.addProperty("enabled", true); config.update(draft, config.revision());
            for (int tick = 0; tick < 8; tick++) RedstoneRefresh.tick(server);
            require(level.getBlockState(location).getValue(RedstoneWireBlock.POWER) == 15, "Config enable recalculates existing wire without a block interaction");
            options.addProperty("enabled", false); config.update(draft, config.revision());
            for (int tick = 0; tick < 8; tick++) RedstoneRefresh.tick(server);
            require(level.getBlockState(location).getValue(RedstoneWireBlock.POWER) == 0, "Config disable restores existing vanilla wire power");
            options.addProperty("enabled", true); config.update(draft, config.revision());
            for (int tick = 0; tick < 8; tick++) RedstoneRefresh.tick(server);
            RedstoneRefresh.close(server); level.setBlock(location, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedstoneWireBlock.POWER, 0), 2);
            RedstoneRefresh.activated(level, level.getChunkAt(location));
            for (int tick = 0; tick < 8; tick++) RedstoneRefresh.tick(server);
            require(level.getBlockState(location).getValue(RedstoneWireBlock.POWER) == 15, "Activating a chunk recalculates a wire restored with stale saved power");
            passed("existing redstone wire refreshes on Config enable, disable and chunk activation without manual neighbor updates");
        } finally { config.update(previous, config.revision()); }
    }
    private static void testFletching(MinecraftServer server, ServerLevel level) throws Exception {
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "SumireVerify"), ClientInformation.createDefault());
        var menu = new FletchingMenu(1, player.getInventory()); menu.slots.getFirst().setByPlayer(new ItemStack(Items.STICK));
        require(menu.clickMenuButton(player, 0), "Fletching accepts one stick");
        int arrows = 0;
        for (int slot = 1; slot < 28; slot++) { ItemStack result = menu.slots.get(slot).remove(64); if (!result.isEmpty()) { require(result.is(Items.ARROW), "Fletching output type"); arrows += result.getCount(); } }
        require(arrows == 1000 && menu.slots.getFirst().getItem().isEmpty(), "One stick must produce exactly 1000 arrows");
        passed("fletching 1000 arrows without oversized stacks");
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            var draft = config.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("fletching").addProperty("output_count", 5000); config.update(draft, config.revision());
            player = new PlayerVerification.TestPlayer(server, level, "SumireFletchingOverflow"); player.setPos(256.5, 245, 32.5);
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            menu = new FletchingMenu(2, player.getInventory()); menu.slots.getFirst().setByPlayer(new ItemStack(Items.STICK, 2));
            require(menu.clickMenuButton(player, 0), "Configured large fletching output crafts");
            require(!menu.clickMenuButton(player, 0) && menu.slots.getFirst().getItem().getCount() == 1, "Pending output blocks a second craft without consuming the remaining stick");
            require(menu.quickMoveStack(player, 1).isEmpty(), "Full inventory refuses fletching shift-click");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            require(menu.quickMoveStack(player, 1).is(Items.ARROW) && player.getInventory().countItem(Items.ARROW) == 64 && menu.slots.get(1).getItem().getCount() == 64, "Shift-click moves one stack and refills its output slot from pending items");
            int spawnedBefore = SPAWNED.size(); menu.removed(player);
            int droppedArrows = 0, droppedSticks = 0;
            for (var entity : SPAWNED.subList(spawnedBefore, SPAWNED.size())) if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
                ItemStack stack = item.getItem(); require(stack.getCount() <= stack.getMaxStackSize(), "Closing never creates oversized dropped stacks");
                if (stack.is(Items.ARROW)) droppedArrows += stack.getCount();
                if (stack.is(Items.STICK)) droppedSticks += stack.getCount();
            }
            require(droppedArrows + player.getInventory().countItem(Items.ARROW) == 5000 && droppedSticks == 1, "Closing full-inventory fletching conserves all pending arrows and the unconsumed stick");
            int countAfterClose = SPAWNED.size(); menu.removed(player); require(SPAWNED.size() == countAfterClose, "Repeated close does not duplicate pending output");
            passed("fletching pending output, full-inventory shift-click and close preserve five thousand arrows in legal stacks without duplication");
        } finally { config.update(previous, config.revision()); }
    }
}
