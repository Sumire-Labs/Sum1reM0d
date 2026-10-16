package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.CraftingMaterialSwap;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RecipeMap.class)
public abstract class CraftingMaterialMapMixin {
 @Shadow public abstract <I extends RecipeInput,T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type);
 @Inject(method="getRecipesFor",at=@At("HEAD"),cancellable=true)
 private <I extends RecipeInput,T extends Recipe<I>> void sum1rem0d$recipes(RecipeType<T> type,I input,Level level,CallbackInfoReturnable<Stream<RecipeHolder<T>>> cir) { if(input instanceof CraftingInput crafting){RecipeHolder<T> special=com.sumirelabs.sum1rem0d.game.DyeTextiles.priority(crafting,level);if(special!=null){cir.setReturnValue(Stream.of(special));return;}} if(input instanceof CraftingInput && com.sumirelabs.sum1rem0d.game.Rules.on("paper_leather_swap")) cir.setReturnValue(input.isEmpty()?Stream.empty():byType(type).stream().filter(r->CraftingMaterialSwap.matches(r.value(),input,level))); }
}
