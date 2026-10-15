package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ModContent;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the registered utility item discoverable on both loader backends. */
@Mixin(CreativeModeTab.class)
public abstract class SoapCreativeTabMixin {
    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;
    @Inject(method = "buildContents", at = @At("TAIL"))
    private void sum1rem0d$soap(CallbackInfo ci) {
        var key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab) (Object) this);
        if (key == null || !key.toString().equals("minecraft:tools_and_utilities")) return;
        var visible = ItemStackLinkedSet.createTypeAndComponentsSet(); visible.addAll(displayItems); visible.add(new ItemStack(ModContent.soap())); visible.add(new ItemStack(ModContent.coin())); visible.add(new ItemStack(ModContent.coinRoll())); displayItems = visible;
        var searchable = ItemStackLinkedSet.createTypeAndComponentsSet(); searchable.addAll(displayItemsSearchTab); searchable.add(new ItemStack(ModContent.soap())); searchable.add(new ItemStack(ModContent.coin())); searchable.add(new ItemStack(ModContent.coinRoll())); displayItemsSearchTab = searchable;
    }
}
