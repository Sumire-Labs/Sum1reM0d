package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingPotionSlotMixin extends Slot {
    protected BrewingPotionSlotMixin(Container c, int slot, int x, int y) { super(c, slot, x, y); }
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$place(ItemStack stack, CallbackInfoReturnable<Boolean> cir) { if (Rules.on("single_brewing") && getContainerSlot() != 0) cir.setReturnValue(false); }
    @Override public boolean isActive() { return !Rules.on("single_brewing") || getContainerSlot() == 0; }
}
