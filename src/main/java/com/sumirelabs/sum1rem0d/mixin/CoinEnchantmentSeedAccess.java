package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface CoinEnchantmentSeedAccess {
  @Accessor("enchantmentSeed")
  void sum1rem0d$enchantmentSeed(int seed);
}
