package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class)
public abstract class ExtendedBaneMixin {
 @Inject(method="modifyDamage", at=@At("RETURN"), cancellable=true)
 private static void sum1rem0d$bane(ServerLevel level, ItemStack stack, Entity victim, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
  if(!Rules.on("extended_bane") || !(victim instanceof Player && Rules.b("extended_bane","players") || (victim instanceof EnderDragon || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart) && Rules.b("extended_bane","dragon"))) return;
  int rank=EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BANE_OF_ARTHROPODS),stack);
  cir.setReturnValue(cir.getReturnValueF()+(float)(rank*Rules.n("extended_bane","damage_per_level")));
 }
}
