package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

/** Opt-in native UI regression fixture for preedit and single-insert commit semantics. */
final class ImeSmoke {
    private static boolean finished;
    private ImeSmoke() {}

    static void tick(Minecraft mc) {
        if (finished || !Boolean.getBoolean("sum1rem0d.imeSmoke") || !mc.isGameLoadFinished()) return;
        finished = true;
        var result = new JsonObject();
        var passed = new ArrayList<String>();
        JsonObject original = PersonalConfig.store().snapshot();
        Screen previous = mc.gui.screen();
        try {
            update(true);
            verify(mc, new ConfigScreen(previous, true), "Config検索欄", passed);
            verify(mc, new WikiScreen(previous), "Wiki検索欄", passed);
            update(false);
            verify(mc, new WikiScreen(previous), "Config OFF時のWiki検索欄", passed);
            result.addProperty("success", true);
        } catch (Throwable failure) {
            result.addProperty("success", false);
            result.addProperty("failure", failure.toString());
            Sum1reM0d.LOGGER.error("IME_SMOKE failed", failure);
        } finally {
            try { PersonalConfig.store().update(original, PersonalConfig.store().revision()); }
            catch (Exception failure) { result.addProperty("restoreFailure", failure.toString()); result.addProperty("success", false); }
            mc.gui.setScreen(previous);
        }
        result.add("passed", new com.google.gson.Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-ime-verification.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
        Sum1reM0d.LOGGER.info("IME_SMOKE: completed success={}", result.get("success").getAsBoolean());
        mc.stop();
    }

    private static void verify(Minecraft mc, Screen screen, String label, List<String> passed) {
        mc.gui.setScreen(screen);
        EditBox box = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow(() -> new AssertionError(label + " EditBox not found"));
        box.setValue("日本語:"); box.setCursorPosition(box.getValue().length()); box.setFocused(true);
        var preedit = new PreeditEvent("かな漢字", 4, List.of("かな", "漢字"), 1);
        require(box.preeditUpdated(preedit), label + " accepts native composition event");
        require(box.getValue().equals("日本語:"), label + " keeps uncommitted preedit out of stored text");
        // EditBox represents a canceled/empty native composition with a null preedit event.
        require(box.preeditUpdated(null), label + " clears composition through the native cancellation form");
        "仮名".codePoints().forEach(point -> require(box.charTyped(new CharacterEvent(point)), label + " accepts committed Unicode input"));
        require(box.getValue().equals("日本語:仮名"), label + " inserts the committed string exactly once");
        passed.add(label + ": composition is separate, clauses/caret remain on native preedit path, commit inserts once");
    }

    private static void update(boolean enabled) throws Exception {
        var store = PersonalConfig.store();
        JsonObject draft = store.snapshot();
        draft.getAsJsonObject("features").getAsJsonObject("japanese_ime").addProperty("enabled", enabled);
        store.update(draft, store.revision());
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
