package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface InstantFurnace {
    record CostSnapshot(ItemStack input, ItemStack fuel, double credit, boolean preview, RecipeHolder<?> pending, long tick, int operations) {}
    void sum1rem0d$refresh();
    boolean sum1rem0d$isPreview();
    boolean sum1rem0d$consume(boolean transactional);
    CostSnapshot sum1rem0d$snapshotCosts();
    void sum1rem0d$restoreCosts(CostSnapshot snapshot);
    void sum1rem0d$finishCommit();
}
