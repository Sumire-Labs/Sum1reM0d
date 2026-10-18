package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;

/** Vanilla bundle behavior with insertion restricted to bottled milk. */
public final class MilkBundleItem extends BundleItem {
    public MilkBundleItem(Properties properties) { super(properties); }

    @Override public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();
        if (target.isEmpty()) return super.overrideStackedOnOther(bundle, slot, action, player);
        if (!target.is(FoodContent.item("bottled_milk")) || !bundleEnabled()) return false;
        return super.overrideStackedOnOther(bundle, slot, action, player);
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack bundle, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (!other.isEmpty() && (!other.is(FoodContent.item("bottled_milk")) || !bundleEnabled())) return false;
        return super.overrideOtherStackedOnMe(bundle, other, slot, action, player, access);
    }

    private static boolean bundleEnabled() { return Rules.on("bottled_milk") && Rules.b("bottled_milk", "milk_bundle_enabled"); }
}
