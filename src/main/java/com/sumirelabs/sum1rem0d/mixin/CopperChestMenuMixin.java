package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class CopperChestMenuMixin {
    @Inject(method = "getMenuProvider", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$largeMenu(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
        if (!(state.getBlock() instanceof CopperChestBlock)) return;
        var original = cir.getReturnValue();
        if (original == null) return;
        var chest = (ChestBlock) (Object) this;
        var container = ChestBlock.getContainer(chest, state, level, pos, false);
        if (container == null || container.getContainerSize() <= 27) return;
        cir.setReturnValue(new MenuProvider() {
            @Override public Component getDisplayName() { return original.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                var combined = ChestBlock.getContainer(chest, state, level, pos, false);
                if (combined == null) return null;
                int slots = combined.getContainerSize();
                if (slots == 72) return new ChestMenu(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_8, id, inventory, combined, 8);
                if (slots == 144) return new ChestMenu(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_16, id, inventory, combined, 16);
                return null;
            }
        });
    }
}
