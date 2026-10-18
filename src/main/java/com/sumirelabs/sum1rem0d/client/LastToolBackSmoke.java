package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Opt-in client regression checks for previous-tool selection when changing held items. */
final class LastToolBackSmoke {
    private static boolean finished;
    private static boolean connectStarted;
    private static JsonObject visualResult, savedPersonal;
    private static net.minecraft.client.CameraType savedCamera;
    private static long captureAt;
    private static int visualStage;
    private static long connectStartedAt;
    private LastToolBackSmoke() {}

    static void tick(Minecraft mc) {
        if (visualStage > 0) { visualTick(mc); return; }
        if (finished || !Boolean.getBoolean("sum1rem0d.lastToolBackSmoke") || !mc.isGameLoadFinished()) return;
        if (mc.player == null || mc.level == null) {
            if (!connectStarted) {
                connectStarted = true;
                connectStartedAt = System.nanoTime();
                String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25597");
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                        new ServerData("Last tool verification", address, ServerData.Type.OTHER), false, null);
            } else if (System.nanoTime() - connectStartedAt > 120_000_000_000L) {
                var timeout = new JsonObject();
                timeout.addProperty("success", false);
                timeout.addProperty("failure", "Timed out waiting for the last-tool verification server");
                complete(mc, timeout);
            }
            return;
        }
        finished = true;
        var result = new JsonObject();
        try {
            LastToolBack.clear();
            LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.WOODEN_PICKAXE));
            require(LastToolBack.lastForVerification().isEmpty(), "First observed tool is held now and is not yet the previous tool");
            LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.IRON_AXE));
            require(LastToolBack.lastForVerification().is(Items.WOODEN_PICKAXE), "Switching from tool A to tool B puts A on the back");
            LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.IRON_AXE));
            require(LastToolBack.lastForVerification().is(Items.WOODEN_PICKAXE), "Holding the same tool continuously does not replace the previous tool");
            var wornAxe = new ItemStack(Items.IRON_AXE);
            wornAxe.setDamageValue(7);
            LastToolBack.observeHeldForVerification(mc, wornAxe);
            require(LastToolBack.lastForVerification().is(Items.WOODEN_PICKAXE), "Durability changes on the current tool do not replace the previous tool");
            LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.STICK));
            require(LastToolBack.lastForVerification().is(Items.IRON_AXE) && LastToolBack.lastForVerification().getDamageValue() == 7,
                    "Switching from tool B to a non-tool puts the latest B stack on the back");
            LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.STICK));
            require(LastToolBack.lastForVerification().is(Items.IRON_AXE), "Continuing to hold a non-tool preserves the last equipped tool");
            result.addProperty("success", true);
            result.add("passed", new com.google.gson.Gson().toJsonTree(java.util.List.of(
                    "first tool is not duplicated on the back", "tool A→B displays A",
                    "same-tool and durability changes keep A", "B→non-tool displays latest B stack", "continued non-tool hold keeps B")));
        } catch (Throwable failure) {
            result.addProperty("success", false);
            result.addProperty("failure", failure.toString());
            Sum1reM0d.LOGGER.error("LAST_TOOL_BACK_SMOKE failed", failure);
        } finally {
            LastToolBack.clear();
            LastToolBack.tick(mc);
        }
        if (result.get("success").getAsBoolean()) {
            try {
                savedPersonal = com.sumirelabs.sum1rem0d.config.PersonalConfig.store().snapshot();
                var personal = savedPersonal.deepCopy(); personal.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("enabled", false); personal.getAsJsonObject("features").getAsJsonObject("last_tool_back").addProperty("enabled", true);
                personal.getAsJsonObject("features").getAsJsonObject("last_tool_back").addProperty("rotation", 100);
                personal.getAsJsonObject("features").getAsJsonObject("last_tool_back").addProperty("scale", .60);
                com.sumirelabs.sum1rem0d.config.PersonalConfig.store().update(personal, com.sumirelabs.sum1rem0d.config.PersonalConfig.store().revision());
                var rules = com.sumirelabs.sum1rem0d.config.ClientRules.snapshot(); rules.getAsJsonObject("features").getAsJsonObject("second_offhand").addProperty("enabled", true);
                com.sumirelabs.sum1rem0d.config.ClientRules.accept(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(0,false,rules.toString(),"",0,false));
                SecondOffhandClient.accept(new com.sumirelabs.sum1rem0d.network.SecondOffhandStatePayload(mc.player.getUUID(), new ItemStack(Items.DIAMOND_AXE)));
                mc.player.getInventory().setSelectedItem(new ItemStack(Items.STICK));
                LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.DIAMOND_PICKAXE)); LastToolBack.observeHeldForVerification(mc, new ItemStack(Items.STICK));
                savedCamera = mc.options.getCameraType();
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                mc.gui.setScreen(null);
                visualResult = result; visualStage = 1; captureAt = System.nanoTime() + 3_000_000_000L;
            } catch (Exception e) { result.addProperty("success", false); result.addProperty("failure", e.toString()); complete(mc, result); }
        } else complete(mc, result);
    }
    private static void visualTick(Minecraft mc) {
        if (System.nanoTime() < captureAt) return;
        try {
            String name = visualStage == 1 ? "back-tool-colors" : "second-offhand-layout";
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
            if (visualStage == 1) {
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                visualStage = 2; captureAt = System.nanoTime() + 2_000_000_000L; return;
            }
            mc.options.setCameraType(savedCamera);
            com.sumirelabs.sum1rem0d.config.PersonalConfig.store().update(savedPersonal, com.sumirelabs.sum1rem0d.config.PersonalConfig.store().revision());
            visualStage = 0; complete(mc, visualResult);
        } catch (Exception e) { visualResult.addProperty("success", false); visualResult.addProperty("failure", e.toString()); visualStage = 0; complete(mc, visualResult); }
    }

    private static void complete(Minecraft mc, JsonObject result) {
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-last-tool-back-verification.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        Sum1reM0d.LOGGER.info("LAST_TOOL_BACK_SMOKE: completed success={}", result.get("success").getAsBoolean());
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        mc.stop();
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
