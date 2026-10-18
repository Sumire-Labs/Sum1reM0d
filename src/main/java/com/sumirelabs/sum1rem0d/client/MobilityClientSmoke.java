package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.game.LargeCauldronBlock;
import com.sumirelabs.sum1rem0d.game.MobilityContent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

/** Opt-in client checks for scrollable large-chest menus and baked cauldron shell/liquid tint models. */
final class MobilityClientSmoke {
    private static boolean finished;
    private static boolean connectStarted;
    private static long connectStartedAt;
    private static final List<String> passed = new ArrayList<>();
    private MobilityClientSmoke() {}

    static void tick(Minecraft mc) {
        if (finished || !Boolean.getBoolean("sum1rem0d.mobilityClientSmoke") || !mc.isGameLoadFinished()) return;
        if (mc.player == null || mc.level == null) {
            if (!connectStarted) {
                connectStarted = true; connectStartedAt = System.nanoTime();
                String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25597");
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                        new ServerData("Mobility client verification", address, ServerData.Type.OTHER), false, null);
            } else if (System.nanoTime() - connectStartedAt > 120_000_000_000L) {
                finish(mc, false, "Timed out waiting for the mobility verification server");
            }
            return;
        }
        finished = true;
        var result = new JsonObject();
        Screen previous = mc.gui.screen();
        try {
            verifyCauldronModels(mc);
            verifyLowHeightChestScreens(mc);
            result.addProperty("success", true);
        } catch (Throwable failure) {
            result.addProperty("success", false);
            result.addProperty("failure", failure.toString());
            Sum1reM0d.LOGGER.error("MOBILITY_CLIENT_SMOKE failed", failure);
        } finally {
            mc.gui.setScreen(previous);
        }
        result.add("passed", new com.google.gson.Gson().toJsonTree(passed));
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-mobility-client-verification.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        Sum1reM0d.LOGGER.info("MOBILITY_CLIENT_SMOKE: completed success={}", result.get("success").getAsBoolean());
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        mc.stop();
    }

    private static void finish(Minecraft mc, boolean success, String failure) {
        finished = true;
        var result = new JsonObject();
        result.addProperty("success", success);
        if (failure != null) result.addProperty("failure", failure);
        result.add("passed", new com.google.gson.Gson().toJsonTree(passed));
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-mobility-client-verification.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception writeFailure) { throw new IllegalStateException(writeFailure); }
        Sum1reM0d.LOGGER.info("MOBILITY_CLIENT_SMOKE: completed success={}", success);
        mc.stop();
    }

    private static void verifyCauldronModels(Minecraft mc) {
        var large = MobilityContent.largeCauldron();
        var huge = MobilityContent.hugeCauldron();
        require(large != null && huge != null, "Registered large/huge cauldron blocks exist on client");
        BlockPos probePos = mc.player.blockPosition();
        requireOuterWalls(large.defaultBlockState().setValue(LargeCauldronBlock.PART, 0), probePos, mc, true, false, true, true,
                "Large X-axis first part has west/north/south outer walls only");
        requireOuterWalls(large.defaultBlockState().setValue(LargeCauldronBlock.PART, 1), probePos, mc, false, true, true, true,
                "Large X-axis second part has east/north/south outer walls only");
        requireOuterWalls(large.defaultBlockState().setValue(LargeCauldronBlock.PART, 0).setValue(LargeCauldronBlock.Z_AXIS, true), probePos, mc, true, true, true, false,
                "Large Z-axis first part has both side walls and a north end wall");
        requireOuterWalls(large.defaultBlockState().setValue(LargeCauldronBlock.PART, 1).setValue(LargeCauldronBlock.Z_AXIS, true), probePos, mc, true, true, false, true,
                "Large Z-axis second part has both side walls and a south end wall");
        for (int part = 0; part < 4; part++) {
            boolean west = part % 2 == 0, east = !west, north = part < 2, south = !north;
            requireOuterWalls(huge.defaultBlockState().setValue(LargeCauldronBlock.PART, part), probePos, mc, west, east, north, south,
                    "Huge footprint part " + part + " has walls only on its exterior perimeter");
        }

        ModelManager models = mc.getModelManager();
        var modelSet = models.getBlockStateModelSet();
        for (int level = 1; level <= 3; level++) {
            var water = large.defaultBlockState().setValue(LargeCauldronBlock.CONTENTS, LargeCauldronBlock.Contents.WATER).setValue(LargeCauldronBlock.LEVEL, level);
            var powder = huge.defaultBlockState().setValue(LargeCauldronBlock.CONTENTS, LargeCauldronBlock.Contents.POWDER_SNOW).setValue(LargeCauldronBlock.LEVEL, level);
            var lava = huge.defaultBlockState().setValue(LargeCauldronBlock.CONTENTS, LargeCauldronBlock.Contents.LAVA).setValue(LargeCauldronBlock.LEVEL, level);
            var waterModel = modelSet.get(water);
            require(waterModel != modelSet.missingModel(), "Water level " + level + " model is baked");
            require(hasTintedUpFace(waterModel, mc), "Water level " + level + " has a biome-tinted fluid face");
            require(mc.getBlockColors().getTintSource(water, 0) != null
                    && mc.getBlockColors().getTintSource(water, 0).colorInWorld(water, mc.level, probePos) != 0,
                    "Large cauldron water tint is registered in BlockColors");
            require(modelSet.get(powder) != modelSet.missingModel(), "Powder snow level " + level + " model is baked");
            require(modelSet.get(lava) != modelSet.missingModel(), "Lava level " + level + " model is baked");
        }
        passed.add("W21 model bake: shell perimeter, no internal collision wall, 1–3 fluid surfaces and biome water tint");
    }

    private static void requireOuterWalls(BlockState state, BlockPos pos, Minecraft mc,
            boolean west, boolean east, boolean north, boolean south, String message) {
        VoxelShape shape = state.getCollisionShape(mc.level, pos, CollisionContext.empty());
        require(!shape.isEmpty(), message + " shape exists");
        require(contains(shape, 0.01, 0.5, 0.5) == west, message + " west edge");
        require(contains(shape, 0.99, 0.5, 0.5) == east, message + " east edge");
        require(contains(shape, 0.5, 0.5, 0.01) == north, message + " north edge");
        require(contains(shape, 0.5, 0.5, 0.99) == south, message + " south edge");
        if (west && !east) require(!contains(shape, 0.99, 0.5, 0.5), message + " internal east seam remains open");
        if (east && !west) require(!contains(shape, 0.01, 0.5, 0.5), message + " internal west seam remains open");
        if (north && !south) require(!contains(shape, 0.5, 0.5, 0.99), message + " internal south seam remains open");
        if (south && !north) require(!contains(shape, 0.5, 0.5, 0.01), message + " internal north seam remains open");
    }

    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        for (var box : shape.toAabbs()) if (box.contains(x, y, z)) return true;
        return false;
    }

    private static boolean hasTintedUpFace(net.minecraft.client.renderer.block.dispatch.BlockStateModel model, Minecraft mc) {
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(net.minecraft.util.RandomSource.create(0L), parts);
        for (BlockStateModelPart part : parts) {
            // A model face without `cullface` belongs to the general (null direction) bucket,
            // even when its baked normal points UP, as in vanilla cauldron water templates.
            for (var quad : part.getQuads(null))
                if (quad.direction() == Direction.UP && quad.materialInfo().tintIndex() == 0) return true;
            for (var quad : part.getQuads(Direction.UP))
                if (quad.direction() == Direction.UP && quad.materialInfo().tintIndex() == 0) return true;
        }
        return false;
    }

    private static void verifyLowHeightChestScreens(Minecraft mc) {
        int width = Math.max(320, mc.getWindow().getGuiScaledWidth());
        var inventory = mc.player.getInventory();
        var single = new CopperChestScreen(MobilityContent.COPPER_CHEST_8.create(905, inventory), inventory, Component.literal("Copper chest 72"));
        mc.gui.setScreen(single); single.resize(width, 160);
        require(single.visibleRowsForVerification() == 1 && single.getMenu().getRowCount() == 8 && single.getMenu().slots.size() == 108,
                "72-slot screen keeps its full server/client menu while fitting one chest row to a short viewport");
        int fixedInventoryY = single.slotYForVerification(72);
        for (int row = 0; row < 7; row++) single.mouseScrolled(single.scrollTestXForVerification(), single.scrollTestYForVerification(), 0, -1);
        require(single.firstRowForVerification() == 7 && single.slotYForVerification(71) == 18 && single.slotYForVerification(0) < 0
                && single.slotYForVerification(72) == fixedInventoryY,
                "Scrolling reaches single-chest slot 71 while keeping player inventory fixed");

        var doubled = new CopperChestScreen(MobilityContent.COPPER_CHEST_16.create(906, inventory), inventory, Component.literal("Copper chest 144"));
        mc.gui.setScreen(doubled); doubled.resize(width, 160);
        require(doubled.visibleRowsForVerification() == 1 && doubled.getMenu().getRowCount() == 16 && doubled.getMenu().slots.size() == 180,
                "144-slot screen keeps the entire 16-row server/client menu on a short viewport");
        fixedInventoryY = doubled.slotYForVerification(144);
        for (int row = 0; row < 15; row++) doubled.mouseScrolled(doubled.scrollTestXForVerification(), doubled.scrollTestYForVerification(), 0, -1);
        require(doubled.firstRowForVerification() == 15 && doubled.slotYForVerification(143) == 18 && doubled.slotYForVerification(0) < 0
                && doubled.slotYForVerification(144) == fixedInventoryY,
                "Scrolling reaches double-chest slot 143 while keeping player inventory fixed");
        passed.add("W19 short viewport: 8/16-row menu screens scroll to every chest slot and preserve inventory coordinates");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
