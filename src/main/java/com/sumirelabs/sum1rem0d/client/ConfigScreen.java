package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import com.sumirelabs.sum1rem0d.network.ConfigRequestPayload;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Draft-based editing: navigating never applies changes; Apply is explicit. */
public final class ConfigScreen extends Screen {
    private final Screen parent;
    private final boolean online;
    private final boolean personal;
    private JsonObject draft;
    private long revision;
    private long requestId;
    private long requestedAt;
    private boolean pending;
    private int page;
    private String query = "", searchText = "";
    private EditBox search;
    private SearchSuggestionPopup suggestions;
    private String status = "";

    public ConfigScreen(Screen parent) {
        this(parent, false);
    }
    public ConfigScreen(Screen parent, boolean personal) {
        super(Component.literal("Sum1reM0d Config"));
        this.parent = parent;
        this.personal = personal;
        online = !personal && Minecraft.getInstance().getConnection() != null;
        if (online) {
            PlatformClientNetwork.refresh();
            if (ClientRules.received()) takeSnapshot();
        } else {
            draft = localStore().snapshot();
            revision = localStore().revision();
        }
    }
    private void takeSnapshot() {
        draft = ClientRules.snapshot();
        revision = ClientRules.revision;
    }
    boolean editable() { return draft != null && !pending && (!online || ClientRules.editable); }
    private com.sumirelabs.sum1rem0d.config.ConfigStore localStore() { return personal ? PersonalConfig.store() : Sum1reM0d.config(); }
    java.util.List<ConfigSchema.Option> options(ConfigSchema.Feature feature) { return feature.options().stream().filter(option -> PersonalConfig.personal(feature.id(), option.key()) == personal).toList(); }
    JsonObject values(ConfigSchema.Feature feature) { return draft.getAsJsonObject("features").getAsJsonObject(feature.id()); }

    @Override protected void init() {
        int panel = Math.min(420, width - 20);
        int left = (width - panel) / 2;
        var wiki = addRenderableWidget(Button.builder(Component.literal("Wiki"), button -> minecraft.gui.setScreen(new WikiScreen(this))).bounds(5, 5, 55, 20).build());
        wiki.active = WikiRuntime.enabled();
        if (!personal) addRenderableWidget(Button.builder(Component.literal("個人設定"), button -> minecraft.gui.setScreen(new ConfigScreen(this, true))).bounds(width - 77, 5, 72, 20).build());
        search = addRenderableWidget(new EditBox(font, left, 42, panel - 65, 20, Component.literal("機能を検索")));
        search.setMaxLength(80);
        search.setValue(searchText);
        suggestions = new SearchSuggestionPopup(search, ConfigSchema.features().stream().filter(feature -> draft != null && !pending && !options(feature).isEmpty()).map(feature -> new com.sumirelabs.sum1rem0d.wiki.SearchSuggestions.Entry(feature.id(), feature.label(), "")).toList(), height, id -> minecraft.gui.setScreen(new FeatureScreen(this, com.sumirelabs.sum1rem0d.wiki.WikiContent.schema(id))));
        search.setResponder(value -> { searchText = value; suggestions.changed(value); });
        suggestions.changed(searchText);
        addRenderableWidget(Button.builder(Component.literal("検索"), button -> {
            submitSearch();
        }).bounds(left + panel - 60, 42, 60, 20).build());
        List<ConfigSchema.Feature> filtered = ConfigSchema.features().stream()
            .filter(feature -> !options(feature).isEmpty())
            .filter(feature -> (feature.id() + " " + feature.label()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
            .toList();
        int rows = Math.max(1, (height - 145) / 24);
        int pages = Math.max(1, (filtered.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int index = page * rows; index < Math.min(filtered.size(), (page + 1) * rows); index++) {
            var feature = filtered.get(index);
            String state = draft == null ? "?" : values(feature).get("enabled").getAsBoolean() ? "ON" : "OFF";
            String label = feature.label() + " [" + state + "]";
            Button entry = addRenderableWidget(Button.builder(Component.literal(label),
                button -> minecraft.gui.setScreen(new FeatureScreen(this, feature)))
                .bounds(left, 70 + (index - page * rows) * 24, panel, 20).build());
            entry.active = draft != null && !pending;
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), button -> { page--; rebuildWidgets(); })
            .bounds(left, height - 70, 40, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> { page++; rebuildWidgets(); })
            .bounds(left + panel - 40, height - 70, 40, 20).build());
        next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.literal("キャンセル"), button -> onClose())
            .bounds(left, height - 27, (panel - 10) / 2, 20).build());
        Button apply = addRenderableWidget(Button.builder(Component.literal("適用"), button -> apply())
            .bounds(left + (panel + 10) / 2, height - 27, (panel - 10) / 2, 20).build());
        apply.active = editable();
        if (draft == null) status = "サーバー設定を取得できません。Modの導入状況を確認してください。";
    }

    private void submitSearch() { query = search.getValue(); page = 0; rebuildWidgets(); suggestions.dismiss(); }

    private void apply() {
        if (!editable()) return;
        try {
            JsonObject normalized = ConfigSchema.normalize(draft);
            if (online) {
                requestId = ClientRules.nextRequestId(); requestedAt = System.nanoTime();
                pending = PlatformClientNetwork.send(new ConfigRequestPayload(true, revision, normalized.toString(), requestId));
                status = pending ? "サーバーへ保存を要求しています…" : "サーバーへ送信できません。";
            } else {
                localStore().update(normalized, revision);
                revision = localStore().revision();
                status = personal ? "個人設定を保存しました。" : "標準設定を保存しました。";
            }
        } catch (IOException | RuntimeException exception) {
            String detail = exception instanceof IOException ? "設定ファイルを確認してください。" : exception.getMessage();
            if (detail == null) detail = "設定値を確認してください。";
            status = "保存に失敗しました: " + detail;
            Sum1reM0d.LOGGER.error("Cannot apply configuration", exception);
        }
        rebuildWidgets();
    }

    @Override public void tick() {
        if (online && draft == null && ClientRules.received()) {
            takeSnapshot(); status = ""; rebuildWidgets();
        }
        if (pending && ClientRules.responseId == requestId) {
            pending = false;
            status = ClientRules.responseMessage;
            if (ClientRules.responseApplied) takeSnapshot();
            rebuildWidgets();
        }
        if (pending && (minecraft.getConnection() == null || System.nanoTime() - requestedAt > 15_000_000_000L)) {
            pending = false; status = "応答を確認できません。編集内容は保持しています。画面を開き直して保存状態を確認してください。";
            rebuildWidgets();
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 13, 0xFFFFFFFF);
        String scope = personal ? "個人設定 / 他のプレイヤーには影響しません" : online ? (ClientRules.editable ? "サーバー設定 / 変更可能" : "サーバー設定 / 閲覧のみ") : "このインスタンスの標準設定";
        graphics.centeredText(font, scope, width / 2, 28, 0xFFB8C6DA);
        graphics.centeredText(font, "ページ " + (page + 1) + " / 各機能の詳細から調整できます", width / 2, height - 64, 0xFFB8C6DA);
        graphics.centeredText(font, font.plainSubstrByWidth(status, width - 16), width / 2, height - 42, 0xFFFFD180);
        suggestions.render(graphics, font, mouseX, mouseY);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return suggestions.click(event) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (suggestions.key(event)) return true;
        if (search.isFocused() && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN) { submitSearch(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
