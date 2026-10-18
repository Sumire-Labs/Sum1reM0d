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
  private int selected = -1, firstVisible;
  private final java.util.function.Function<SearchSuggestions.Entry,net.minecraft.world.item.ItemStack> icons;
  private final Map<String,net.minecraft.world.item.ItemStack> iconCache=new HashMap<>();
  private boolean dismissed;

  SearchSuggestionPopup(
      EditBox input,
      List<SearchSuggestions.Entry> entries,
      int screenHeight,
      Consumer<String> open) {
    this(input,entries,screenHeight,open,SearchSuggestionPopup::defaultIcon);
  }
  SearchSuggestionPopup(EditBox input,List<SearchSuggestions.Entry> entries,int screenHeight,Consumer<String> open,java.util.function.Function<SearchSuggestions.Entry,net.minecraft.world.item.ItemStack> icons) {
    this.icons=icons;
    this.input = input;
    this.entries = entries;
    this.open = open;
    limit = Math.max(1, Math.min(PersonalConfig.store().integer("wiki","search_suggestion_rows"), (screenHeight - input.getY() - input.getHeight() - 44) / 26));
  }

  void changed(String value) {
    matches = SearchSuggestions.find(entries, value, entries.size());
    selected = -1; firstVisible=0;
    dismissed = false;
  }

  void dismiss() { dismissed = true; }

  private boolean contains(double x, double y) { return x >= input.getX() && x < input.getX() + input.getWidth() && y >= top() && y < top() + shown() * 26 + 14; }

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
        || y >= top() + shown() * 26) return -1;
    return firstVisible+(int) (y - top()) / 26;
  }

  boolean click(MouseButtonEvent event) {
    if (input.isMouseOver(event.x(), event.y())) {
      dismissed = false;
      return false;
    }
    if (!visible()) return false;
    if(matches.size()>limit && event.x()>=input.getX()+input.getWidth()-7 && event.x()<input.getX()+input.getWidth() && event.y()>=top() && event.y()<top()+shown()*26){
      firstVisible=Math.clamp((int)((event.y()-top())/(shown()*26)*(matches.size()-shown())),0,matches.size()-shown());return true;
    }
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
      selected = selected<0?firstVisible:(selected + 1) % matches.size();
      ensureVisible();
      return true;
    }
    if (key == InputConstants.KEY_UP) {
      selected = selected < 0 ? matches.size() - 1 : Math.floorMod(selected - 1, matches.size());
      ensureVisible();
      return true;
    }
    if (key == InputConstants.KEY_TAB) {
      int index = selected < 0 ? firstVisible : selected;
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
    int steps=Math.max(1,(int)Math.ceil(Math.abs(amount)));
    firstVisible=Math.clamp(firstVisible+(amount<0?steps:-steps),0,Math.max(0,matches.size()-shown()));
    if(selected>=0)selected=Math.clamp(selected,firstVisible,firstVisible+shown()-1);
    return true;
  }

  void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
    if (!visible()) return;
    g.nextStratum();
    int x = input.getX(), y = top(), width = input.getWidth(), height = shown() * 26 + 14;
    g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF8B8B8B);
    g.fill(x, y, x + width, y + height, 0xFF202020);
    int hovered = row(mouseX, mouseY);
    for (int visible=0;visible<shown();visible++) {
      int i=firstVisible+visible, at=y+visible*26;
      if (i == (hovered>=0?hovered:selected)) g.fill(x + 1, at, x + width - 1, at + 26, 0xFF40566B);
      var entry = matches.get(i);
      var icon=PersonalConfig.store().bool("wiki","search_suggestion_icons")?iconCache.computeIfAbsent(entry.id(),id->icons.apply(entry)):net.minecraft.world.item.ItemStack.EMPTY; int inset=icon.isEmpty()?5:26;
      if(!icon.isEmpty())g.item(icon,x+5,at+5);
      g.text(
          font,
          font.plainSubstrByWidth(entry.label(), width-inset-10),
          x + inset,
          at + 3,
          0xFFFFFFFF,
          false);
      g.text(
          font, font.plainSubstrByWidth(entry.id(), width-inset-10), x + inset, at + 14, 0xFFB8C6DA, false);
    }
    if(matches.size()>limit){
      int track=shown()*26,thumb=Math.max(8,track*shown()/matches.size()),offset=(track-thumb)*firstVisible/(matches.size()-shown());
      g.fill(x+width-5,y,x+width-1,y+track,0xFF101010);g.fill(x+width-5,y+offset,x+width-1,y+offset+thumb,0xFFB8C6DA);
    }
    g.text(
        font,
        font.plainSubstrByWidth((firstVisible+1)+"–"+(firstVisible+shown())+" / "+matches.size()+" · ホイール / ↑↓ / Enter / Tab", width - 8),
        x + 4,
        y + shown() * 26 + 3,
        0xFFB8C6DA,
        false);
  }
  private int shown(){return Math.min(limit,matches.size());}
  private void ensureVisible(){if(selected<firstVisible)firstVisible=selected;else if(selected>=firstVisible+shown())firstVisible=selected-shown()+1;}
  static net.minecraft.world.item.ItemStack defaultIcon(SearchSuggestions.Entry entry){
    String id=entry.id();var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
    for(String candidate:id.contains(":")?List.of(id):List.of("sum1rem0d:"+id,"minecraft:"+id)){
      var key=net.minecraft.resources.Identifier.tryParse(candidate);if(key==null)continue;var item=registry.getOptional(key).orElse(net.minecraft.world.item.Items.AIR);
      if(item!=net.minecraft.world.item.Items.AIR&&item.builtInRegistryHolder().areComponentsBound())return new net.minecraft.world.item.ItemStack(item);
    }
    return net.minecraft.world.item.ItemStack.EMPTY;
  }
}
