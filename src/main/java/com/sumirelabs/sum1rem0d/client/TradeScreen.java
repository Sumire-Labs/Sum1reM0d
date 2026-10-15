package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.game.TradeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private EditBox amount;
    private Button accept;
    private long revision=-1;
    public TradeScreen(TradeMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,230);}
    private JsonObject trade(){var state=WalletScreen.state();return state.has("trade")?state.getAsJsonObject("trade"):new JsonObject();}
    @Override protected void init(){super.init();amount=addRenderableWidget(new EditBox(font,leftPos+8,topPos+94,80,20,Component.literal("提示コイン")));amount.setMaxLength(10);amount.setValue(Long.toString(WalletScreen.number(trade(),"ownCoins")));
        addRenderableWidget(Button.builder(Component.literal("金額を提示"),b->{try{send("offer",Long.parseLong(amount.getValue()));}catch(NumberFormatException ignored){}}).bounds(leftPos+92,topPos+94,76,20).build());
        accept=addRenderableWidget(Button.builder(Component.literal("内容を承諾"),b->send("confirm",0)).bounds(leftPos+8,topPos+118,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("キャンセル"),b->onClose()).bounds(leftPos+92,topPos+118,76,20).build());
    }
    private void send(String action,long coins){if(revision>=0)WalletScreen.request(action,"",coins,revision);}
    @Override protected void containerTick(){super.containerTick();var t=trade();long next=WalletScreen.number(t,"revision");if(next!=revision){revision=next;amount.setValue(Long.toString(WalletScreen.number(t,"ownCoins")));}if(t.has("other")) accept.setTooltip(Tooltip.create(Component.literal("取引相手: "+t.get("other").getAsString()+"。双方が承諾すると成立します")));accept.active=t.has("revision")&&!t.get("accepted").getAsBoolean();accept.setMessage(Component.literal(t.has("accepted")&&t.get("accepted").getAsBoolean()?"承諾済み":"内容を承諾"));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float tick){super.extractBackground(g,x,y,tick);VanillaGuiParts.panel(g,leftPos,topPos,imageWidth,imageHeight);for(var slot:menu.slots)VanillaGuiParts.slot(g,leftPos+slot.x,topPos+slot.y);var t=trade();g.text(font,"自分の提示",leftPos+8,topPos+20,0xFF404040,false);g.text(font,"相手の提示",leftPos+108,topPos+20,0xFF404040,false);g.text(font,WalletScreen.number(t,"ownCoins")+" C",leftPos+8,topPos+84,0xFF604000,false);g.text(font,WalletScreen.number(t,"otherCoins")+" C"+(t.has("otherAccepted")&&t.get("otherAccepted").getAsBoolean()?" ✓":""),leftPos+108,topPos+84,0xFF604000,false);}
}
