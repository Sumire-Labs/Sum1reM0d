package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class CustomHud {
  private CustomHud() {}

  static void render(GuiGraphicsExtractor g) {
    var mc = Minecraft.getInstance();
    if (mc.gui.hud.isHidden()) return;
    if (ClientRules.enabled("economy") && PersonalConfig.store().bool("economy", "hud"))
      draw(
          g,
          "economy",
          Long.toString(WalletScreen.number(WalletScreen.state(), "balance")),
          0,
          false);
    if (ClientRules.enabled("cleanliness") && PersonalConfig.store().bool("cleanliness", "hud")) {
      double max = ClientRules.number("cleanliness", "maximum"),
          value = ClientGameState.number("cleanliness", max);
      draw(
          g,
          "cleanliness",
          (int) value + " / " + (int) max,
          Math.max(0, Math.min(1, value / max)),
          PersonalConfig.store().bool("cleanliness", "hud_bar"));
    }
  }

  private static int color(String feature, String key) {
    return 0xFF000000
        | Integer.parseInt(PersonalConfig.store().text(feature, key).substring(1), 16);
  }

  private static void draw(
      GuiGraphicsExtractor g, String feature, String value, double fraction, boolean bar) {
    var mc = Minecraft.getInstance();
    var config = PersonalConfig.store();
    String prefix = config.text(feature, "hud_label");
    String text = (prefix.isBlank() ? "" : prefix + " ") + value;
    int width = Math.max(bar ? 92 : 0, mc.font.width(text) + 6), height = bar ? 15 : 13;
    var p =
        HudLayout.place(
            g.guiWidth(),
            g.guiHeight(),
            width,
            height,
            config.integer(feature, "hud_anchor"),
            config.integer(feature, "hud_x"),
            config.integer(feature, "hud_y"),
            config.number(feature, "hud_scale"));
    int x = (int) Math.floor(p.x()),
        y = (int) Math.floor(p.y()),
        alpha = (int) Math.round(config.number(feature, "hud_background_opacity") * 255);
    g.pose().pushMatrix();
    g.pose().scale(p.scale());
    if (alpha > 0) g.fill(x, y, x + width, y + height, alpha << 24);
    if (bar)
      g.fill(
          x + 2,
          y + 11,
          x + 2 + (int) (fraction * 88),
          y + 13,
          color(feature, fraction < 0.5 ? "hud_low_color" : "hud_bar_color"));
    g.text(
        mc.font,
        text,
        x + 3,
        y + (bar ? 1 : 2),
        color(feature, "hud_text_color"),
        config.bool(feature, "hud_shadow"));
    g.pose().popMatrix();
  }
}
