package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Mailbox slots are read-only previews; extraction is a single server journal transaction. */
public final class DeliveryBoxMenu extends AbstractContainerMenu {
    public final DeliveryBoxEntity box;
    public final BlockPos boxPos;

    public DeliveryBoxMenu(int id, Inventory inventory) {
        this(id, inventory, findNearbyBox(inventory));
    }
    public DeliveryBoxMenu(int id, Inventory inventory, DeliveryBoxEntity box) {
        super(DeliveryBoxContent.MENU, id);
        this.box = box;
        this.boxPos = box == null ? BlockPos.ZERO : box.getBlockPos();
        Container mailbox = box == null ? new net.minecraft.world.SimpleContainer(27) : box;
        for (int row=0; row<3; row++) for (int col=0; col<9; col++) {
            int slot = row*9+col;
            addSlot(new Slot(mailbox,slot,8+col*18,18+row*18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        addStandardInventorySlots(inventory,8,84);
    }
    private static DeliveryBoxEntity findNearbyBox(Inventory inventory) {
        var level = inventory.player.level(); var center = inventory.player.blockPosition();
        for (int y=-2;y<=2;y++) for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++)
            if(level.getBlockEntity(center.offset(x,y,z)) instanceof DeliveryBoxEntity box) return box;
        return null;
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot >= 0 && slot < 27) {
            if (player instanceof ServerPlayer server && box != null
                && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
                Economy.takeDeliveryBox(server, box, slot);
                broadcastChanges();
            }
            return;
        }
        super.clicked(slot,button,input,player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 0 && index < 27 && player instanceof ServerPlayer server && box != null) {
            Economy.takeDeliveryBox(server,box,index); broadcastChanges();
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean stillValid(Player player) {
        return box == null ? player.level().isClientSide() : box.stillValid(player)
            && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(box.getBlockPos())) <= 64;
    }
}
