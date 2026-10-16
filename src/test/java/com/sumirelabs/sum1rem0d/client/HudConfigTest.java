package com.sumirelabs.sum1rem0d.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sumirelabs.sum1rem0d.config.*;
import org.junit.jupiter.api.Test;

class HudConfigTest {
  @Test
  void acceptsBlankAndJapaneseLabelsAndHexColors() {
    var d = ConfigSchema.defaults();
    var h = d.getAsJsonObject("features").getAsJsonObject("economy");
    h.addProperty("hud_label", "所持金");
    h.addProperty("hud_text_color", "#aB12Ff");
    assertDoesNotThrow(() -> ConfigSchema.normalize(d));
    h.addProperty("hud_label", "");
    assertDoesNotThrow(() -> ConfigSchema.normalize(d));
    assertTrue(PersonalConfig.personal("economy", "hud_scale"));
    assertFalse(PersonalConfig.personal("economy", "starting_balance"));
  }

  @Test
  void rejectsInvalidColorAndControlCharacters() {
    var d = ConfigSchema.defaults();
    var h = d.getAsJsonObject("features").getAsJsonObject("cleanliness");
    h.addProperty("hud_bar_color", "red");
    assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(d));
    h.addProperty("hud_bar_color", "#123456");
    h.addProperty("hud_label", "a\nb");
    assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(d));
  }
}
