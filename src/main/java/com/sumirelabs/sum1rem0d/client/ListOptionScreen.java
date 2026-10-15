package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonPrimitive;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ListOptionScreen extends Screen {
    private final Screen parent;
    private final ConfigScreen root;
    private final ConfigSchema.Feature feature;
    private final ConfigSchema.Option option;
    private final List<String> entries;
    private int page;
    private String error = "";
    ListOptionScreen(Screen parent, ConfigScreen root, ConfigSchema.Feature feature, ConfigSchema.Option option) {
        super(Component.literal(option.label())); this.parent = parent; this.root = root; this.feature = feature; this.option = option;
        entries = new ArrayList<>(ConfigSchema.split(root.values(feature).get(option.key()).getAsString()));
    }
    @Override protected void init() {
        int panel = Math.min(430, width - 20), left = (width - panel) / 2;
        int rows = Math.max(1, (height - 140) / 24), pages = Math.max(1, (entries.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int row = 0; row < rows && page * rows + row < entries.size(); row++) {
            int index = page * rows + row;
            addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(entries.get(index), panel - 38)), button -> edit(index)).bounds(left, 42 + row * 24, panel - 28, 20).build());
            Button remove = addRenderableWidget(Button.builder(Component.literal("×"), button -> { entries.remove(index); rebuildWidgets(); }).bounds(left + panel - 24, 42 + row * 24, 24, 20).build()); remove.active = root.editable();
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), button -> { page--; rebuildWidgets(); }).bounds(left, height - 92, 40, 20).build()); previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> { page++; rebuildWidgets(); }).bounds(left + panel - 40, height - 92, 40, 20).build()); next.active = page + 1 < pages;
        Button add = addRenderableWidget(Button.builder(Component.literal("追加"), button -> edit(-1)).bounds(left + 45, height - 92, (panel - 100) / 2, 20).build()); add.active = root.editable();
        Button reset = addRenderableWidget(Button.builder(Component.literal("初期値"), button -> { entries.clear(); entries.addAll(ConfigSchema.split(option.initial().getAsString())); rebuildWidgets(); }).bounds(left + panel / 2, height - 92, (panel - 100) / 2, 20).build()); reset.active = root.editable();
        addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose()).bounds(left, height - 28, (panel - 8) / 2, 20).build());
        Button confirm = addRenderableWidget(Button.builder(Component.literal("変更を保持"), button -> {
            try { var value = new JsonPrimitive(String.join(",", entries)); option.validate(value); root.values(feature).add(option.key(), value); onClose(); }
            catch (IllegalArgumentException exception) { error = "項目数・文字数または内容を確認してください。"; }
        }).bounds(left + (panel + 8) / 2, height - 28, (panel - 8) / 2, 20).build()); confirm.active = root.editable();
    }
    private void edit(int index) {
        minecraft.gui.setScreen(new ResourceEntryScreen(this, root.editable(), option, index < 0 ? "" : entries.get(index), value -> {
            if (index < 0) entries.add(value); else entries.set(index, value);
        }));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
        super.extractRenderState(graphics, x, y, delta); graphics.centeredText(font, title, width / 2, 17, -1);
        graphics.centeredText(font, "ページ " + (page + 1) + " / " + entries.size() + "件", width / 2, height - 64, 0xFFB8C6DA);
        graphics.centeredText(font, error, width / 2, height - 44, 0xFFFF8080);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
