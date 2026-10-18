package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlockEntity.class)
public abstract class CopperChestSizeMixin {
    @Shadow private NonNullList<ItemStack> items;
    @Unique private boolean sum1rem0d$copperExpanded;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sum1rem0d$initializeCopper(CallbackInfo ci) {
        if (sum1rem0d$isCopper() && com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("copper_chest")) sum1rem0d$expand();
    }

    @Inject(method = "getContainerSize", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$copperSize(CallbackInfoReturnable<Integer> cir) {
        if (sum1rem0d$isCopper() && (sum1rem0d$copperExpanded || com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("copper_chest"))) {
            if (items.size() < 72) sum1rem0d$expand();
            cir.setReturnValue(72);
        }
    }

    @Inject(method = "loadAdditional", at = @At("HEAD"))
    private void sum1rem0d$migrateLegacy(ValueInput input, CallbackInfo ci) {
        if (!sum1rem0d$isCopper()) return;
        sum1rem0d$copperExpanded = input.getBooleanOr("sum1rem0d_copper_chest_72", false) || com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("copper_chest");
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void sum1rem0d$finishMigration(ValueInput input, CallbackInfo ci) {
        if (sum1rem0d$isCopper() && sum1rem0d$copperExpanded && items.size() < 72) sum1rem0d$expand();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void sum1rem0d$saveMigrationMarker(ValueOutput output, CallbackInfo ci) {
        if (sum1rem0d$isCopper()) output.putBoolean("sum1rem0d_copper_chest_72", sum1rem0d$copperExpanded || items.size() >= 72);
    }

    @Unique private boolean sum1rem0d$isCopper() { return ((ChestBlockEntity) (Object) this).getBlockState().getBlock() instanceof CopperChestBlock; }
    @Unique private void sum1rem0d$expand() {
        NonNullList<ItemStack> enlarged = NonNullList.withSize(72, ItemStack.EMPTY);
        for (int slot = 0; slot < Math.min(items.size(), enlarged.size()); slot++) enlarged.set(slot, items.get(slot));
        items = enlarged;
        sum1rem0d$copperExpanded = true;
    }
}
