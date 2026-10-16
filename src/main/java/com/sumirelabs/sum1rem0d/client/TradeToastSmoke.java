package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/** Drives the actual payload receiver and vanilla toast manager in an isolated title-screen run. */
final class TradeToastSmoke {
  private static int stage;
  private static long changed;
  private static JsonObject personal, rules, state;
  private static long revision;
  private static boolean editable;
  private static TradeInvitationToast first;
  private static final List<String> passed = new ArrayList<>();

  private TradeToastSmoke() {}

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void payload(long id) {
    var root = new JsonObject();
    root.addProperty("day", 0);
    var economy = new JsonObject();
    economy.addProperty("balance", 123);
    if (id > 0) {
      economy.addProperty("inviteRevision", id);
      economy.addProperty("inviteName", "SumireGuest");
    }
    root.add("economy", economy);
    ClientRuntime.accept(new GameStatePayload(root.toString()));
  }

  private static TradeInvitationToast toast(Minecraft mc, long id) {
    return mc.gui.toastManager()
        .getToast(TradeInvitationToast.class, TradeInvitationToast.tokenFor(id));
  }

  static void tick(Minecraft mc) {
    if (!Boolean.getBoolean("sum1rem0d.tradeToastSmoke")
        || stage < 0
        || !mc.isGameLoadFinished()
        || mc.gui.overlay() != null) return;
    if (changed != 0 && System.nanoTime() - changed < 1_000_000_000L) return;
    try {
      var manager = mc.gui.toastManager();
      switch (stage) {
        case 0 -> {
          personal = PersonalConfig.store().snapshot();
          rules = ClientRules.snapshot();
          state = ClientGameState.snapshot();
          revision = ClientRules.revision;
          editable = ClientRules.editable;
          var fixture = rules.deepCopy();
          for (var f : fixture.getAsJsonObject("features").entrySet())
            f.getValue().getAsJsonObject().addProperty("enabled", false);
          for (String id : List.of("economy", "player_trading"))
            fixture.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled", true);
          ClientRules.accept(
              new ConfigStatePayload(revision, editable, fixture.toString(), "", 0, false));
          var local = personal.deepCopy();
          local
              .getAsJsonObject("features")
              .getAsJsonObject("player_trading")
              .addProperty("toast_enabled", true);
          local
              .getAsJsonObject("features")
              .getAsJsonObject("player_trading")
              .addProperty("toast_seconds", 5);
          PersonalConfig.store().update(local, PersonalConfig.store().revision());
          mc.gui.setScreen(new TitleScreen());
          manager.clear();
          TradeInvitationToast.clear();
          payload(41);
          first = toast(mc, 41);
          require(first != null, "Actual GameState receiver queues native trade notification");
        }
        case 1 -> {
          net.minecraft.client.Screenshot.grab(
              mc.gameDirectory,
              "sumire-trade-invitation-toast.png",
              mc.gameRenderer.mainRenderTarget(),
              1,
              c -> {});
          for (int i = 0; i < 5; i++) payload(41);
          require(toast(mc, 41) == first, "Periodic repeated snapshots retain the same toast");
          first.update(manager, 1000);
          require(
              first.getWantedVisibility() == Toast.Visibility.SHOW,
              "Notification remains visible before its deadline");
          var render = new GuiRenderState();
          first.extractRenderState(new GuiGraphicsExtractor(mc, render, 0, 0), mc.font, 1000);
          int[] text = {0};
          render.forEachText(t -> text[0]++);
          require(
              text[0] == 3 && first.width() == 240 && first.height() == 48,
              "Native toast draws its title, sender and wallet hint");
          long deadline = (long) (5000 * manager.getNotificationDisplayTimeMultiplier()) + 1;
          first.update(manager, deadline);
          require(
              first.getWantedVisibility() == Toast.Visibility.HIDE,
              "Configured toast duration respects vanilla accessibility multiplier");
          passed.add(
              "actual payload receiver, repeated snapshot deduplication, native drawing and"
                  + " configured duration");
          payload(42);
          var second = toast(mc, 42);
          require(second != null && second != first, "New request creates a new toast");
          var local = PersonalConfig.store().snapshot();
          local
              .getAsJsonObject("features")
              .getAsJsonObject("player_trading")
              .addProperty("toast_enabled", false);
          PersonalConfig.store().update(local, PersonalConfig.store().revision());
          require(
              second.getWantedVisibility() == Toast.Visibility.HIDE,
              "Personal OFF hides the existing notification immediately");
          payload(43);
          require(toast(mc, 43) == null, "Disabled notifications are not queued");
          local
              .getAsJsonObject("features")
              .getAsJsonObject("player_trading")
              .addProperty("toast_enabled", true);
          PersonalConfig.store().update(local, PersonalConfig.store().revision());
          payload(43);
          require(toast(mc, 43) == null, "Reenabling does not replay old invitations");
          passed.add("personal OFF hides active toast and suppresses replay when reenabled");
          payload(44);
          var old = toast(mc, 44);
          payload(0);
          require(
              old.getWantedVisibility() == Toast.Visibility.HIDE,
              "Resolved or expired invitation closes its toast");
          TradeInvitationToast.clear();
          payload(44);
          require(
              toast(mc, 44) != null
                  && toast(mc, 44) != old
                  && old.getWantedVisibility() == Toast.Visibility.HIDE,
              "Reconnect generation prevents old toasts resurfacing with reused IDs");
          passed.add("resolved invitation dismissal and connection-safe revision reuse");
        }
        case 2 -> {
          finish(mc, true, "");
          return;
        }
      }
      stage++;
      changed = System.nanoTime();
    } catch (Throwable error) {
      Sum1reM0d.LOGGER.error("Trade toast smoke failed", error);
      finish(mc, false, error.toString());
    }
  }

  private static void finish(Minecraft mc, boolean success, String failure) {
    stage = -1;
    try {
      if (personal != null)
        PersonalConfig.store().update(personal, PersonalConfig.store().revision());
      if (rules != null)
        ClientRules.accept(
            new ConfigStatePayload(revision, editable, rules.toString(), "", 0, false));
      if (state != null) ClientGameState.accept(new GameStatePayload(state.toString()));
      TradeInvitationToast.clear();
      mc.gui.toastManager().clear();
      var result = new JsonObject();
      result.addProperty("success", success);
      result.addProperty("failure", failure);
      result.add("passed", new Gson().toJsonTree(passed));
      Files.writeString(
          mc.gameDirectory.toPath().resolve("sum1rem0d-trade-toast-verification.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(result));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    mc.stop();
  }
}
