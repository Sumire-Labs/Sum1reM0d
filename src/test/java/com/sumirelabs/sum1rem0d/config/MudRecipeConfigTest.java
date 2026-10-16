package com.sumirelabs.sum1rem0d.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MudRecipeConfigTest {
  @Test
  void recipeRequiresOneToNineExactItemSlots() {
    var d = ConfigSchema.defaults();
    var mud = d.getAsJsonObject("features").getAsJsonObject("mud_ball");
    mud.addProperty("mud_materials", "");
    assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(d));
    mud.addProperty(
        "mud_materials", String.join(",", java.util.Collections.nCopies(10, "minecraft:dirt")));
    assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(d));
    mud.addProperty("mud_materials", "#minecraft:dirt");
    assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(d));
    mud.addProperty("mud_materials", "minecraft:dirt,minecraft:dirt");
    assertDoesNotThrow(() -> ConfigSchema.normalize(d));
  }
}
