package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class FeatureScreen extends Screen {
    private final ConfigScreen parent;
    private final ConfigSchema.Feature feature;
    private int page;
    FeatureScreen(ConfigScreen parent, ConfigSchema.Feature feature) {
        super(Component.literal(feature.label()));
        this.parent = parent;
        this.feature = feature;
    }
    @Override protected void init() {
        int panel = Math.min(360, width - 20);
        int left = (width - panel) / 2;
        var options = parent.options(feature);
        int rows = Math.max(1, (height - 150) / 24);
        int pages = Math.max(1, (options.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int row = 0; row < rows && page * rows + row < options.size(); row++) {
            var option = options.get(page * rows + row);
            var current = parent.values(feature).get(option.key());
            String value = option.isBoolean() ? (current.getAsBoolean() ? "ON" : "OFF") : option.kind() == ConfigSchema.Kind.LIST || option.kind() == ConfigSchema.Kind.MAP ? ConfigSchema.split(current.getAsString()).size() + "件" : current.getAsString();
            Button entry = addRenderableWidget(Button.builder(Component.literal(option.label() + ": " + value), button -> {
                if (option.isBoolean()) {
                    parent.values(feature).addProperty(option.key(), !parent.values(feature).get(option.key()).getAsBoolean());
                    rebuildWidgets();
                } else if (option.kind() == ConfigSchema.Kind.LIST || option.kind() == ConfigSchema.Kind.MAP) {
                    minecraft.gui.setScreen(new ListOptionScreen(this, parent, feature, option));
                } else {
                    minecraft.gui.setScreen(new OptionScreen(this, parent, feature, option));
                }
            }).bounds(left, 54 + row * 24, panel, 20).build());
            entry.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(com.sumirelabs.sum1rem0d.config.ConfigTiming.description(feature.id(), option.key()))));
            entry.active = parent.editable() || option.kind() == ConfigSchema.Kind.LIST || option.kind() == ConfigSchema.Kind.MAP;
        }
        Button reset = addRenderableWidget(Button.builder(Component.literal("この機能を初期値へ戻す"), button -> {
            for (var option : options) parent.values(feature).add(option.key(), option.initial());
            rebuildWidgets();
        }).bounds(left, height - 54, panel, 20).build());
        reset.active = parent.editable();
        Button prev = addRenderableWidget(Button.builder(Component.literal("◀"), button -> { page--; rebuildWidgets(); }).bounds(left, height - 80, 40, 20).build()); prev.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> { page++; rebuildWidgets(); }).bounds(left + panel - 40, height - 80, 40, 20).build()); next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.literal("戻る（一覧で適用）"), button -> onClose())
            .bounds(left, height - 28, panel, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF);
        graphics.centeredText(font, feature.id().equals("recipe_viewers") ? "抽選確率は次回起動時から反映" : feature.id().equals("sleep_advancement") ? "睡眠時間と対象世界は「睡眠不足」の項目で設定" : "設定一覧の「適用」で保存", width / 2, 36, 0xFFB8C6DA);
        graphics.centeredText(font, "ページ " + (page + 1), width / 2, height - 74, 0xFFB8C6DA);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
