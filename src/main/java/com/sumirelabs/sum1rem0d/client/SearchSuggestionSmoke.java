package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.*;

/** Explicit standalone native GUI check, without joining or modifying a world. */
final class SearchSuggestionSmoke {
  private static int stage;
  private static long changed;
  private static JsonObject original;
  private static final List<String> passed = new ArrayList<>();

  private SearchSuggestionSmoke() {}

  private static EditBox input(Screen s) {
    return s.children().stream()
        .filter(EditBox.class::isInstance)
        .map(EditBox.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private static SearchSuggestionPopup popup(Screen s) throws Exception {
    var f = s.getClass().getDeclaredField("suggestions");
    f.setAccessible(true);
    return (SearchSuggestionPopup) f.get(s);
  }

  private static void type(Screen s, String text) {
    var input = input(s);
    s.setFocused(input);
    input.setFocused(true);
    input.setValue(text);
  }

  private static void key(Screen s, int key) {
    s.keyPressed(new KeyEvent(key, 0, 0));
  }

  private static void require(boolean condition, String text) {
    if (!condition) throw new AssertionError(text);
  }

  private static void shot(Minecraft mc, String name) {
    net.minecraft.client.Screenshot.grab(
        mc.gameDirectory,
        "sumire-suggestions-" + name + ".png",
        mc.gameRenderer.mainRenderTarget(),
        1,
        c -> {});
  }

  static void tick(Minecraft mc) {
    if (!Boolean.getBoolean("sum1rem0d.suggestionsSmoke")
        || stage < 0
        || !mc.isGameLoadFinished()
        || mc.gui.overlay() != null) return;
    if (changed != 0 && System.nanoTime() - changed < 700_000_000L) return;
    try {
      switch (stage) {
        case 0 -> {
          original = PersonalConfig.store().snapshot();
          var d = original.deepCopy();
          d.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("enabled", true);
          d.getAsJsonObject("features")
              .getAsJsonObject("wiki")
              .addProperty("search_suggestions", true);
          PersonalConfig.store().update(d, PersonalConfig.store().revision());
          mc.gui.setScreen(new WikiScreen(new TitleScreen()));
          type(mc.gui.screen(), "石鹸");
          require(
              popup(mc.gui.screen()).visible(),
              "Japanese Wiki description match opens suggestions");
        }
        case 1 -> {
          shot(mc, "wiki");
          var wiki = (WikiScreen) mc.gui.screen();
          key(wiki, InputConstants.KEY_DOWN);
          key(wiki, InputConstants.KEY_RETURN);
          require(
              wiki.feature().equals("cleanliness"), "Keyboard selection opens soap Wiki article");
          wiki.setFocused(input(wiki));
          require(!popup(wiki).visible(), "Opening an article dismisses suggestions even with keyboard focus restored");
          passed.add("Japanese explanation match and arrow/Enter open the native Wiki article");
          mc.gui.setScreen(new ConfigScreen(new TitleScreen(), true));
          type(mc.gui.screen(), "coin_anvil");
          require(
              !popup(mc.gui.screen()).visible(),
              "Personal suggestions exclude server-only settings");
          type(mc.gui.screen(), "no_chat");
          require(
              popup(mc.gui.screen()).visible(),
              "Personal suggestions include their available settings");
        }
        case 2 -> {
          shot(mc, "personal");
          var config = mc.gui.screen();
          key(config, InputConstants.KEY_TAB);
          require(
              input(config).getValue().equals("no_chat_reports") && !popup(config).visible(),
              "Tab completes selected ID without opening or applying settings");
          key(config, InputConstants.KEY_DOWN);
          require(popup(config).visible(), "Down reopens dismissed suggestions");
          key(config, InputConstants.KEY_ESCAPE);
          require(
              mc.gui.screen() == config && !popup(config).visible(),
              "Escape closes only the popup");
          type(config, "wiki");
          key(config, InputConstants.KEY_RETURN);
          config.setFocused(input(config));
          require(!popup(config).visible(), "Ordinary Enter dismisses suggestions after a search rebuild");
          passed.add(
              "personal scope filtering, Tab completion, reopening and Escape retain the current"
                  + " screen");
          mc.gui.setScreen(new ConfigScreen(new TitleScreen()));
          type(mc.gui.screen(), "coin_anv");
        }
        case 3 -> {
          shot(mc, "config");
          var config = mc.gui.screen();
          var input = input(config);
          config.mouseClicked(new MouseButtonEvent(input.getX()+10, input.getY()+input.getHeight()+31, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)), false);
          require(mc.gui.screen()==config, "Popup help footer cannot click through to native feature buttons");
          config.mouseClicked(
              new MouseButtonEvent(
                  input.getX() + 10,
                  input.getY() + input.getHeight() + 8,
                  new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)),
              false);
          var opened = mc.gui.screen();
          require(
              opened instanceof FeatureScreen
                  && opened
                      .getTitle()
                      .getString()
                      .equals(
                          com.sumirelabs.sum1rem0d.wiki.WikiContent.schema("coin_anvil").label()),
              "Suggestion click opens the feature instead of the covered list button");
          passed.add(
              "native Config candidate mouse click opens the intended feature without"
                  + " click-through");
          var d = PersonalConfig.store().snapshot();
          d.getAsJsonObject("features")
              .getAsJsonObject("wiki")
              .addProperty("search_suggestions", false);
          PersonalConfig.store().update(d, PersonalConfig.store().revision());
          mc.gui.setScreen(new WikiScreen(new TitleScreen()));
          type(mc.gui.screen(), "石鹸");
          require(!popup(mc.gui.screen()).visible(), "Personal OFF hides suggestions immediately");
          key(mc.gui.screen(), InputConstants.KEY_RETURN);
          require(
              ((WikiScreen) mc.gui.screen()).view().equals("search"),
              "Normal Enter search remains usable with suggestions disabled");
          passed.add("personal OFF hides suggestions and preserves ordinary Enter search");
        }
        case 4 -> {
          shot(mc, "disabled");
          finish(mc, true, "");
          return;
        }
      }
      stage++;
      changed = System.nanoTime();
    } catch (Throwable failure) {
      Sum1reM0d.LOGGER.error("Suggestion smoke failed", failure);
      finish(mc, false, failure.toString());
    }
  }

  private static void finish(Minecraft mc, boolean success, String failure) {
    stage = -1;
    try {
      if (original != null)
        PersonalConfig.store().update(original, PersonalConfig.store().revision());
      var result = new JsonObject();
      result.addProperty("success", success);
      result.addProperty("failure", failure);
      result.add("passed", new Gson().toJsonTree(passed));
      Files.writeString(
          mc.gameDirectory.toPath().resolve("sum1rem0d-suggestions-verification.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(result));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    mc.gui.setScreen(new TitleScreen());
    mc.stop();
  }
}
