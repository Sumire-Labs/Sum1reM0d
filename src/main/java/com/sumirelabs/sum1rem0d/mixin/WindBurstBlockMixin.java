package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WindBurstBlockRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.ExplodeEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends the native Wind Burst explosion after its normal knockback and particles. */
@Mixin(ExplodeEffect.class)
public abstract class WindBurstBlockMixin {
    @Inject(method="apply",at=@At("TAIL"))
    private void sum1rem0d$throwBlocks(ServerLevel level,int effectLevel,EnchantedItemInUse enchantedItem,Entity affected,Vec3 position,CallbackInfo callback){
        if(enchantedItem.owner()!=affected||enchantedItem.itemStack().isEmpty()||!(affected instanceof net.minecraft.world.entity.player.Player player))return;
        var enchantment=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST);
        int rank=EnchantmentHelper.getItemEnchantmentLevel(enchantment,enchantedItem.itemStack());
        if(rank>0)WindBurstBlockRules.blow(level,rank,player,position,enchantedItem.itemStack());
    }
}
