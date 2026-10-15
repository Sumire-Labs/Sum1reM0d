package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit local two-client verification; restores temporary state before disconnect saves. */
public final class MultiplayerVerification {
    private static final Set<String> NAMES = Set.of("Developer", "SumireGuest");
    private static final Map<String, ServerPlayer> actors = new HashMap<>();
    private static final Map<String, List<ItemStack>> inventories = new HashMap<>();
    private static final Map<String, UUID> ids = new HashMap<>();
    private static final Map<String, Boolean> ops = new HashMap<>();
    private static final Map<String, Boolean> diamondProgress = new HashMap<>();
    private static final Set<String> acknowledgements = new HashSet<>();
    private static final List<String> passed = new ArrayList<>();
    private static JsonObject original, checkpoint;
    private static long clock, started, swappedAt;
    private static int step, phase = -1;
    private static boolean restored, finished;
    private MultiplayerVerification() {}
    public static boolean temporaryInventory(ServerPlayer player) { return Boolean.getBoolean("sum1rem0d.multiVerify") && original != null && !restored && actors.containsKey(player.getGameProfile().name()); }
    public static boolean request(ConfigRequestPayload request, ServerPlayer player) {
        if (!Boolean.getBoolean("sum1rem0d.multiVerify") || request.update() || phase < 0 || request.requestId() != -9000L - phase) return false;
        if (NAMES.contains(player.getGameProfile().name())) acknowledgements.add(player.getGameProfile().name()); return true;
    }
    public static void tick(MinecraftServer server) {
        if (!Boolean.getBoolean("sum1rem0d.multiVerify") || finished) return;
        try {
            var players = server.getPlayerList().getPlayers();
            for (var player : players) ServerVerification.require(NAMES.contains(player.getGameProfile().name()), "Only local verification accounts may connect");
            if (original == null) {
                if (players.isEmpty()) return;
                started = System.nanoTime(); original = Sum1reM0d.config().snapshot(); checkpoint = WorldState.get(server).checkpoint(); clock = server.overworld().getOverworldClockTime();
                var safe = original.deepCopy(); for (var feature : safe.getAsJsonObject("features").entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
                Sum1reM0d.config().update(safe, Sum1reM0d.config().revision());
            }
            if (System.nanoTime() - started > 180_000_000_000L) throw new AssertionError("Multiplayer verification timeout at " + step);
            for (var player : players) if (!actors.containsKey(player.getGameProfile().name())) {
                String name = player.getGameProfile().name(); actors.put(name, player); ids.put(name, player.getUUID()); ops.put(name, server.getPlayerList().isOp(new NameAndId(player.getGameProfile())));
                diamondProgress.put(name, diamondProgress(server, player));
                var items = new ArrayList<ItemStack>(); for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) items.add(player.getInventory().getItem(slot).copy()); inventories.put(name, items);
                var savedPlayers = checkpoint.getAsJsonObject("players"); String uuid = player.getUUID().toString(); if (!savedPlayers.has(uuid)) savedPlayers.add(uuid, WorldState.get(server).checkpoint().getAsJsonObject("players").get(uuid));
            }
            if (step == 8) { if (players.isEmpty()) finish(server, true, ""); return; }
            if (players.size() != 2) return;
            var state = WorldState.get(server); var a = actors.get("Developer"); var b = actors.get("SumireGuest"); long now = server.overworld().getGameTime();
            if (phase >= 0 && now % 20 == 0 && !ready()) for (var player : players) if (!acknowledgements.contains(player.getGameProfile().name())) ServerConfigHandler.send(player, "SUMIRE_MULTI:" + phase);
            switch (step) {
                case 0 -> { time(server, 6000); for (var player : players) player.inventoryMenu.broadcastFullState(); sendPhase(0); step = 1; }
                case 1 -> {
                    if (!ready()) return;
                    for (var player : players) { for (int slot = 18; slot < 27; slot++) player.getInventory().setItem(slot, new ItemStack(player == a ? Items.DIAMOND : Items.EMERALD, player == a ? 1 : 2)); player.getInventory().setChanged(); player.inventoryMenu.broadcastFullState(); }
                    sendPhase(1); step = 2;
                }
                case 2 -> {
                    if (!ready()) return; passed.add("two real clients acknowledge seeded inventory rows");
                    var draft = Sum1reM0d.config().snapshot(); var swap = draft.getAsJsonObject("features").getAsJsonObject("inventory_swap"); for (String id : DailyEvents.FEATURES) draft.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled", id.equals("inventory_swap")); draft.getAsJsonObject("features").getAsJsonObject("daily_events").addProperty("enabled", true); draft.getAsJsonObject("features").getAsJsonObject("daily_events").addProperty("chance", 100); swap.addProperty("enabled", true); swap.addProperty("chance", 100); var selectedDay = state.checkpoint(); selectedDay.addProperty("day", Long.MIN_VALUE); state.restore(selectedDay); swap.addProperty("same_dimension", true); swap.addProperty("start_slot", 18); swap.addProperty("slots", 9); Sum1reM0d.config().update(draft, Sum1reM0d.config().revision()); state.exchangedDay(Long.MIN_VALUE); time(server, 13000); step = 3;
                }
                case 3 -> {
                    if (state.exchangedDay() != 0) return;
                    requireSwapped(a, b); swappedAt = now; sendPhase(2); step = 4;
                }
                case 4 -> {
                    if (!ready() || now - swappedAt < 40) return; requireSwapped(a, b); passed.add("selected daily event swaps rows on both connected clients and cannot repeat on the same day");
                    var draft = Sum1reM0d.config().snapshot(); var features = draft.getAsJsonObject("features"); features.getAsJsonObject("inventory_swap").addProperty("enabled", false); var names = features.getAsJsonObject("name_swap"); names.addProperty("enabled", true); names.addProperty("interval", 2); names.addProperty("chat", true); names.addProperty("tab", true); names.addProperty("nametag", true); Sum1reM0d.config().update(draft, Sum1reM0d.config().revision()); state.next("names", now + 20); step = 5;
                }
                case 5 -> {
                    if (!b.getGameProfile().name().equals(state.aliases().get(a.getUUID().toString()))) return;
                    state.next("names", now + 12000); sendPhase(3); step = 6;
                }
                case 6 -> {
                    if (!ready()) return;
                    for (var player : players) { String name = player.getGameProfile().name(); ServerVerification.require(player.getUUID().equals(ids.get(name)) && server.getPlayerList().isOp(new NameAndId(player.getGameProfile())) == ops.get(name), "Aliases preserve authentication and OP state"); }
                    passed.add("both real clients acknowledge name and Tab aliases with immutable identities and permissions"); restore(server); sendPhase(4); step = 7;
                }
                case 7 -> { if (!ready()) return; for (var player : players) ServerVerification.require(diamondProgress(server, player) == diamondProgress.get(player.getGameProfile().name()), "Temporary items must not grant diamond advancement"); passed.add("temporary inventory fixture does not change diamond advancement progress"); passed.add("both real clients acknowledge restoration of original inventories and settings"); sendPhase(5); step = 8; }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("MULTI_VERIFY failed", failure); finish(server, false, failure.toString()); }
    }
    private static void requireSwapped(ServerPlayer a, ServerPlayer b) {
        for (int slot = 18; slot < 27; slot++) ServerVerification.require(a.getInventory().getItem(slot).is(Items.EMERALD) && a.getInventory().getItem(slot).getCount() == 2 && b.getInventory().getItem(slot).is(Items.DIAMOND) && b.getInventory().getItem(slot).getCount() == 1, "Server exchange preserves exact row items and counts");
    }
    private static boolean ready() { return acknowledgements.containsAll(NAMES); }
    private static boolean diamondProgress(MinecraftServer server, ServerPlayer player) { var advancement = server.getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "story/mine_diamond")); return player.getAdvancements().getOrStartProgress(advancement).isDone(); }
    private static void sendPhase(int next) { phase = next; acknowledgements.clear(); Sum1reM0d.LOGGER.info("MULTI_VERIFY: phase {}", next); for (var player : actors.values()) ServerConfigHandler.send(player, "SUMIRE_MULTI:" + next); }
    private static void time(MinecraftServer server, long value) { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + value); server.overworld().environmentAttributes().invalidateTickCache(); server.overworld().updateSkyBrightness(); }
    private static void restore(MinecraftServer server) throws Exception {
        if (restored || original == null) return;
        for (var entry : actors.entrySet()) { var player = entry.getValue(); var items = inventories.get(entry.getKey()); for (int slot = 0; slot < items.size(); slot++) player.getInventory().setItem(slot, items.get(slot).copy()); player.getInventory().setChanged(); player.inventoryMenu.broadcastFullState(); }
        time(server, clock); var state = WorldState.get(server); state.restore(checkpoint); state.save(); Sum1reM0d.config().update(original, Sum1reM0d.config().revision()); restored = true;
        for (var player : actors.values()) Gameplay.sendState(player);
    }
    public static void leaving(ServerPlayer player) { if (Boolean.getBoolean("sum1rem0d.multiVerify") && original != null && !restored) finish(player.level().getServer(), false, "Verification player disconnected before restoration"); }
    public static void stopping(MinecraftServer server) { if (Boolean.getBoolean("sum1rem0d.multiVerify") && original != null && !finished) finish(server, false, "Server stopped before verification completed"); }
    private static void finish(MinecraftServer server, boolean success, String failure) {
        if (finished) return; finished = true;
        try { restore(server); } catch (Exception error) { success = false; failure += "; restore: " + error; }
        var report = new JsonObject(); report.addProperty("success", success); report.addProperty("failure", failure); report.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-multiplayer-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report)); } catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save multiplayer report", error); }
        server.halt(false);
    }
}
