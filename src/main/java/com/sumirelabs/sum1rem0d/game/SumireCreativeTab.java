package com.sumirelabs.sum1rem0d.game;

import java.util.Comparator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

/** An additional catalog; vanilla tab contributions are retained. */
public final class SumireCreativeTab {
    private SumireCreativeTab() {}
    public static CreativeModeTab create() {
        //? if fabric {
        var builder = net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder();
        //?} elif neoforge {
        /*var builder = CreativeModeTab.builder();
        *///?}
        return builder.title(Component.translatable("itemGroup.sum1rem0d"))
            .icon(() -> ModContent.coin().getDefaultInstance())
            .displayItems((parameters, output) -> {
                if (!Rules.on("creative_tab")) return;
                BuiltInRegistries.ITEM.stream()
                    .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("sum1rem0d"))
                    .filter(item -> !BuiltInRegistries.ITEM.getKey(item).equals(ModContent.id("automated_crafting_table")))
                    .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).getPath()))
                    .forEach(item -> output.accept(item));
            }).build();
    }
    public static void registerFabric() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModContent.id("main"), create());
    }
}
