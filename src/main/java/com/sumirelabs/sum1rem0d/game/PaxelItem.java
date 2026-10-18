package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
//? if >=26.3 {
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.BlockTransformers;
//?} else {
import net.minecraft.world.item.Items;
//?}
//? if neoforge {
//? if <26.3 {
import net.minecraft.world.item.ItemInstance;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
//?}
//?}

/** Adds vanilla axe stripping/wax removal and shovel path-making to a material paxel. */
public final class PaxelItem extends Item {
    public PaxelItem(Properties properties) { super(properties); }

    //? if neoforge {
    //? if <26.3 {
    @Override public boolean canPerformAction(ItemInstance item, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }
    //?}
    //?}

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!Rules.on("material_paxel")) return super.useOn(context);
        String path = BuiltInRegistries.ITEM.getKey(context.getItemInHand().getItem()).getPath();
        if (!PaxelContent.ITEMS.contains(path)) return super.useOn(context);
        InteractionResult axe;
        //? if >=26.3 {
        var registry = context.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER);
        axe = registry.getOrThrow(BlockTransformers.AXE).value().transformBlock(context);
        //?} else {
        axe = Items.WOODEN_AXE.useOn(context);
        //?}
        if (axe.consumesAction()) return axe;
        InteractionResult shovel;
        //? if >=26.3 {
        shovel = registry.getOrThrow(BlockTransformers.SHOVEL).value().transformBlock(context);
        //?} else {
        shovel = Items.WOODEN_SHOVEL.useOn(context);
        //?}
        if (shovel.consumesAction()) return shovel;
        return super.useOn(context);
    }
}
