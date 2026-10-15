package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class SmeltingRules {
    private SmeltingRules() {}
    public static boolean allowed(ServerLevel level, RecipeType<?> type, ItemStack ingredient) {
        if (!Rules.on("exclusive_smelting") || type != RecipeType.SMELTING) return true;
        var input = new SingleRecipeInput(ingredient);
        return level.recipeAccess().getRecipeFor(RecipeType.BLASTING, input, level).isEmpty()
            && level.recipeAccess().getRecipeFor(RecipeType.SMOKING, input, level).isEmpty();
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RecipeHolder<? extends AbstractCookingRecipe> recipe(ServerLevel level, RecipeType<? extends AbstractCookingRecipe> type, ItemStack ingredient) {
        if (ingredient.isEmpty() || !allowed(level, type, ingredient)) return null;
        return (RecipeHolder<? extends AbstractCookingRecipe>) level.recipeAccess().getRecipeFor((RecipeType) type, new SingleRecipeInput(ingredient), level).orElse(null);
    }
    public static int requiredFuel(double credit, double cost) { return FuelAccounting.requiredFuel(credit, cost); }
    public static double remainingCredit(double credit, double cost, int paid) { return FuelAccounting.remainingCredit(credit, cost, paid); }
}
