package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/** Resolves the vanilla wool shape item used by the mixed-wool recipe in 26.3+. */
public final class VanillaWoolShapes {
    private VanillaWoolShapes() {}

    private static String path(DyeColor color, boolean stairs) {
        return color.getName() + "_wool_" + (stairs ? "stairs" : "slab");
    }

    public static Item item(DyeColor color, boolean stairs) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path(color, stairs)));
    }
}
