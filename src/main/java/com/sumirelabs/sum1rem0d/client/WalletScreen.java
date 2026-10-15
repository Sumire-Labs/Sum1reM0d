package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.config.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Wallet and nearby trade invitations. Numbers are parsed as exact longs on both sides. */
public final class WalletScreen extends Screen {
    private final Screen parent;
    private EditBox amount;
    private String amountText="10", lastPlayers="";
    private int left,top,page;
    public WalletScreen(Screen parent){super(Component.literal("Sum1reコ↑イン↓ 財布"));this.parent=parent;}
    static JsonObject state(){var root=ClientGameState.snapshot();return root.has("economy")?root.getAsJsonObject("economy"):new JsonObject();}
    static long number(JsonObject json,String key){return json.has(key)?json.get(key).getAsLong():0;}
    static void request(String action,String target,long amount,long revision){PlatformClientNetwork.economy(new com.sumirelabs.sum1rem0d.network.EconomyRequestPayload(action,target,amount,revision));}
    @Override protected void init(){
        left=(width-300)/2;top=Math.max(4,(height-230)/2);
        amount=addRenderableWidget(new EditBox(font,left+8,top+48,110,20,Component.literal("金額")));amount.setMaxLength(10);amount.setValue(amountText);amount.setResponder(v->amountText=v);
        button("1コインで出金",left+124,top+48,80,()->withdraw("coin"));button("10コイン束",left+208,top+48,84,()->withdraw("roll"));
        button("所持コインを全て入金",left+8,top+72,284,()->request("deposit","",0,0));
        var state=state();
        if(state.has("inviteRevision")){
            button("依頼を承諾",left+8,top+112,138,()->request("join","",0,number(state,"inviteRevision")));
            button("断る",left+152,top+112,140,()->request("decline","",0,0));
        }
        var players=state.has("players")?state.getAsJsonArray("players"):new JsonArray();int pages=Math.max(1,(players.size()+1)/2);page=Math.min(page,pages-1);
        for(int i=0;i<2&&page*2+i<players.size();i++){var player=players.get(page*2+i).getAsJsonObject();button(player.get("name").getAsString()+" に取引依頼",left+8,top+152+i*22,284,()->request("invite",player.get("id").getAsString(),0,0));}
        var prev=button("◀",left+8,top+202,30,()->{page--;rebuildWidgets();});prev.active=page>0;
        var next=button("▶",left+42,top+202,30,()->{page++;rebuildWidgets();});next.active=page+1<pages;
        button("閉じる",left+208,top+202,84,this::onClose);
        lastPlayers=players.toString()+state.has("inviteRevision")+(state.has("inviteRevision")?state.get("inviteRevision"):"");
        request("refresh","",0,0);
    }
    private Button button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    private void withdraw(String denomination){try{request("withdraw",denomination,Long.parseLong(amount.getValue()),0);}catch(NumberFormatException ignored){}}
    @Override public void tick(){var s=state();String current=(s.has("players")?s.get("players").toString():"[]")+s.has("inviteRevision")+(s.has("inviteRevision")?s.get("inviteRevision"):"");if(!current.equals(lastPlayers))rebuildWidgets();if(!ClientRules.enabled("economy"))onClose();}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float tick){super.extractBackground(g,x,y,tick);VanillaGuiParts.panel(g,left,top,300,230);g.text(font,title,left+8,top+8,0xFF404040,false);g.item(new net.minecraft.world.item.ItemStack(com.sumirelabs.sum1rem0d.game.ModContent.coin()),left+8,top+24);g.text(font,"残高: "+number(state(),"balance"),left+28,top+27,0xFF604000,false);g.text(font,state().has("inviteName")?state().get("inviteName").getAsString()+" からの依頼":"取引依頼はここに届きます",left+8,top+99,0xFF404040,false);g.text(font,"近くのプレイヤー（"+(page+1)+"）",left+8,top+139,0xFF404040,false);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
