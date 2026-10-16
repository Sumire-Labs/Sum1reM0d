package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Notification only: no advancement or trade consent is awarded. */
public final class TradeInvitationToast implements Toast {
  private static final InviteNotificationTracker TRACKER = new InviteNotificationTracker();
  private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");

  public record Token(long generation, long revision) {}

  private final Token token;
  private final String sender;
  private Visibility visibility = Visibility.SHOW;

  private TradeInvitationToast(long revision, String sender) {
    token = new Token(TRACKER.generation(), revision);
    this.sender = sender;
  }

  public static void clear() {
    TRACKER.clear();
  }

  public static void receive(JsonObject root) {
    if (!root.has("economy")) return;
    var economy = root.getAsJsonObject("economy");
    if (!economy.has("inviteRevision") || !economy.has("inviteName")) return;
    long revision = economy.get("inviteRevision").getAsLong();
    if (!TRACKER.observe(revision)) return;
    if (enabled())
      Minecraft.getInstance()
          .gui.toastManager()
          .addToast(new TradeInvitationToast(revision, economy.get("inviteName").getAsString()));
  }

  private static boolean enabled() {
    return ClientRules.enabled("economy")
        && ClientRules.enabled("player_trading")
        && PersonalConfig.store().bool("player_trading", "toast_enabled");
  }

  private boolean active() {
    var state = WalletScreen.state();
    return token.generation() == TRACKER.generation()
        && state.has("inviteRevision")
        && state.get("inviteRevision").getAsLong() == token.revision();
  }

  static Token tokenFor(long revision) {
    return new Token(TRACKER.generation(), revision);
  }

  @Override
  public void update(ToastManager manager, long visibleForMs) {
    if (visibility == Visibility.HIDE) return;
    visibility =
        enabled()
                && active()
                && visibleForMs
                    < PersonalConfig.store().integer("player_trading", "toast_seconds")
                        * 1000L
                        * manager.getNotificationDisplayTimeMultiplier()
            ? Visibility.SHOW
            : Visibility.HIDE;
  }

  @Override
  public Visibility getWantedVisibility() {
    if (!enabled() || !active()) visibility = Visibility.HIDE;
    return visibility;
  }

  @Override
  public Object getToken() {
    return token;
  }

  @Override
  public int width() {
    return 240;
  }

  @Override
  public int height() {
    return 48;
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor g, Font font, long visibleForMs) {
    g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
    g.blit(
        RenderPipelines.GUI_TEXTURED,
        ModContent.id("textures/item/coin.png"),
        8,
        16,
        0,
        0,
        16,
        16,
        16,
        16,
        16,
        16);
    g.text(font, Component.translatable("toast.sum1rem0d.trade.title"), 30, 6, 0xFFFFD060, false);
    g.text(
        font,
        font.plainSubstrByWidth(
            Component.translatable("toast.sum1rem0d.trade.sender", sender).getString(),
            width() - 38),
        30,
        19,
        0xFFFFFFFF,
        false);
    g.text(
        font,
        font.plainSubstrByWidth(
            Component.translatable("toast.sum1rem0d.trade.hint").getString(), width() - 38),
        30,
        32,
        0xFFB8C6DA,
        false);
  }
}
