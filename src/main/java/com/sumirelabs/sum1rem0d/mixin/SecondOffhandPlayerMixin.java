package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.SecondOffhandAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class SecondOffhandPlayerMixin implements SecondOffhandAccess {
    @Unique private ItemStack sum1rem0d$secondOffhand = ItemStack.EMPTY;

    @Override public ItemStack sum1rem0d$secondOffhand() { return sum1rem0d$secondOffhand; }
    @Override public void sum1rem0d$secondOffhand(ItemStack stack) {
        sum1rem0d$secondOffhand = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$saveSecondOffhand(ValueOutput output, CallbackInfo ci) {
        if (!sum1rem0d$secondOffhand.isEmpty()) output.store("sum1rem0d_second_offhand", ItemStack.CODEC, sum1rem0d$secondOffhand);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$loadSecondOffhand(ValueInput input, CallbackInfo ci) {
        sum1rem0d$secondOffhand = input.read("sum1rem0d_second_offhand", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void sum1rem0d$restoreAfterDeath(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        com.sumirelabs.sum1rem0d.game.SecondOffhandService.restore((ServerPlayer) (Object) this, oldPlayer);
    }
}
