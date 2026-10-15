package com.sumirelabs.sum1rem0d.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigValidationTest {
    private static void invalid(String feature, String key, Object value) {
        var input = ConfigSchema.defaults(); var values = input.getAsJsonObject("features").getAsJsonObject(feature);
        if (value instanceof Number number) values.addProperty(key, number); else values.addProperty(key, value.toString());
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
    }
    @Test void reversedFuelAndDistanceRangesAreRejected() {
        invalid("daily_fuel", "minimum", 2);
        invalid("knockback_mob", "minimum_distance", 30);
    }
    @Test void sleepThresholdsAndCleanlinessThresholdAreOrdered() {
        invalid("sleep_deprivation", "hunger_days", 5);
        invalid("sleep_deprivation", "death_days", 3);
        invalid("cleanliness", "maximum", 40);
    }
    @Test void inventoryRangeCannotOverflow() { invalid("inventory_swap", "start_slot", 30); }
    @Test void resourceAndTagSyntaxIsValidated() {
        invalid("vegan", "forbidden_items", "Minecraft:beef");
        invalid("grass_coal", "input_item", "#minecraft:logs");
        invalid("damage_effects", "buff_effects", "invalid value");
        var input = ConfigSchema.defaults(); input.getAsJsonObject("features").getAsJsonObject("vegan").addProperty("forbidden_items", "#example:animal_food,minecraft:beef");
        assertDoesNotThrow(() -> ConfigSchema.normalize(input));
    }
    @Test void mappingSyntaxAndDuplicateSourcesAreRejected() {
        invalid("ore_drops", "mapping", "minecraft:coal_ore=broken");
        invalid("ore_drops", "mapping", "minecraft:coal_ore=minecraft:coal,minecraft:coal_ore=minecraft:diamond");
    }
    @Test void urlsPhasesAndModIdsAreValidated() {
        invalid("youtube", "url", "javascript:alert(1)");
        invalid("advertisements", "urls", "file:///etc/passwd");
        invalid("growth_sleep", "moon_phases", "0,8");
        invalid("recipe_viewers", "target_mods", "invalid mod id");
    }
    @Test void soapMaterialsMustFitCraftingGrid() {
        invalid("cleanliness", "soap_materials", "");
        invalid("cleanliness", "soap_materials", String.join(",", java.util.Collections.nCopies(10, "minecraft:clay_ball")));
    }
}
