package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.sumirelabs.sum1rem0d.game.DeliveryBoxMenu;
import com.sumirelabs.sum1rem0d.config.ClientGameState;

/** Japanese mailbox GUI with ordinary item slots and owner-only nearby access management. */
public final class DeliveryBoxScreen extends AbstractContainerScreen<DeliveryBoxMenu> {
    private JsonArray candidates = new JsonArray();
    private int selected;
    private Button target, permission;

    public DeliveryBoxScreen(DeliveryBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 280, 166);
    }
    private JsonObject selectedPlayer() {
        return candidates.isEmpty() ? null : candidates.get(Math.floorMod(selected,candidates.size())).getAsJsonObject();
    }
    private boolean isOwner() { return menu.box != null && menu.box.owner() != null && minecraft.player != null && menu.box.owner().equals(minecraft.player.getUUID()); }
    private boolean permitted(String id) {
        var allowed = ShopClient.state().getAsJsonArray("deliveryBoxPermitted");
        if (allowed == null) return false;
        for (var entry : allowed) if (entry.getAsString().equals(id)) return true;
        return false;
    }
    @Override protected void init() {
        super.init();
        var gameState=ClientGameState.snapshot();
        var economy = gameState.has("economy")?gameState.getAsJsonObject("economy"):new JsonObject();
        candidates = economy != null && economy.has("deliveryPlayers") ? economy.getAsJsonArray("deliveryPlayers") : new JsonArray();
        target = addRenderableWidget(Button.builder(Component.literal("近くのプレイヤーを選択"), b -> {
            if (!candidates.isEmpty()) selected=(selected+1)%candidates.size(); updateButtons();
        }).bounds(leftPos+188,topPos+25,84,20).build());
        permission = addRenderableWidget(Button.builder(Component.literal("許可"), b -> {
            var row=selectedPlayer();if(row==null||minecraft.player==null)return;
            var body=new JsonObject();body.addProperty("target",row.get("id").getAsString());body.addProperty("allow",!permitted(row.get("id").getAsString()));
            body.addProperty("boxPos",menu.boxPos.asLong());body.addProperty("boxDimension",minecraft.level.dimension().identifier().toString());
            ShopClient.request("box_permission",body);updateButtons();
        }).bounds(leftPos+188,topPos+50,84,20).build());
        updateButtons();
        ShopClient.refresh();
    }
    private void updateButtons() {
        var row=selectedPlayer();boolean owner=isOwner();
        target.active=owner&&row!=null;
        if(row==null) target.setMessage(Component.literal("近くに対象者がいません"));
        else target.setMessage(Component.literal(net.minecraft.client.Minecraft.getInstance().font.plainSubstrByWidth(row.get("name").getAsString(),76)));
        permission.active=owner&&row!=null&&!ShopClient.blocking();
        permission.setMessage(Component.literal(row!=null&&permitted(row.get("id").getAsString())?"許可を解除":"利用を許可"));
    }
    @Override protected void containerTick() { super.containerTick(); if(target!=null)updateButtons(); }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta) {
        super.extractBackground(g,x,y,delta);
        VanillaGuiParts.panel(g,leftPos,topPos,imageWidth,imageHeight);
        for(var slot:menu.slots) VanillaGuiParts.slot(g,leftPos+slot.x,topPos+slot.y);
        g.text(font,"宅配BOXの中身",leftPos+8,topPos+5,0xFF404040,false);
        g.text(font,"所持品",leftPos+8,topPos+73,0xFF404040,false);
        g.text(font,isOwner()?"許可プレイヤー設定":"所有者だけが許可を変更できます",leftPos+188,topPos+7,0xFF404040,false);
        var row=selectedPlayer();
        if(row!=null)g.text(font,permitted(row.get("id").getAsString())?"現在: 許可済み":"現在: 許可なし",leftPos+188,topPos+77,0xFF404040,false);
        g.text(font,"満杯時は注文の残数が保たれます",leftPos+8,topPos+157,0xFF606060,false);
    }
}
