package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Read-only mirrors: clicking an inventory slot proposes its complete stack. */
public final class TradeMenu extends AbstractContainerMenu {
    public static final MenuType<TradeMenu> TYPE = new MenuType<>(TradeMenu::new,FeatureFlags.DEFAULT_FLAGS);
    public final Economy.Session session;
    private final SimpleContainer previews = new SimpleContainer(18);
    public TradeMenu(int id,Inventory inventory) { this(id,inventory,null); }
    public TradeMenu(int id,Inventory inventory,Economy.Session session) {
        super(TYPE,id);this.session=session;
        for(int side=0;side<2;side++) for(int row=0;row<3;row++) for(int col=0;col<3;col++) addSlot(readOnly(previews,side*9+row*3+col,8+side*108+col*18,32+row*18));
        // Match vanilla's inventory ordering; all actions remain read-only.
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(readOnly(inventory,9+row*9+col,8+col*18,146+row*18));
        for(int col=0;col<9;col++)addSlot(readOnly(inventory,col,8+col*18,204));
    }
    private Slot readOnly(Container c,int index,int x,int y) { return new Slot(c,index,x,y) { @Override public boolean mayPlace(ItemStack s){return false;} @Override public boolean mayPickup(Player p){return false;} }; }
    void refresh(Economy.Session s,ServerPlayer p) {
        for(int i=0;i<18;i++)previews.setItem(i,ItemStack.EMPTY);
        int i=0;for(var stack:s.offer(p).values())previews.setItem(i++,stack.copy());
        i=9;for(var stack:s.offer(s.other(p)).values())previews.setItem(i++,stack.copy());broadcastChanges();
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player p) {
        if(!(p instanceof ServerPlayer server) || session==null || session.closed)return;
        if(input!=ContainerInput.PICKUP && input!=ContainerInput.QUICK_MOVE)return;
        if(slot>=0&&slot<9)Economy.select(server,slot,true);
        else if(slot>=18&&slot<slots.size())Economy.select(server,slots.get(slot).getContainerSlot(),false);
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return session==null || !session.closed;}
    @Override public void removed(Player p){super.removed(p);if(p instanceof ServerPlayer server && session!=null && !session.closed)Economy.cancel(server,"画面を閉じたため取引をキャンセルしました");}
}
