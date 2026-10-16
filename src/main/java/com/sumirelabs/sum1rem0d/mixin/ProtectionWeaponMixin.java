package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.SwordGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Enchantment.class)
public abstract class ProtectionWeaponMixin {
 @Inject(method={"canEnchant","isSupportedItem","isPrimaryItem"},at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$protection(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {if(((Enchantment)(Object)this).description().equals(Component.translatable("enchantment.minecraft.protection")) && SwordGuard.weapon(stack)) cir.setReturnValue(true);}
}
