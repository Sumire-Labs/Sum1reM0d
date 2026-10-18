package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;

/** Shared max-stack override for vanilla item instances and NeoForge's ItemStack override. */
public final class StackableFluids {
    private StackableFluids() {}

    public static int overrideMaxStackSize(ItemInstance instance) {
        if (!Rules.on("stackable_fluids")) return -1;
        var item = instance.typeHolder().value();
        boolean fluidContainer = item instanceof BucketItem && !instance.is(Items.BUCKET);
        boolean potion = instance.is(Items.POTION) || instance.is(Items.SPLASH_POTION) || instance.is(Items.LINGERING_POTION);
        if (!(fluidContainer || potion || instance.is(Items.MILK_BUCKET) || instance.is(Items.POWDER_SNOW_BUCKET))) return -1;
        return Math.max(1, Math.min(64, Rules.i("stackable_fluids", "stack_size")));
    }
}
