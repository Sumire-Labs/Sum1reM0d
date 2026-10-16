package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.MultiplayerVerification;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryChangeTrigger.class)
public abstract class VerificationInventoryCriteriaMixin {
    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$temporaryItems(ServerPlayer player, Inventory inventory, ItemStack stack, CallbackInfo ci) { if (com.sumirelabs.sum1rem0d.game.RemainingLiveVerification.temporary(player) || MultiplayerVerification.temporaryInventory(player) || com.sumirelabs.sum1rem0d.game.EconomyMultiplayerVerification.temporaryInventory(player) || com.sumirelabs.sum1rem0d.game.CoinServicesMultiplayerVerification.temporaryInventory(player)) ci.cancel(); }
}
