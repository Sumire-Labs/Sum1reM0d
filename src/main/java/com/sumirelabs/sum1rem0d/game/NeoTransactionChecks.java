package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
//? if neoforge {
/*import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.Transaction;
*///?}

public final class NeoTransactionChecks {
    private NeoTransactionChecks() {}
    public static boolean verify(AbstractFurnaceBlockEntity furnace) {
        boolean verified = false;
        //? if neoforge {
        /*furnace.setItem(0, new ItemStack(Items.SAND)); furnace.setItem(1, new ItemStack(Items.COAL, 5));
        var wrapper = VanillaContainerWrapper.of(furnace);
        ((InstantFurnace) furnace).sum1rem0d$refresh();
        try (var transaction = Transaction.openRoot()) {
            ServerVerification.require(wrapper.extract(2, ItemResource.of(Items.GLASS), 1, transaction) == 1, "Transactional output extraction");
            ServerVerification.require(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty(), "Transactional cost reserved immediately");
        }
        ServerVerification.require(furnace.getItem(0).getCount() == 1 && furnace.getItem(1).getCount() == 5, "Aborted transaction restores ingredients and fuel");
        ServerVerification.require(furnace.getItem(2).is(Items.GLASS), "Aborted transaction restores preview");
        try (var transaction = Transaction.openRoot()) {
            ServerVerification.require(wrapper.extract(2, ItemResource.of(Items.GLASS), 1, transaction) == 1, "Committed extraction");
            ServerVerification.require(wrapper.extract(0, ItemResource.of(Items.SAND), 1, transaction) == 0, "Cannot extract both unpaid output and its input");
            transaction.commit();
        }
        ServerVerification.require(furnace.getItem(0).isEmpty() && furnace.getItem(1).isEmpty() && furnace.getItem(2).isEmpty(), "Committed transaction charges exactly once");
        verified = true;
        *///?}
        return verified;
    }
}
