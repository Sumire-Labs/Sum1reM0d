package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.CraftingMaterialSwap;
import java.util.List;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(RecipeManager.class)
public abstract class MaterialRecipeDisplayMixin {
 @Redirect(method="unpackRecipeInfo",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/crafting/Recipe;display()Ljava/util/List;"))
 private static List<RecipeDisplay> sum1rem0d$display(Recipe<?> recipe,Iterable<RecipeHolder<?>> recipes,FeatureFlagSet flags) { return CraftingMaterialSwap.display(recipe,recipes); }
}
