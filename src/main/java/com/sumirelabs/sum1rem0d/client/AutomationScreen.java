package com.sumirelabs.sum1rem0d.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.sumirelabs.sum1rem0d.game.AutomationMenu;

/** Small localized machine-pattern/tool UI. */
public final class AutomationScreen extends AbstractContainerScreen<AutomationMenu> {
    private Button mode;
    public AutomationScreen(AutomationMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,166);}
    @Override protected void init(){super.init();if(menu.machine!=null&&menu.machine.kind().equals("auto_clicker")){mode=addRenderableWidget(Button.builder(Component.literal(modeLabel()),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+90,topPos+21,78,20).build());}}
    private String modeLabel(){return menu.machine!=null&&menu.machine.leftClick()?"左クリック":"右クリック";}
    @Override protected void containerTick(){super.containerTick();if(mode!=null)mode.setMessage(Component.literal(modeLabel()));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){super.extractBackground(g,x,y,delta);VanillaGuiParts.panel(g,leftPos,topPos,imageWidth,imageHeight);for(var slot:menu.slots)VanillaGuiParts.slot(g,leftPos+slot.x,topPos+slot.y);g.text(font,"機械設定 / パターン",leftPos+8,topPos+5,0xFF404040,false);g.text(font,"持ち物",leftPos+8,topPos+73,0xFF404040,false);}
}
