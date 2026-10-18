package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CatMeatItem extends Item {
    private final boolean cooked;
    public CatMeatItem(Properties properties, boolean cooked) { super(properties); this.cooked = cooked; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!cooked) FoodContent.consumeRawCatMeat(entity);
        return result;
    }
}
