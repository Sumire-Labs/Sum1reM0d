package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native bank widgets and real anvil/enchantment screens, against the opt-in services server. */
public final class CoinServicesSmoke {
  private static boolean done, assetsOpened, assetsCaptured, assetsCleanFrame;
  private static long started, originalCoins, originalXp;
  private static int phase = -2, step, ticks;
  private static List<ItemStack> original;
  private static final List<String> passed = new ArrayList<>();

  private CoinServicesSmoke() {}

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void press(Screen screen, String text) {
    var button =
        screen.children().stream()
            .filter(c -> c instanceof Button b && b.getMessage().getString().equals(text))
            .map(c -> (Button) c)
            .findFirst()
            .orElseThrow();
    require(button.active, "Button enabled: " + text);
    button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
  }

  private static void amount(Screen screen, String value) {
    screen.children().stream()
        .filter(c -> c instanceof EditBox)
        .map(c -> (EditBox) c)
        .findFirst()
        .orElseThrow()
        .setValue(value);
  }

  private static void ack(int value) {
    WalletScreen.request("services_verify", "", value, 0);
  }

  private static void screenshot(Minecraft mc, String name) {
    mc.gui.toastManager().clear();
    Screenshot.grab(
        mc.gameDirectory,
        "sumire-coin-services-" + name + ".png",
        mc.gameRenderer.mainRenderTarget(),
        1,
        message -> {});
  }

