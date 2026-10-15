package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingMixin {
    @Shadow private NonNullList<ItemStack> items;
    @Shadow private int brewTime, fuel;
    //? if >=26.3 {
    @Shadow private int totalBrewTime, totalFuel;
    //?} else {
    /*@Unique private int totalBrewTime, totalFuel;
    *///?}
    @Shadow private Item ingredient;
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    //? if >=26.3 {
    private static void sum1rem0d$brew(ServerLevel level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, CallbackInfo ci) {
    //?} else {
    /*private static void sum1rem0d$brew(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, CallbackInfo ci) {
    *///?}
        if (!Rules.on("single_brewing")) return;
        var self = (BrewingMixin) (Object) entity;
        for (int slot = 1; slot <= 2; slot++) if (!self.items.get(slot).isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, self.items.get(slot)); self.items.set(slot, ItemStack.EMPTY);
        }
        //? if >=26.3 {
        var input = new BrewingInput(self.items.get(0), self.items.get(3));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.BREWING, input, level);
        boolean canBrew = recipe.isPresent();
        //?} else {
        /*boolean canBrew = level.potionBrewing().hasMix(self.items.get(0), self.items.get(3));
        *///?}
        self.fuel = 0; self.totalFuel = 0;
        if (self.brewTime > 0) {
            if (!canBrew || !self.items.get(3).is(self.ingredient)) self.brewTime = 0;
            else if (--self.brewTime == 0) {
                //? if >=26.3 {
                self.items.set(0, recipe.get().value().assemble(input));
                //?} else {
                /*self.items.set(0, level.potionBrewing().mix(self.items.get(3), self.items.get(0)));
                *///?}
                var reagent = self.items.get(3); var remainder = reagent.getCraftingRemainder(); reagent.shrink(1);
                if (remainder != null) { if (reagent.isEmpty()) self.items.set(3, remainder.create()); else Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create()); }
                level.levelEvent(1035, pos, 0);
            }
        } else if (canBrew && self.items.get(4).is(Items.BLAZE_POWDER) && self.items.get(4).getCount() >= Rules.i("single_brewing", "fuel_cost")) {
            self.items.get(4).shrink(Rules.i("single_brewing", "fuel_cost"));
            self.brewTime = Rules.i("single_brewing", "seconds") * 20; self.totalBrewTime = self.brewTime; self.ingredient = self.items.get(3).getItem();
        }
        if (state.getBlock() instanceof BrewingStandBlock) {
            BlockState next = state.setValue(BrewingStandBlock.HAS_BOTTLE[0], !self.items.get(0).isEmpty()).setValue(BrewingStandBlock.HAS_BOTTLE[1], false).setValue(BrewingStandBlock.HAS_BOTTLE[2], false);
            if (next != state) level.setBlock(pos, next, 2);
        }
        entity.setChanged(); ci.cancel();
    }
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$slots(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) { if (Rules.on("single_brewing") && (slot == 1 || slot == 2)) cir.setReturnValue(false); }
    @Inject(method = "getSlotsForFace", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$automation(Direction side, CallbackInfoReturnable<int[]> cir) {
        if (Rules.on("single_brewing")) cir.setReturnValue(side == Direction.UP ? new int[]{3} : side == Direction.DOWN ? new int[]{0, 3} : new int[]{0, 4});
    }
}
