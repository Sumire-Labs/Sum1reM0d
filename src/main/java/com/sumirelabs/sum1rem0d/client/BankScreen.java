package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.ExperienceAccounting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Exchanges are entered in XP points in either direction; the server revalidates the quote. */
public final class BankScreen extends Screen {
  private final Screen parent;
  private EditBox amount;
  private Button deposit, withdraw;
  private String text = "10";
  private int left, top;
  private String depositQuote = "", withdrawQuote = "";

  public BankScreen(Screen parent) {
    super(Component.literal("Sum1re XP銀行"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    left = (width - 300) / 2;
    top = Math.max(4, (height - 220) / 2);
    amount =
        addRenderableWidget(
            new EditBox(font, left + 8, top + 88, 284, 20, Component.literal("交換するXPポイント")));
    amount.setMaxLength(10);
    amount.setValue(text);
    amount.setResponder(value -> text = value);
    deposit =
        addRenderableWidget(
            Button.builder(Component.literal("XP → コイン"), b -> exchange(true))
                .bounds(left + 8, top + 136, 138, 20)
                .build());
    withdraw =
        addRenderableWidget(
            Button.builder(Component.literal("コイン → XP"), b -> exchange(false))
                .bounds(left + 154, top + 136, 138, 20)
                .build());
    addRenderableWidget(
        Button.builder(Component.literal("戻る"), b -> onClose())
            .bounds(left + 208, top + 194, 84, 20)
            .build());
    WalletScreen.request("refresh", "", 0, 0);
    quotes();
  }

  private void exchange(boolean deposit) {
    try {
      WalletScreen.request(
          deposit ? "xp_deposit" : "xp_withdraw",
          "",
          Long.parseLong(amount.getValue()),
          ClientRules.revision);
    } catch (NumberFormatException ignored) {
    }
  }

  private void quotes() {
    var state = WalletScreen.state();
    deposit.active = false;
    withdraw.active = false;
    depositQuote = "正のXPポイントを入力してください";
    withdrawQuote = "";
    try {
      long value = Long.parseLong(text);
      if (value <= 0) return;
      if (value > ClientRules.integer("xp_bank", "maximum_xp_per_transaction")) {
        depositQuote =
            "1回の上限: " + ClientRules.integer("xp_bank", "maximum_xp_per_transaction") + " XP";
        return;
      }
      if (!state.has("xpSupported") || !state.get("xpSupported").getAsBoolean()) {
        depositQuote = "XPの状態を確認してください";
        return;
      }
      for (boolean direction : new boolean[] {true, false})
        try {
          var quote =
              ExperienceAccounting.exchange(
                  WalletScreen.number(state, "xpPoints"),
                  WalletScreen.number(state, "balance"),
                  value,
                  ClientRules.integer("xp_bank", "xp_per_exchange"),
                  ClientRules.integer("xp_bank", "coins_per_exchange"),
                  ClientRules.integer("xp_bank", "fee_percent"),
                  direction,
                  ClientRules.integer("economy", "maximum_balance"));
          String line =
              (direction ? "受取: " : "支払: ") + quote.coins() + " C（手数料 " + quote.fee() + " C）";
          if (direction) {
            depositQuote = line;
            deposit.active = ClientRules.bool("xp_bank", "xp_to_coins");
          } else {
            withdrawQuote = line;
            withdraw.active = ClientRules.bool("xp_bank", "coins_to_xp");
          }
        } catch (IllegalArgumentException | ArithmeticException invalid) {
          if (direction) depositQuote = invalid.getMessage();
          else withdrawQuote = invalid.getMessage();
        }
    } catch (NumberFormatException ignored) {
    }
    if (minecraft.player != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
      deposit.active = false;
      withdraw.active = false;
      depositQuote = "取引・作業画面を閉じてから利用してください";
    }
    if (minecraft.player == null
        || minecraft.player.isCreative()
        || minecraft.player.isSpectator()) {
      deposit.active = false;
      withdraw.active = false;
      depositQuote = "サバイバルで利用できます";
    }
  }

  @Override
  public void tick() {
    quotes();
    if (!ClientRules.enabled("economy") || !ClientRules.enabled("xp_bank")) onClose();
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor g, int x, int y, float tick) {
    super.extractBackground(g, x, y, tick);
    VanillaGuiParts.panel(g, left, top, 300, 220);
    g.text(font, title, left + 8, top + 8, 0xFF404040, false);
    var state = WalletScreen.state();
    g.text(
        font,
        "残高: " + WalletScreen.number(state, "balance") + " コイン",
        left + 8,
        top + 28,
        0xFF604000,
        false);
    g.text(
        font,
        "利用可能XP: "
            + WalletScreen.number(state, "xpPoints")
            + " ポイント（Lv "
            + (minecraft.player == null ? 0 : minecraft.player.experienceLevel)
            + "）",
        left + 8,
        top + 42,
        0xFF307030,
        false);
    g.text(
        font,
        "交換単位: "
            + ClientRules.integer("xp_bank", "xp_per_exchange")
            + " XP ⇔ "
            + ClientRules.integer("xp_bank", "coins_per_exchange")
            + " C / 手数料 "
            + ClientRules.integer("xp_bank", "fee_percent")
            + "%",
        left + 8,
        top + 58,
        0xFF404040,
        false);
    g.text(font, "交換するXPポイント（レベルではありません）", left + 8, top + 76, 0xFF404040, false);
    g.text(
        font,
        font.plainSubstrByWidth(depositQuote, 284),
        left + 8,
        top + 112,
        deposit.active ? 0xFF307030 : 0xFF904040,
        false);
    g.text(
        font,
        font.plainSubstrByWidth(withdrawQuote, 284),
        left + 8,
        top + 124,
        withdraw.active ? 0xFF604000 : 0xFF904040,
        false);
    g.textWithWordWrap(
        font,
        Component.literal("同じXP量を指定して、両方向に交換できます。支払い・受取はサーバーで確定します。"),
        left + 8,
        top + 164,
        284,
        0xFF404040);
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