  public static void tick(Minecraft mc) {
    if (!Boolean.getBoolean("sum1rem0d.coinServicesSmoke") || done || !mc.isGameLoadFinished())
      return;
    try {
      if (started == 0) {
        if (mc.gui.overlay() != null) return;
        started = System.nanoTime();
        String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
        ConnectScreen.startConnecting(
            new TitleScreen(),
            mc,
            ServerAddress.parseString(address),
            new ServerData("Coin services verification", address, ServerData.Type.OTHER),
            false,
            null);
        return;
      }
      if (System.nanoTime() - started > 240_000_000_000L
          || mc.gui.screen() instanceof DisconnectedScreen)
        throw new AssertionError(
            "Coin services client timeout/disconnect phase=" + phase + " step=" + step);
      if (mc.player == null || !ClientRules.received() || !ClientGameState.received()) return;
      var state = WalletScreen.state();
      if (!state.has("servicesPhase")) return;
      int next = state.get("servicesPhase").getAsInt();
      if (next < 0) return;
      if (next != phase) {
        phase = next;
        step = 0;
        ticks = 0;
        Sum1reM0d.LOGGER.info("COIN_SERVICES_SMOKE phase {}", phase);
      }
      ticks++;
      switch (phase) {
        case 0 -> {
          if (step != 0) return;
          if (Boolean.getBoolean("sum1rem0d.hudKeysSmoke")) HudKeysVerification.verify(mc, passed::add);
          original = new ArrayList<>();
          for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++)
            original.add(mc.player.getInventory().getItem(i).copy());
          originalCoins = WalletScreen.number(state, "balance");
          originalXp = WalletScreen.number(state, "xpPoints");
          ack(0);
          step = 1;
        }
        case 1 -> {
          if (Boolean.getBoolean("sum1rem0d.assetRedesignSmoke") && step == 0 && !assetsCaptured) {
            if (!assetsOpened) { AssetPreviewScreen.verify(mc); mc.gui.setScreen(new AssetPreviewScreen()); assetsOpened = true; ticks = 0; return; }
            if (!assetsCleanFrame) { if (ticks < 20) return; mc.gui.toastManager().clear(); assetsCleanFrame = true; ticks = 0; return; }
            if (ticks < 2) return;
            screenshot(mc,"16x16-assets"); mc.gui.setScreen(null); assetsCaptured = true;
            passed.add("native soap, coin, banknote bundle and mud ball models resolve dedicated 16x16 textures");
          }
          if (step == 0) {
            if (mc.gui.screen() != null) return;
            mc.gui.toastManager().clear();
            mc.gui.setScreen(new WalletScreen(null));
            step = 1;
            ticks = 0;
            return;
          }
          if (step == 1 && ticks > 10) {
            press(mc.gui.screen(), "XP銀行");
            step = 2;
            ticks = 0;
            return;
          }
          if (step == 2 && ticks > 15) {
            require(mc.gui.screen() instanceof BankScreen, "Wallet opens native bank screen");
            require(
                WalletScreen.number(state, "xpPoints") == 1395
                    && WalletScreen.number(state, "balance") == 500,
                "Bank receives actual XP and balance");
            screenshot(mc, "bank");
            amount(mc.gui.screen(), "100");
            step = 3;
            ticks = 0;
            return;
          }
          if (step == 3 && ticks > 2) {
            press(mc.gui.screen(), "XP → コイン");
            passed.add(
                "native wallet button opens bank and native bank button submits XP conversion");
            step = 4;
          }
        }
        case 2 -> {
          if (!(mc.gui.screen() instanceof BankScreen)) return;
          if (step == 0) {
            require(
                WalletScreen.number(state, "xpPoints") == 1295
                    && WalletScreen.number(state, "balance") == 600
                    && mc.player.experienceLevel == 29,
                "XP conversion updates authoritative points, level and currency");
            screenshot(mc, "bank-converted");
            amount(mc.gui.screen(), "100");
            step = 1;
            ticks = 0;
            return;
          }
          if (step == 1 && ticks > 2) {
            press(mc.gui.screen(), "コイン → XP");
            passed.add(
                "converted XP level and balance synchronize; reverse conversion uses bank button");
            step = 2;
          }
        }
        case 3 -> {
          if (!(mc.gui.screen() instanceof AnvilScreen screen) || ticks < 15) return;
          if (step == 0) {
            amount(screen, "コイン払いの剣");
            step = 1;
            ticks = 0;
            return;
          }
          if (step != 1
              || screen.getMenu().getCost() != 1
              || !screen.getMenu().getSlot(2).hasItem()) return;
          require(
              mc.player.experienceLevel == 0
                  && WalletScreen.number(state, "balance") == 500
                  && screen.getMenu().getSlot(2).mayPickup(mc.player),
              "Native anvil client allows coin payment at zero XP: level="
                  + mc.player.experienceLevel
                  + " balance="
                  + WalletScreen.number(state, "balance")
                  + " cost="
                  + screen.getMenu().getCost());
          screenshot(mc, "anvil");
          mc.gameMode.handleContainerInput(
              screen.getMenu().containerId, 2, 0, ContainerInput.PICKUP, mc.player);
          passed.add(
              "native anvil name field and result slot are usable with coins at XP level zero");
          step = 2;
        }
        case 4 -> {
          if (!(mc.gui.screen() instanceof EnchantmentScreen screen) || ticks < 15 || step != 0)
            return;
          require(
              mc.player.experienceLevel == 0
                  && WalletScreen.number(state, "balance") == 490
                  && screen.getMenu().costs[2] == 30,
              "Native enchanting screen has level-30 offer with zero player XP");
          screenshot(mc, "enchant");
          int x = ((ContainerScreenAccess) screen).sum1rem0d$leftPos() + 80,
              y = (screen.height - 166) / 2 + 14 + 19 * 2 + 8;
          require(
              screen.mouseClicked(
                  new MouseButtonEvent(
                      x, y, new MouseButtonInfo(org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT, 0)),
                  false),
              "Actual enchantment screen mouse callback accepts third offer with coins");
          passed.add("actual vanilla enchantment mouse callback accepts third offer without XP");
          step = 1;
        }
        case 5 -> {
          if (!(mc.gui.screen() instanceof EnchantmentScreen screen) || ticks < 10 || step != 0)
            return;
          require(
              WalletScreen.number(state, "balance") == 460
                  && mc.player.experienceLevel == 0
                  && screen.getMenu().getSlot(1).getItem().isEmpty()
                  && !screen.getMenu().getSlot(0).getItem().getEnchantments().isEmpty(),
              "Native enchant result, lapis, coin balance and unchanged XP synchronize");
          screenshot(mc, "enchanted");
          passed.add(
              "server result consumes exactly 30 coins and three lapis while retaining zero XP");
          ack(5);
          step = 1;
        }
        case 6 -> {
          if (step != 0) return;
          require(
              original != null
                  && WalletScreen.number(state, "balance") == originalCoins
                  && WalletScreen.number(state, "xpPoints") == originalXp,
              "Original wallet and XP restored");
          for (int i = 0; i < original.size(); i++)
            require(
                ItemStack.matches(original.get(i), mc.player.getInventory().getItem(i)),
                "Original inventory restored");
          passed.add("original inventory, wallet and XP are restored after native verification");
          ack(6);
          step = 1;
        }
        case 7 -> finish(mc, true, "");
        default -> {}
      }
    } catch (Throwable failure) {
      Sum1reM0d.LOGGER.error("COIN_SERVICES_SMOKE failed", failure);
      finish(mc, false, failure.toString());
    }
  }

  private static void finish(Minecraft mc, boolean success, String failure) {
    done = true;
    var report = new JsonObject();
    report.addProperty("success", success);
    report.addProperty("failure", failure);
    report.add("passed", new Gson().toJsonTree(passed));
    try {
      Files.writeString(
          mc.gameDirectory.toPath().resolve("sum1rem0d-coin-services-verification.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(report));
    } catch (Exception problem) {
      Sum1reM0d.LOGGER.error("Cannot save coin services client result", problem);
    }
    mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
    mc.stop();
  }
}
