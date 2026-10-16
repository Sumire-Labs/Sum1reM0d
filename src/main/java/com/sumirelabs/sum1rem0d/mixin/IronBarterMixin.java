package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PiglinAi.class)
public abstract class IronBarterMixin {
 @Inject(method="isLovedItem",at=@At("HEAD"),cancellable=true)
 private static void sum1rem0d$loveIron(ItemStack stack,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) { if(Rules.on("iron_barter") && stack.is(Items.IRON_INGOT)) cir.setReturnValue(true); }
 @Redirect(method="isBarterCurrency", at=@At(value="FIELD", target="Lnet/minecraft/world/entity/monster/piglin/PiglinAi;BARTERING_ITEM:Lnet/minecraft/world/item/Item;"))
 private static Item sum1rem0d$currency() { return Rules.on("iron_barter") ? Items.IRON_INGOT : Items.GOLD_INGOT; }
 // NeoForge replaces some vanilla checks with its currency extension.
 //? if neoforge {
 /*@Redirect(method={"wantsToPickup", "canAdmire", "stopHoldingOffHandItem"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"), require=0)
 private static boolean sum1rem0d$neoCurrency(ItemStack stack) { return Rules.on("iron_barter") ? stack.is(Items.IRON_INGOT) : stack.isPiglinCurrency(); }
 *///?}
}
