package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonPrimitive;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class OptionScreen extends Screen {
    private final FeatureScreen parent;
    private final ConfigScreen root;
    private final ConfigSchema.Feature feature;
    private final ConfigSchema.Option option;
    private EditBox input;
    private String draft;
    private String error = "";

    OptionScreen(FeatureScreen parent, ConfigScreen root, ConfigSchema.Feature feature, ConfigSchema.Option option) {
        super(Component.literal(option.label()));
        this.parent = parent; this.root = root; this.feature = feature; this.option = option;
        draft = root.values(feature).get(option.key()).getAsString();
    }
    @Override protected void init() {
        int panel = Math.min(320, width - 20);
        int left = (width - panel) / 2;
        input = addRenderableWidget(new EditBox(font, left, 65, panel, 20, title));
        input.setMaxLength(option.isNumber() ? 12 : (int) option.max());
        input.setValue(draft);
        input.setResponder(value -> draft = value);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.literal("初期値"), button -> input.setValue(option.initial().getAsString()))
            .bounds(left, 95, panel, 20).build());
        addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose())
            .bounds(left, height - 28, (panel - 8) / 2, 20).build());
        Button confirm = addRenderableWidget(Button.builder(Component.literal("変更を保持"), button -> {
            try {
                JsonPrimitive value = option.isNumber() ? new JsonPrimitive(Double.parseDouble(input.getValue())) : new JsonPrimitive(input.getValue());
                option.validate(value);
                if (!root.editable()) throw new IllegalArgumentException("変更権限がありません。");
                root.values(feature).add(option.key(), value);
                onClose();
            } catch (IllegalArgumentException exception) {
                error = option.isNumber() ? option.min() + "～" + option.max() + (option.kind() == ConfigSchema.Kind.INTEGER ? "の整数" : "の数値") + "を入力してください。" : "入力値を確認してください。";
            }
        }).bounds(left + (panel + 8) / 2, height - 28, (panel - 8) / 2, 20).build());
        confirm.active = root.editable();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
        int panel = Math.min(320, width - 20);
        graphics.textWithWordWrap(font, Component.literal(com.sumirelabs.sum1rem0d.config.ConfigTiming.description(feature.id(), option.key())), (width - panel) / 2, 40, panel, 0xFFB8C6DA);
        graphics.centeredText(font, error, width / 2, 127, 0xFFFF8080);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
