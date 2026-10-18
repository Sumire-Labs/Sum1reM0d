package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import com.sumirelabs.sum1rem0d.game.ShulkerBoxColorAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Send custom RGB without exposing or repeatedly transmitting container contents. */
@Mixin(BlockEntity.class)
public abstract class ShulkerUpdateTagMixin {
    @Inject(method = "getUpdateTag", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$shulkerUpdateTag(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            CompoundTag tag = new CompoundTag();
            int rgb = ((ShulkerBoxColorAccess) this).sum1rem0d$mixedRgb();
            if (rgb >= 0) tag.putInt("sum1rem0d_mixed_rgb", rgb);
            cir.setReturnValue(tag);
        }
    }

    @Inject(method = "getUpdatePacket", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$shulkerUpdatePacket(CallbackInfoReturnable<Packet<ClientGamePacketListener>> cir) {
        if ((Object) this instanceof ShulkerBoxBlockEntity blockEntity)
            cir.setReturnValue(ClientboundBlockEntityDataPacket.create(blockEntity));
    }
}
