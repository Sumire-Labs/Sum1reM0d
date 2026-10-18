package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CombatItemRules;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Describes the existing shelf combat rule without adding its damage a second time as an attribute. */
@Mixin(Item.class)
public abstract class ShelfWeaponTooltipMixin {
    @Inject(method="appendHoverText",at=@At("TAIL"))
    private void sum1rem0d$shelfStats(ItemStack stack,Item.TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag,CallbackInfo ci) {
        if(!Rules.on("shelf_weapon")||!CombatItemRules.isShelf(stack))return;
        out.accept(Component.translatable("tooltip.sum1rem0d.shelf.damage",number(Rules.n("shelf_weapon","damage"))).withStyle(ChatFormatting.BLUE));
        if(Rules.b("shelf_weapon","guard_enabled"))out.accept(Component.translatable("tooltip.sum1rem0d.shelf.guard",number(Rules.n("shelf_weapon","guard_damage_reduction")*100)).withStyle(ChatFormatting.BLUE));
    }
    @org.spongepowered.asm.mixin.Unique
    private static String number(double value){return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();}
}
