package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.wiki.WikiContent;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.platform.InputConstants;

/** Offline reader with category navigation, search, pagination and live rule summaries. */
public final class WikiScreen extends Screen {
    private final Screen parent;
    private String view = "intro", category, feature, query = "", searchText = "";
    private int page, navigationPage, pages = 1;
    private int resultsCount;
    private int left, top, navWidth, right, contentWidth, panelHeight, bodyY, rows;
    private long serverRevision, personalRevision;
    private EditBox search;
    private List<FormattedCharSequence> lines = List.of();
    public WikiScreen(Screen parent) { super(Component.literal("Sum1reM0d Wiki")); this.parent = parent; }
    String view() { return view; }
    String feature() { return feature; }
    int page() { return page; }
    int pages() { return pages; }
    private void navigate(String view, String category, String feature) { this.view = view; this.category = category; this.feature = feature; page = 0; rebuildWidgets(); }
    private void article(String id) { navigate("article", WikiContent.categoryFor(id).id(), id); }
    private void category(String id) { query = ""; searchText = ""; navigate("category", id, null); }
    @Override protected void init() {
        int totalWidth = Math.min(760, width - 20);
        left = (width - totalWidth) / 2; top = 36; panelHeight = Math.max(100, height - 70);
        navWidth = Math.min(132, Math.max(104, totalWidth / 3)); right = left + navWidth + 6; contentWidth = totalWidth - navWidth - 6;
        serverRevision = ClientRules.revision; personalRevision = PersonalConfig.store().revision();
        search = addRenderableWidget(new EditBox(font, right, 8, Math.max(40, contentWidth - 52), 20, Component.literal("Wikiを検索")));
        search.setMaxLength(80); search.setHint(Component.literal("機能名・説明を検索")); search.setValue(searchText); search.setResponder(value -> searchText = value);
        addRenderableWidget(Button.builder(Component.literal("検索"), button -> submitSearch()).bounds(right + contentWidth - 48, 8, 48, 20).build());
        navButton("導入", top + 8, () -> navigate("intro", null, null), view.equals("intro"));
        navButton("カテゴリ一覧", top + 30, () -> navigate("categories", null, null), view.equals("categories"));
        int visible = Math.max(1, (panelHeight - 82) / 20), count = WikiContent.categories().size();
        int navPages = Math.max(1, (count + visible - 1) / visible); navigationPage = Math.min(navigationPage, navPages - 1);
        for (int i = 0; i < visible && navigationPage * visible + i < count; i++) {
            var item = WikiContent.categories().get(navigationPage * visible + i);
            navButton(item.title(), top + 56 + i * 20, () -> category(item.id()), item.id().equals(category));
        }
        if (navPages > 1) {
            var previous = addRenderableWidget(Button.builder(Component.literal("◀"), button -> { navigationPage--; rebuildWidgets(); }).bounds(left + 8, top + panelHeight - 24, 30, 18).build()); previous.active = navigationPage > 0;
            var next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> { navigationPage++; rebuildWidgets(); }).bounds(left + navWidth - 38, top + panelHeight - 24, 30, 18).build()); next.active = navigationPage + 1 < navPages;
        }
        bodyY = top + (view.equals("article") ? 44 : 32);
        int available = Math.max(20, top + panelHeight - 30 - bodyY);
        if (view.equals("intro") || view.equals("article")) {
            var paragraphs = view.equals("intro") ? WikiContent.introduction() : articleParagraphs();
            var wrapped = new ArrayList<FormattedCharSequence>();
            for (String paragraph : paragraphs) { wrapped.addAll(font.split(Component.literal(paragraph), contentWidth - 18)); wrapped.add(FormattedCharSequence.EMPTY); }
            lines = List.copyOf(wrapped); rows = Math.max(1, available / 11); pages = Math.max(1, (lines.size() + rows - 1) / rows); page = Math.min(page, pages - 1);
        } else if (view.equals("categories")) {
            rows = Math.max(1, available / 22); pages = Math.max(1, (WikiContent.categories().size() + rows - 1) / rows); page = Math.min(page, pages - 1);
            for (int i = 0; i < rows && page * rows + i < count; i++) {
                var item = WikiContent.categories().get(page * rows + i);
                var button = addRenderableWidget(Button.builder(Component.literal(item.title() + " / " + item.features().size() + "機能"), widget -> category(item.id())).bounds(right + 8, bodyY + i * 22, contentWidth - 16, 20).build());
                button.setTooltip(Tooltip.create(Component.literal(item.description())));
            }
        } else {
            var articles = view.equals("search") ? WikiContent.search(query) : WikiContent.category(category).features().stream().map(WikiContent::article).toList();
            resultsCount = articles.size();
            rows = Math.max(1, available / 22); pages = Math.max(1, (articles.size() + rows - 1) / rows); page = Math.min(page, pages - 1);
            for (int i = 0; i < rows && page * rows + i < articles.size(); i++) {
                var item = articles.get(page * rows + i);
                String label = item.title() + (enabled(item.feature()) ? " [ON]" : " [OFF]");
                var button = addRenderableWidget(Button.builder(Component.literal(label), widget -> article(item.feature())).bounds(right + 8, bodyY + i * 22, contentWidth - 16, 20).build());
                button.setTooltip(Tooltip.create(Component.literal(item.paragraphs().getFirst())));
            }
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("◀"), button -> turn(-1)).bounds(right + 8, top + panelHeight - 24, 32, 18).build()); previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.literal("▶"), button -> turn(1)).bounds(right + contentWidth - 40, top + panelHeight - 24, 32, 18).build()); next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.literal("閉じる"), button -> minecraft.gui.setScreen(parent)).bounds(left, height - 26, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("個人設定"), button -> minecraft.gui.setScreen(new ConfigScreen(this, true))).bounds(left + 86, height - 26, 88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("サーバー設定"), button -> minecraft.gui.setScreen(new ConfigScreen(this))).bounds(left + 180, height - 26, 100, 20).build());
    }
    private void navButton(String label, int y, Runnable action, boolean selected) {
        var button = addRenderableWidget(Button.builder(Component.literal(label), widget -> action.run()).bounds(left + 6, y, navWidth - 12, 18).build()); button.active = !selected;
    }
    private void submitSearch() { query = searchText.strip(); navigate(query.isEmpty() ? "categories" : "search", null, null); }
    private boolean enabled(String id) { return PersonalConfig.personal(id, "enabled") ? PersonalConfig.store().enabled(id) : ClientRules.enabled(id); }
    private List<String> articleParagraphs() {
        var result = new ArrayList<>(WikiContent.article(feature).paragraphs()); result.add("現在の設定（説明中の数値は初期値の例です）");
        var personalValues = PersonalConfig.store().snapshot().getAsJsonObject("features").getAsJsonObject(feature);
        var commonValues = ClientRules.snapshot().getAsJsonObject("features").getAsJsonObject(feature);
        for (var option : WikiContent.schema(feature).options()) {
            boolean personal = PersonalConfig.personal(feature, option.key());
            var values = personal ? personalValues : commonValues;
            String value = values.get(option.key()).getAsString();
            if (option.isBoolean()) value = values.get(option.key()).getAsBoolean() ? "ON" : "OFF";
            else if (option.kind() == ConfigSchema.Kind.LIST || option.kind() == ConfigSchema.Kind.MAP) value = ConfigSchema.split(value).size() + "件";
            else if (value.length() > 70) value = value.substring(0, 67) + "...";
            result.add(option.label() + ": " + value + (personal ? "（個人）" : minecraft.getConnection() == null ? "（ローカル）" : "（サーバー）"));
        }
        result.add("設定の反映: " + ConfigTiming.description(feature, "enabled")); return result;
    }
    private void turn(int change) { page = Math.max(0, Math.min(pages - 1, page + change)); rebuildWidgets(); }
    @Override public void tick() {
        if (!WikiRuntime.enabled()) { minecraft.gui.setScreen(parent); return; }
        if (serverRevision != ClientRules.revision || personalRevision != PersonalConfig.store().revision()) rebuildWidgets();
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { if (vertical != 0) { turn(vertical < 0 ? 1 : -1); return true; } return super.mouseScrolled(x, y, horizontal, vertical); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN && search.isFocused()) { submitSearch(); return true; }
        if (!search.isFocused() && event.key() == InputConstants.KEY_RIGHT) { turn(1); return true; }
        if (!search.isFocused() && event.key() == InputConstants.KEY_LEFT) { turn(-1); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() {
        if (view.equals("article")) category(WikiContent.categoryFor(feature).id());
        else if (!view.equals("intro")) navigate("intro", null, null);
        else minecraft.gui.setScreen(parent);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, com.sumirelabs.sum1rem0d.game.ModContent.id("icon.png"), left + 4, 9, 0, 0, 16, 16, 256, 256, 256, 256);
        graphics.text(font, title, left + 24, 13, 0xFFFFFFFF, false);
        VanillaGuiParts.panel(graphics, left, top, navWidth, panelHeight); VanillaGuiParts.panel(graphics, right, top, contentWidth, panelHeight);
        // Draw native widgets over the panel frames.
        super.extractRenderState(graphics, mouseX, mouseY, tick);
        String heading = switch (view) { case "intro" -> "はじめに / 導入"; case "categories" -> "カテゴリ一覧"; case "category" -> WikiContent.category(category).title(); case "article" -> WikiContent.article(feature).title(); default -> "検索: " + query; };
        graphics.textWithWordWrap(font, Component.literal(heading), right + 8, top + 7, contentWidth - 16, 0xFF303030);
        if (view.equals("article")) {
            String state = enabled(feature) ? "設定: ON" : "設定: OFF";
            if (com.sumirelabs.sum1rem0d.game.DailyEvents.FEATURES.contains(feature) && ClientGameState.received()) state += ClientGameState.daily(feature) ? " / 今日の異変" : " / 本日は非発動";
            graphics.text(font, state, right + 8, top + 29, enabled(feature) ? 0xFF236B35 : 0xFF7A3838, false);
        }
        if (view.equals("intro") || view.equals("article")) {
            for (int i = 0; i < rows && page * rows + i < lines.size(); i++) graphics.text(font, lines.get(page * rows + i), right + 9, bodyY + i * 11, 0xFF404040, false);
        } else if (view.equals("search") && resultsCount == 0) graphics.text(font, "見つかりませんでした", right + 9, bodyY, 0xFF404040, false);
        graphics.centeredText(font, (page + 1) + " / " + pages, right + contentWidth / 2, top + panelHeight - 19, 0xFF404040);
    }
}
