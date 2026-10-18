package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Dyes bundles or shulker boxes by copying the original stack so container contents survive. */
public final class MixedContainersRecipe extends CustomRecipe {
    public static final RecipeSerializer<MixedContainersRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(MixedContainersRecipe::new), StreamCodec.unit(new MixedContainersRecipe()));

    @Override public boolean matches(CraftingInput input, Level level) { return !assemble(input).isEmpty(); }

    @Override public ItemStack assemble(CraftingInput input) {
        if (!Rules.on("mixed_containers")) return ItemStack.EMPTY;
        ItemStack container = ItemStack.EMPTY;
        var dyes = new ArrayList<DyeColor>();
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) {
            if (isContainer(stack)) {
                if (!container.isEmpty()) return ItemStack.EMPTY;
                container = stack;
            } else if (stack.get(DataComponents.DYE) != null) dyes.add(stack.get(DataComponents.DYE));
            else return ItemStack.EMPTY;
        }
        if (container.isEmpty() || dyes.isEmpty() || dyes.size() > Rules.i("mixed_containers", "maximum_dyes")) return ItemStack.EMPTY;
        int baseColor = color(container);
        var result = container.copyWithCount(1);
        result.set(DataComponents.DYED_COLOR, DyedItemColor.applyDyes(new DyedItemColor(baseColor), dyes));
        return result;
    }

    private static boolean isContainer(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        boolean bundle = id.getNamespace().equals("minecraft") && (path.equals("bundle") || path.endsWith("_bundle"));
        return bundle && Rules.b("mixed_containers", "bundle_enabled")
                || (path.equals("shulker_box") || path.endsWith("_shulker_box")) && Rules.b("mixed_containers", "shulker_enabled");
    }

    private static int color(ItemStack stack) {
        var existing = stack.get(DataComponents.DYED_COLOR);
        if (existing != null) return existing.rgb() & 0xFFFFFF;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("bundle")) return DyeColor.BROWN.getTextureDiffuseColor() & 0xFFFFFF;
        for (DyeColor color : DyeColor.values()) if (path.equals(color.getName() + "_bundle")) return color.getTextureDiffuseColor() & 0xFFFFFF;
        for (DyeColor color : DyeColor.values()) if (path.startsWith(color.getName() + "_shulker_box")) return color.getTextureDiffuseColor() & 0xFFFFFF;
        return 0xFFFFFF;
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return SERIALIZER; }
}
