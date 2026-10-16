package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.CraftingMaterialSwap;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlaceRecipe.class)
public abstract class MaterialRecipePlacementMixin {
 @Shadow private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<?> recipe,StackedItemContents items) {throw new AssertionError();}
 @Inject(method="tryPlaceRecipe",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$excluded(RecipeHolder<?> recipe,StackedItemContents items,CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) { if(CraftingMaterialSwap.ingredientSwap() && CraftingMaterialSwap.excluded(recipe)) cir.setReturnValue(CraftingMaterialSwap.original(()->tryPlaceRecipe(recipe,items))); }
}
