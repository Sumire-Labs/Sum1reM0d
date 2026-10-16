package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Ingredient.class)
public abstract class PaperLeatherIngredientMixin {
 @ModifyVariable(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),argsOnly=true)
 private ItemStack sum1rem0d$material(ItemStack stack) { return NetherChanges.swapMaterial(stack); }
 @ModifyVariable(method="acceptsItem",at=@At("HEAD"),argsOnly=true)
 private Holder<Item> sum1rem0d$placement(Holder<Item> item) { return CraftingMaterialSwap.ingredientSwap() ? (item.value()==Items.PAPER ? Items.LEATHER.builtInRegistryHolder() : item.value()==Items.LEATHER ? Items.PAPER.builtInRegistryHolder() : item) : item; }
 @Inject(method="display",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$display(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.crafting.display.SlotDisplay> cir) {
  if(!CraftingMaterialSwap.ingredientSwap()) return;
  var items=((Ingredient)(Object)this).items().toList();
  if(items.stream().noneMatch(h->h.value()==Items.PAPER || h.value()==Items.LEATHER)) return;
  cir.setReturnValue(new net.minecraft.world.item.crafting.display.SlotDisplay.Composite(items.stream().map(h->(net.minecraft.world.item.crafting.display.SlotDisplay)new net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay(h.value()==Items.PAPER?Items.LEATHER.builtInRegistryHolder():h.value()==Items.LEATHER?Items.PAPER.builtInRegistryHolder():h)).toList()));
 }
}
