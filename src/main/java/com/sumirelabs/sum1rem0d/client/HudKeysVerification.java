package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.*;

/** Opt-in native checks in the existing disposable services fixture; restores all client state. */
final class HudKeysVerification {
  private HudKeysVerification() {}

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  static void verify(Minecraft mc, Consumer<String> passed) throws Exception {
    var original = PersonalConfig.store().snapshot();
    var rules = ClientRules.snapshot();
    var state = ClientGameState.snapshot();
    long revision = ClientRules.revision;
    boolean editable = ClientRules.editable, hidden = mc.gui.hud.isHidden();
    var screen = mc.gui.screen();
    var keys =
        Arrays.stream(mc.options.keyMappings)
            .filter(k -> k.getName().startsWith("key.sum1rem0d."))
            .toList();
    var saved = new ArrayList<com.mojang.blaze3d.platform.InputConstants.Key>();
    try {
      require(keys.size() == 4, "Four actions are registered in vanilla Controls");
      for (var k : keys) {
        require(
            k.getDefaultKey().equals(InputConstants.UNKNOWN), "Shortcut defaults are unassigned");
        saved.add(InputConstants.getKey(k.saveString()));
      }
      var personal = original.deepCopy();
      personal.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("enabled", true);
      PersonalConfig.store().update(personal, PersonalConfig.store().revision());
      var fixture = rules.deepCopy();
      for (var f : fixture.getAsJsonObject("features").entrySet())
        f.getValue().getAsJsonObject().addProperty("enabled", false);
      for (String id : List.of("economy", "cleanliness"))
        fixture.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled", true);
      ClientRules.accept(
          new ConfigStatePayload(revision, editable, fixture.toString(), "", 0, false));
      var data = state.deepCopy();
      data.addProperty("cleanliness", 100);
      if (!data.has("economy")) data.add("economy", new com.google.gson.JsonObject());
      data.getAsJsonObject("economy").addProperty("balance", 123);
      ClientGameState.accept(new GameStatePayload(data.toString()));
      int index = 0;
      for (var key : keys) {
        var input = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F6 + index++);
        key.setKey(input);
        KeyMapping.resetMapping();
        mc.gui.setScreen(null);
        KeyMapping.click(input);
        ClientKeys.tick(mc);
        boolean opened =
            switch (key.getName()) {
              case "key.sum1rem0d.wiki" -> mc.gui.screen() instanceof WikiScreen;
              case "key.sum1rem0d.config" -> mc.gui.screen() instanceof ConfigScreen;
              default -> mc.gui.screen() instanceof WalletScreen;
            };
        require(opened, "Native bound key opens " + key.getName());
        Screen typing = new ConfigScreen(null, true);
        mc.gui.setScreen(typing);
        KeyMapping.click(input);
        ClientKeys.tick(mc);
        require(mc.gui.screen() == typing, "Keys cannot interrupt an open screen");
        mc.gui.setScreen(null);
        ClientKeys.tick(mc);
        require(mc.gui.screen() == null, "Blocked clicks are drained rather than replayed");
      }
      passed.accept(
          "vanilla key registrations, default unbound keys, native click opening and typing/menu"
              + " guards");
      if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
      personal = PersonalConfig.store().snapshot();
      var coin = personal.getAsJsonObject("features").getAsJsonObject("economy");
      var clean = personal.getAsJsonObject("features").getAsJsonObject("cleanliness");
      for (var f : List.of(coin, clean)) {
        f.addProperty("hud", true);
        f.addProperty("hud_anchor", 3);
        f.addProperty("hud_x", 12);
        f.addProperty("hud_y", 20);
        f.addProperty("hud_scale", 1.5);
        f.addProperty("hud_background_opacity", 0.4);
        f.addProperty("hud_text_color", "#12ABEF");
        f.addProperty("hud_label", "Test HUD");
      }
      clean.addProperty("hud_bar", true);
      clean.addProperty("hud_bar_color", "#123456");
      PersonalConfig.store().update(personal, PersonalConfig.store().revision());
      var rendered = draw(mc);
      require(
          rendered.rectangles.size() == 3 && rendered.texts == 2,
          "Both native HUDs draw with background, bar and text");
      var background =
          rendered.rectangles.stream()
              .filter(r -> r.col1() == 0x66000000)
              .findFirst()
              .orElseThrow();
      require(
          Math.abs(background.pose().m00() - 1.5F) < 0.001F,
          "HUD scale reaches native render state");
      require(
          rendered.rectangles.stream().anyMatch(r -> r.col1() == 0xFF123456),
          "Custom bar color reaches native render state");
      int width = mc.font.width("Test HUD 123") + 6;
      var expected =
          HudLayout.place(
              mc.getWindow().getGuiScaledWidth(),
              mc.getWindow().getGuiScaledHeight(),
              width,
              13,
              3,
              12,
              20,
              1.5);
      require(
          rendered.rectangles.stream()
              .anyMatch(
                  r ->
                      r.col1() == 0x66000000
                          && Math.min(r.x0(), r.x1()) == (int) Math.floor(expected.x())
                          && Math.min(r.y0(), r.y1()) == (int) Math.floor(expected.y())),
          "Right-bottom anchor and offsets reach native rectangle geometry");
      passed.accept("native HUD right-bottom position, scale, opacity and custom bar color");
      for (var f : List.of(coin, clean)) f.addProperty("hud_background_opacity", 0);
      clean.addProperty("hud_bar", false);
      PersonalConfig.store().update(personal, PersonalConfig.store().revision());
      rendered = draw(mc);
      require(
          rendered.rectangles.isEmpty() && rendered.texts == 2,
          "Transparent text-only mode has no geometry");
      mc.gui.hud.toggle();
      rendered = draw(mc);
      require(rendered.rectangles.isEmpty() && rendered.texts == 0, "F1 hides both custom HUDs");
      mc.gui.hud.toggle();
      for (var f : List.of(coin, clean)) f.addProperty("hud", false);
      PersonalConfig.store().update(personal, PersonalConfig.store().revision());
      rendered = draw(mc);
      require(
          rendered.rectangles.isEmpty() && rendered.texts == 0,
          "Individual HUD toggles suppress drawing");
      passed.accept("native transparent/text-only modes, personal HUD switches and F1 visibility");
    } finally {
      for (int i = 0; i < Math.min(keys.size(), saved.size()); i++)
        keys.get(i).setKey(saved.get(i));
      KeyMapping.resetMapping();
      PersonalConfig.store().update(original, PersonalConfig.store().revision());
      ClientRules.accept(
          new ConfigStatePayload(revision, editable, rules.toString(), "", 0, false));
      ClientGameState.accept(new GameStatePayload(state.toString()));
      if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
      mc.gui.setScreen(screen);
    }
  }

  private record Draw(List<ColoredRectangleRenderState> rectangles, int texts) {}

  private static Draw draw(Minecraft mc) {
    var state = new GuiRenderState();
    var g = new GuiGraphicsExtractor(mc, state, 0, 0);
    CustomHud.render(g);
    var rectangles = new ArrayList<ColoredRectangleRenderState>();
    state.forEachElement(
        e -> {
          if (e instanceof ColoredRectangleRenderState r) rectangles.add(r);
        },
        GuiRenderState.TraverseRange.ALL);
    int[] texts = {0};
    state.forEachText(t -> texts[0]++);
    return new Draw(rectangles, texts[0]);
  }
}
