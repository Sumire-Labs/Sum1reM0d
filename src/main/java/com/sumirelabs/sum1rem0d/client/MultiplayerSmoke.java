package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.network.ConfigRequestPayload;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.item.*;

public final class MultiplayerSmoke {
    private static boolean done;
    private static long started;
    private static int acknowledged = -1;
    private static UUID id;
    private static boolean editable;
    private static List<ItemStack> original;
    private static final List<String> passed = new ArrayList<>();
    private MultiplayerSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.multiSmoke") || done || !mc.isGameLoadFinished()) return;
        try {
            if (started == 0) {
                if (mc.gui.overlay() != null) return; started = System.nanoTime(); String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589"); ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Two-client verification", address, ServerData.Type.OTHER), false, null); return;
            }
            if (System.nanoTime() - started > 180_000_000_000L || mc.gui.screen() instanceof DisconnectedScreen) throw new AssertionError("Multiplayer connection failed or timed out");
            if (mc.player == null || !ClientRules.message.startsWith("SUMIRE_MULTI:")) return;
            int phase = Integer.parseInt(ClientRules.message.substring("SUMIRE_MULTI:".length())); if (phase == acknowledged) return;
            String name = mc.player.getGameProfile().name(); boolean host = name.equals("Developer");
            switch (phase) {
                case 0 -> { id = mc.player.getUUID(); editable = ClientRules.editable; original = new ArrayList<>(); for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) original.add(mc.player.getInventory().getItem(slot).copy()); passed.add("original connected inventory and identity captured"); }
                case 1, 2 -> {
                    require(original != null, "Prepare phase precedes inventory changes"); boolean diamonds = phase == 1 ? host : !host;
                    for (int slot = 0; slot < original.size(); slot++) { var item = mc.player.getInventory().getItem(slot); if (slot >= 18 && slot < 27) require(item.is(diamonds ? Items.DIAMOND : Items.EMERALD) && item.getCount() == (diamonds ? 1 : 2), "Connected client receives exact inventory row"); else require(same(item, original.get(slot)), "Unselected inventory slots remain unchanged"); }
                    passed.add(phase == 1 ? "server seeded row received through native inventory packets" : "full moon exchanged row received and other slots preserved");
                }
                case 3 -> {
                    String other = host ? "SumireGuest" : "Developer"; var info = mc.getConnection().getPlayerInfo(id);
                    require(mc.player.getUUID().equals(id) && mc.player.getDisplayName().getString().contains(other) && info != null && info.getTabListDisplayName().getString().contains(other) && info.getProfile().id().equals(id) && ClientRules.editable == editable, "Connected names and Tab change while identity and permissions stay unchanged"); passed.add("real display and Tab aliases synchronize without changing UUID or permissions");
                }
                case 4 -> { for (int slot = 0; slot < original.size(); slot++) require(same(mc.player.getInventory().getItem(slot), original.get(slot)), "Original connected inventory restored"); require(mc.player.getUUID().equals(id) && ClientRules.editable == editable, "Identity and permissions restored unchanged"); passed.add("original inventory and identity restored on connected client"); }
                case 5 -> { finish(mc, true, ""); return; }
                default -> throw new AssertionError("Unexpected multiplayer phase");
            }
            require(PlatformClientNetwork.send(new ConfigRequestPayload(false, ClientRules.revision, "", -9000L - phase)), "Verification acknowledgement can be sent"); acknowledged = phase;
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("MULTI_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static boolean same(ItemStack a, ItemStack b) { return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        done = true; var report = new JsonObject(); report.addProperty("success", success); report.addProperty("failure", failure); report.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-multiplayer-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report)); } catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save multiplayer client report", error); }
        mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
}
