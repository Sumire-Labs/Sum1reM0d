package com.sumirelabs.sum1rem0d;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import com.sumirelabs.sum1rem0d.network.PlatformNetwork;
import com.sumirelabs.sum1rem0d.game.ModContent;

public final class ModEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Sum1reM0d.initialize(FabricLoader.getInstance().getConfigDir());
        ModContent.registerFabric();
        PlatformNetwork.initialize();
    }
}
//?} elif neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.IEventBus;
import com.sumirelabs.sum1rem0d.network.PlatformNetwork;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Sum1reM0d.MOD_ID)
public final class ModEntrypoint {
    public ModEntrypoint(IEventBus bus) {
        Sum1reM0d.initialize(FMLPaths.CONFIGDIR.get());
        bus.addListener(PlatformNetwork::register);
        bus.addListener(ModEntrypoint::registerContent);
    }
    private static void registerContent(RegisterEvent event) {
        event.register(Registries.MOB_EFFECT, helper -> {
            for (String name : ModContent.EFFECTS) helper.register(ModContent.id(name), ModContent.createEffect(name));
        });
        event.register(Registries.ITEM, helper -> helper.register(ModContent.id("soap"), ModContent.createSoap()));
        event.register(Registries.BLOCK, helper -> helper.register(ModContent.id("upside_down_grass"), ModContent.createGrass()));
        event.register(Registries.BLOCK, helper -> {
            for (String name : java.util.List.of("furnace", "blast_furnace", "smoker")) helper.register(ModContent.id("visual_" + name), ModContent.createVisualFurnace(name));
        });
        event.register(Registries.RECIPE_SERIALIZER, helper -> helper.register(ModContent.id("configured_crafting"), com.sumirelabs.sum1rem0d.game.ConfiguredCraftingRecipe.SERIALIZER));
        event.register(Registries.MENU, helper -> helper.register(ModContent.id("fletching"), com.sumirelabs.sum1rem0d.game.FletchingMenu.TYPE));
    }
}
*///?}
