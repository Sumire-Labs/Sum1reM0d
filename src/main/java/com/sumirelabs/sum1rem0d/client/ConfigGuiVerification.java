package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/** Opt-in offline GUI fixture; exercises actual widget callbacks through disk save. */
final class ConfigGuiVerification {
    private ConfigGuiVerification() {}
    static void verifyMap(Minecraft mc) throws Exception {
        require(mc.getConnection() == null, "Mapping GUI fixture requires an offline client");
        var previousScreen = mc.gui.screen(); var store = Sum1reM0d.config(); var original = store.snapshot();
        var feature = ConfigSchema.features().stream().filter(value -> value.id().equals("ore_drops")).findFirst().orElseThrow();
        var option = feature.options().stream().filter(value -> value.key().equals("mapping")).findFirst().orElseThrow();
        String before = store.text("ore_drops", "mapping");
        require(before.contains("minecraft:coal_ore=minecraft:redstone"), "Mapping fixture expects default coal entry");
        try {
            var root = new ConfigScreen(previousScreen); var details = new FeatureScreen(root, feature); mc.gui.setScreen(details);
            openMap(mc, option.label());
            clickPrefix(mc.gui.screen(), "minecraft:coal_ore=");
            var fields = mc.gui.screen().children().stream().filter(child -> child instanceof EditBox).map(child -> (EditBox) child).toList();
            require(fields.size() == 2 && fields.getFirst().getValue().equals("minecraft:coal_ore") && fields.get(1).getValue().equals("minecraft:redstone"), "Map editor separates block and item IDs");
            fields.get(1).setValue("minecraft:diamond"); click(mc.gui.screen(), "決定"); click(mc.gui.screen(), "キャンセル");
            require(root.values(feature).get("mapping").getAsString().equals(before) && store.text("ore_drops", "mapping").equals(before), "Cancelling the list discards an edited entry without changing the draft or saved config");
            openMap(mc, option.label()); clickPrefix(mc.gui.screen(), "minecraft:coal_ore=");
            var editor = mc.gui.screen(); fields = editor.children().stream().filter(child -> child instanceof EditBox).map(child -> (EditBox) child).toList();
            fields.get(1).setValue("invalid item"); click(editor, "決定"); require(mc.gui.screen() == editor, "Invalid mapping syntax remains in the editor");
            fields.get(1).setValue("minecraft:emerald"); click(editor, "決定");
            require(root.values(feature).get("mapping").getAsString().equals(before), "Entry confirmation does not prematurely save the list draft");
            click(mc.gui.screen(), "変更を保持");
            String expected = before.replace("minecraft:coal_ore=minecraft:redstone", "minecraft:coal_ore=minecraft:emerald");
            require(root.values(feature).get("mapping").getAsString().equals(expected) && store.text("ore_drops", "mapping").equals(before), "List confirmation updates only the unsaved root draft");
            mc.gui.setScreen(root); click(root, "適用");
            require(store.text("ore_drops", "mapping").equals(expected), "Root Apply commits the mapping through ConfigStore");
            var saved = JsonParser.parseString(Files.readString(mc.gameDirectory.toPath().resolve("config/sum1rem0d.json"))).getAsJsonObject();
            require(saved.getAsJsonObject("features").getAsJsonObject("ore_drops").get("mapping").getAsString().equals(expected), "Mapping is present in the actual saved JSON file");
            String savedText = Files.readString(mc.gameDirectory.toPath().resolve("config/sum1rem0d.json")); long savedRevision = store.revision();
            var invalidRoot = new ConfigScreen(previousScreen); mc.gui.setScreen(new FeatureScreen(invalidRoot, feature)); openMap(mc, option.label()); clickPrefix(mc.gui.screen(), "minecraft:coal_ore=");
            fields = mc.gui.screen().children().stream().filter(child -> child instanceof EditBox).map(child -> (EditBox) child).toList(); fields.get(1).setValue("sum1rem0d:missing_item"); click(mc.gui.screen(), "決定"); click(mc.gui.screen(), "変更を保持"); mc.gui.setScreen(invalidRoot); click(invalidRoot, "適用");
            require(store.revision() == savedRevision && store.text("ore_drops", "mapping").equals(expected) && Files.readString(mc.gameDirectory.toPath().resolve("config/sum1rem0d.json")).equals(savedText), "Before world load, a missing registered item still rejects Apply without changing revision or disk contents");
        } finally {
            store.update(original, store.revision()); mc.gui.setScreen(previousScreen);
        }
        require(store.snapshot().equals(original), "Mapping GUI fixture restores the original configuration");
        Sum1reM0d.LOGGER.info("CLIENT_SMOKE: native mapping editor, invalid syntax, list cancel, draft isolation, Apply and disk save verified");
    }
    private static void openMap(Minecraft mc, String label) {
        for (int page = 0; page < 30; page++) {
            var entry = mc.gui.screen().children().stream().filter(child -> child instanceof Button button && button.getMessage().getString().startsWith(label + ":")).map(child -> (Button) child).findFirst();
            if (entry.isPresent()) { press(entry.get()); return; }
            click(mc.gui.screen(), "▶");
        }
        throw new AssertionError("Mapping option not found in feature pages");
    }
    private static void click(Screen screen, String label) {
        press(screen.children().stream().filter(child -> child instanceof Button button && button.getMessage().getString().equals(label)).map(child -> (Button) child).findFirst().orElseThrow());
    }
    private static void clickPrefix(Screen screen, String prefix) {
        press(screen.children().stream().filter(child -> child instanceof Button button && button.getMessage().getString().startsWith(prefix)).map(child -> (Button) child).findFirst().orElseThrow());
    }
    private static void press(Button button) { require(button.active, "Fixture button must be enabled"); button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
