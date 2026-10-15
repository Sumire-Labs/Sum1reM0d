package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ResourceEntryScreen extends Screen {
    private final Screen parent;
    private final boolean editable;
    private final ConfigSchema.Option option;
    private final Consumer<String> accept;
    private final boolean mapping;
    private String first, second = "", error = "";
    private List<String> suggestions = List.of();
    private boolean suggestSecond;
    ResourceEntryScreen(Screen parent, boolean editable, ConfigSchema.Option option, String value, Consumer<String> accept) {
        super(Component.literal(option.label())); this.parent = parent; this.editable = editable; this.option = option; this.accept = accept;
        mapping = option.kind() == ConfigSchema.Kind.MAP;
        if (mapping) { String[] pair = value.split("=", 2); first = pair[0]; if (pair.length == 2) second = pair[1]; }
        else first = value;
    }
    @Override protected void init() {
        int panel = Math.min(420, width - 20), left = (width - panel) / 2;
        EditBox a = addRenderableWidget(new EditBox(font, left, 40, panel - 62, 20, Component.literal(mapping ? "鉱石ブロックID" : "項目")));
        a.setMaxLength((int) option.max()); a.setValue(first); a.setResponder(value -> first = value); a.setEditable(editable);
        addRenderableWidget(Button.builder(Component.literal("候補"), button -> find(false)).bounds(left + panel - 58, 40, 58, 20).build());
        if (mapping) {
            EditBox b = addRenderableWidget(new EditBox(font, left, 65, panel - 62, 20, Component.literal("ドロップアイテムID")));
            b.setMaxLength((int) option.max()); b.setValue(second); b.setResponder(value -> second = value); b.setEditable(editable);
            addRenderableWidget(Button.builder(Component.literal("候補"), button -> find(true)).bounds(left + panel - 58, 65, 58, 20).build());
        }
        int start = mapping ? 93 : 69;
        int rows = Math.max(0, (height - start - 75) / 21);
        for (int row = 0; row < Math.min(rows, suggestions.size()); row++) {
            String suggestion = suggestions.get(row);
            Button select = addRenderableWidget(Button.builder(Component.literal(suggestion), button -> {
                if (suggestSecond) second = suggestion; else first = suggestion; suggestions = List.of(); rebuildWidgets();
            }).bounds(left, start + row * 21, panel, 20).build()); select.active = editable;
        }
        addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose()).bounds(left, height - 28, (panel - 8) / 2, 20).build());
        Button save = addRenderableWidget(Button.builder(Component.literal("決定"), button -> {
            try {
                String aValue = first.trim(), bValue = second.trim();
                if (aValue.isEmpty() || aValue.contains(",") || mapping && aValue.contains("=")) throw new IllegalArgumentException();
                if (mapping && (!aValue.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !bValue.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))) throw new IllegalArgumentException();
                if (option.key().equals("moon_phases") && !aValue.matches("[0-7]")) throw new IllegalArgumentException();
                if (option.key().contains("url") && !aValue.equals("sum1rem0d:default_ad")) {
                    var uri = java.net.URI.create(aValue);
                    if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null) throw new IllegalArgumentException();
                }
                ConfigSchema.validateEntry(option, mapping ? aValue + "=" + bValue : aValue);
                accept.accept(mapping ? aValue + "=" + bValue : aValue); onClose();
            } catch (IllegalArgumentException exception) { error = mapping ? "block_id=item_id の両方を指定してください。" : "値の形式を確認してください。"; }
        }).bounds(left + (panel + 8) / 2, height - 28, (panel - 8) / 2, 20).build()); save.active = editable;
        setInitialFocus(a);
    }
    private void find(boolean right) {
        suggestSecond = right;
        String query = (right ? second : first).toLowerCase(Locale.ROOT);
        if (option.key().equals("moon_phases")) { suggestions = java.util.stream.IntStream.range(0, 8).mapToObj(Integer::toString).filter(id -> id.contains(query)).toList(); rebuildWidgets(); return; }
        if (option.key().equals("namespaces")) { suggestions = BuiltInRegistries.ITEM.keySet().stream().map(id -> id.getNamespace()).distinct().sorted().filter(id -> id.contains(query)).limit(12).toList(); rebuildWidgets(); return; }
        if (option.key().equals("dimensions")) { suggestions = java.util.List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end").stream().filter(id -> id.contains(query)).toList(); rebuildWidgets(); return; }
        if (option.key().equals("target_mods")) {
            //? if fabric {
            suggestions = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().map(mod -> mod.getMetadata().getId()).filter(id -> id.contains(query)).sorted().limit(12).toList();
            //?} elif neoforge {
            /*suggestions = net.neoforged.fml.ModList.get().getMods().stream().map(mod -> mod.getModId()).filter(id -> id.contains(query)).sorted().limit(12).toList();
            *///?}
            rebuildWidgets(); return;
        }
        if (option.key().contains("url")) { suggestions = option.key().equals("urls") ? java.util.List.of("https://www.youtube.com/", "sum1rem0d:default_ad") : java.util.List.of("https://www.youtube.com/"); rebuildWidgets(); return; }
        var registry = mapping ? right ? BuiltInRegistries.ITEM : BuiltInRegistries.BLOCK
            : option.key().contains("effect") ? BuiltInRegistries.MOB_EFFECT
            : option.key().contains("block") ? BuiltInRegistries.BLOCK
            : option.key().contains("mob") ? BuiltInRegistries.ENTITY_TYPE : BuiltInRegistries.ITEM;
        suggestions = registry.keySet().stream().map(Object::toString).filter(id -> id.contains(query)).sorted().limit(12).toList(); rebuildWidgets();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
        super.extractRenderState(graphics, x, y, delta); graphics.centeredText(font, title, width / 2, 17, -1);
        graphics.centeredText(font, font.plainSubstrByWidth(error, width - 20), width / 2, height - 48, 0xFFFF8080);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
