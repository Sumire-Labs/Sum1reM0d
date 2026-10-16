package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import java.util.Optional;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RecipeManager.class)
public abstract class CraftingMaterialMatchMixin {
 @Shadow public abstract <I extends RecipeInput,T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type,I input,Level level);
 @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",at=@At("HEAD"),cancellable=true)
 private <I extends RecipeInput,T extends Recipe<I>> void sum1rem0d$hint(RecipeType<T> type,I input,Level level,RecipeHolder<T> hint,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) { if(input instanceof CraftingInput crafting){RecipeHolder<T> special=DyeTextiles.priority(crafting,level);if(special!=null){cir.setReturnValue(Optional.of(special));return;}} if(input instanceof CraftingInput && Rules.on("paper_leather_swap")) cir.setReturnValue(hint!=null && CraftingMaterialSwap.matches(hint.value(),input,level)?Optional.of(hint):getRecipeFor(type,input,level)); }
}
