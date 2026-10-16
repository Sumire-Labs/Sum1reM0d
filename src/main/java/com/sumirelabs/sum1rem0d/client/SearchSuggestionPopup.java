package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import com.sumirelabs.sum1rem0d.wiki.SearchSuggestions;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;

/** Drawn last and hit-tested first so covered buttons cannot steal suggestion clicks. */
final class SearchSuggestionPopup {
  private final EditBox input;
  private final List<SearchSuggestions.Entry> entries;
  private final Consumer<String> open;
  private final int limit;
  private List<SearchSuggestions.Entry> matches = List.of();
  private int selected = -1;
  private boolean dismissed;

  SearchSuggestionPopup(
      EditBox input,
      List<SearchSuggestions.Entry> entries,
      int screenHeight,
      Consumer<String> open) {
    this.input = input;
    this.entries = entries;
    this.open = open;
    limit = Math.max(1, Math.min(5, (screenHeight - input.getY() - input.getHeight() - 44) / 26));
  }

  void changed(String value) {
    matches = SearchSuggestions.find(entries, value, limit);
    selected = -1;
    dismissed = false;
  }

  void dismiss() { dismissed = true; }

  private boolean contains(double x, double y) { return x >= input.getX() && x < input.getX() + input.getWidth() && y >= top() && y < top() + matches.size() * 26 + 14; }

  boolean visible() {
    return input.isFocused()
        && !dismissed
        && !matches.isEmpty()
        && PersonalConfig.store().bool("wiki", "search_suggestions");
  }

  private int top() {
    return input.getY() + input.getHeight() + 2;
  }

  private int row(double x, double y) {
    if (x < input.getX()
        || x >= input.getX() + input.getWidth()
        || y < top()
        || y >= top() + matches.size() * 26) return -1;
    return (int) (y - top()) / 26;
  }

  boolean click(MouseButtonEvent event) {
    if (input.isMouseOver(event.x(), event.y())) {
      dismissed = false;
      return false;
    }
    if (!visible()) return false;
    int index = row(event.x(), event.y());
    if (index >= 0 && event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
    if (index >= 0) {
      open.accept(matches.get(index).id());
      dismissed = true;
      return true;
    }
    if (contains(event.x(), event.y())) return true;
    dismissed = true;
    return false;
  }

  boolean key(KeyEvent event) {
    int key = event.key();
    if (input.isFocused() && (key == InputConstants.KEY_DOWN || key == InputConstants.KEY_UP))
      dismissed = false;
    if (!visible()) return false;
    if (key == InputConstants.KEY_ESCAPE) {
      dismissed = true;
      return true;
    }
    if (key == InputConstants.KEY_DOWN) {
      selected = (selected + 1) % matches.size();
      return true;
    }
    if (key == InputConstants.KEY_UP) {
      selected = selected < 0 ? matches.size() - 1 : Math.floorMod(selected - 1, matches.size());
      return true;
    }
    if (key == InputConstants.KEY_TAB) {
      int index = selected < 0 ? 0 : selected;
      input.setValue(matches.get(index).id());
      input.moveCursorToEnd(false);
      dismissed = true;
      return true;
    }
    if (key == InputConstants.KEY_RETURN && selected >= 0) {
      open.accept(matches.get(selected).id());
      dismissed = true;
      return true;
    }
    return false;
  }

  boolean scroll(double x, double y, double amount) {
    if (!visible() || !contains(x, y) || amount == 0) return false;
    selected = Math.max(0, Math.min(matches.size() - 1, selected + (amount < 0 ? 1 : -1)));
    return true;
  }

  void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
    if (!visible()) return;
    g.nextStratum();
    int x = input.getX(), y = top(), width = input.getWidth(), height = matches.size() * 26 + 14;
    g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF8B8B8B);
    g.fill(x, y, x + width, y + height, 0xFF202020);
    int hovered = row(mouseX, mouseY);
    for (int i = 0; i < matches.size(); i++) {
      int at = y + i * 26;
      if (i == selected || i == hovered) g.fill(x + 1, at, x + width - 1, at + 26, 0xFF40566B);
      var entry = matches.get(i);
      g.text(
          font,
          font.plainSubstrByWidth(entry.label(), width - 10),
          x + 5,
          at + 3,
          0xFFFFFFFF,
          false);
      g.text(
          font, font.plainSubstrByWidth(entry.id(), width - 10), x + 5, at + 14, 0xFFB8C6DA, false);
    }
    g.text(
        font,
        font.plainSubstrByWidth("↑↓ 選択 / Enter 開く / Tab 補完", width - 8),
        x + 4,
        y + matches.size() * 26 + 3,
        0xFFB8C6DA,
        false);
  }
}
