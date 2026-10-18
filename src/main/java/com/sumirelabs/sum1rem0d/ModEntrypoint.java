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
        com.sumirelabs.sum1rem0d.game.WoolShapeBackport.registerFabric();
        com.sumirelabs.sum1rem0d.game.SumireCreativeTab.registerFabric();
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
        com.sumirelabs.sum1rem0d.game.NaturalFlipData.register(bus);
        bus.addListener(PlatformNetwork::register);
        bus.addListener(com.sumirelabs.sum1rem0d.game.ExtraContent::setupNeo);
        bus.addListener(com.sumirelabs.sum1rem0d.game.WoolShapeBackport::registerNeo);
        bus.addListener(com.sumirelabs.sum1rem0d.game.WoolShapeBackport::setupNeo);
        bus.addListener(ModEntrypoint::registerContent);
    }
    private static void registerContent(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), com.sumirelabs.sum1rem0d.game.SumireCreativeTab.create()));
        event.register(Registries.BLOCK,h->{for(var name:com.sumirelabs.sum1rem0d.game.AutomationContent.BLOCKS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.AutomationContent.createBlock(name));for(var name:com.sumirelabs.sum1rem0d.game.AutomationContent.MATERIAL_BLOCKS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.AutomationContent.createBlock(name));});
        event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ModContent.id("automation_machine"),com.sumirelabs.sum1rem0d.game.AutomationContent.createType()));
        event.register(Registries.ITEM,h->{for(var name:com.sumirelabs.sum1rem0d.game.AutomationContent.BLOCKS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.AutomationContent.createBlockItem(name));for(var name:com.sumirelabs.sum1rem0d.game.AutomationContent.MATERIAL_BLOCKS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.AutomationContent.createBlockItem(name));for(var name:com.sumirelabs.sum1rem0d.game.AutomationContent.ITEMS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.AutomationContent.createItem(name));if(com.sumirelabs.sum1rem0d.game.AutomationContent.needsLegacySulfur())h.register(ModContent.id("sulfur"),com.sumirelabs.sum1rem0d.game.AutomationContent.createItem("sulfur"));});
        event.register(Registries.MENU,h->h.register(ModContent.id("automation_machine"),com.sumirelabs.sum1rem0d.game.AutomationContent.MENU));
        event.register(Registries.RECIPE_SERIALIZER,h->h.register(ModContent.id("mixed_containers"),com.sumirelabs.sum1rem0d.game.MixedContainersRecipe.SERIALIZER));
        event.register(Registries.BLOCK,h->h.register(ModContent.id("sumzon_delivery_box"),com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.createBlock()));
        event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ModContent.id("sumzon_delivery_box"),com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.createType()));
        event.register(Registries.ITEM,h->h.register(ModContent.id("sumzon_delivery_box"),com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.createItem()));
        event.register(Registries.MENU,h->h.register(ModContent.id("sumzon_delivery_box"),com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.MENU));
        event.register(Registries.ITEM,h->h.register(ModContent.id("life_berry"),com.sumirelabs.sum1rem0d.game.LifeBerryItem.create()));
        event.register(Registries.BLOCK,helper->{for(var name:com.sumirelabs.sum1rem0d.game.ExtraContent.BLOCKS)helper.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.ExtraContent.createBlock(name));});
        event.register(Registries.BLOCK_ENTITY_TYPE,helper->{for(var name:com.sumirelabs.sum1rem0d.game.ExtraContent.BLOCK_ENTITIES)helper.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.ExtraContent.createType(name));});
        event.register(Registries.ITEM,helper->{for(var name:com.sumirelabs.sum1rem0d.game.ExtraContent.ITEMS)helper.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.ExtraContent.createItem(name));});
        event.register(Registries.RECIPE_SERIALIZER,helper->helper.register(ModContent.id("extra_crafting"),com.sumirelabs.sum1rem0d.game.ExtraCraftingRecipe.SERIALIZER));
        event.register(Registries.ITEM,h->{for(String name:com.sumirelabs.sum1rem0d.game.PaxelContent.ITEMS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.PaxelContent.createItem(name));});
        event.register(Registries.RECIPE_SERIALIZER,h->h.register(ModContent.id("paxel"),com.sumirelabs.sum1rem0d.game.PaxelRecipe.SERIALIZER));
        event.register(Registries.ITEM,h->{for(String name:com.sumirelabs.sum1rem0d.game.FoodContent.ITEMS)h.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.FoodContent.createItem(name));});
        event.register(Registries.RECIPE_SERIALIZER,h->h.register(ModContent.id("food_content"),com.sumirelabs.sum1rem0d.game.FoodRecipe.SERIALIZER));
        event.register(Registries.BLOCK,helper -> helper.register(ModContent.id("mixed_concrete"),com.sumirelabs.sum1rem0d.game.MixedConcrete.createBlock()));
        event.register(Registries.BLOCK_ENTITY_TYPE,helper -> helper.register(ModContent.id("mixed_concrete"),com.sumirelabs.sum1rem0d.game.MixedConcrete.createType()));
        event.register(Registries.ITEM,helper -> helper.register(ModContent.id("mixed_concrete"),com.sumirelabs.sum1rem0d.game.MixedConcrete.createItem()));
        event.register(Registries.RECIPE_SERIALIZER,helper -> helper.register(ModContent.id("mixed_concrete"),com.sumirelabs.sum1rem0d.game.MixedConcreteRecipe.SERIALIZER));
        event.register(Registries.ENTITY_TYPE,helper -> helper.register(ModContent.id("seat"),com.sumirelabs.sum1rem0d.game.MobilityContent.createSeatType()));
        event.register(Registries.BLOCK,helper -> { for (String name : com.sumirelabs.sum1rem0d.game.MobilityContent.CAULDRON_BLOCKS) helper.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.MobilityContent.createCauldronBlock(name)); });
        event.register(Registries.ITEM,helper -> { for (String name : com.sumirelabs.sum1rem0d.game.MobilityContent.CAULDRON_BLOCKS) helper.register(ModContent.id(name),com.sumirelabs.sum1rem0d.game.MobilityContent.createCauldronItem(name)); });
        event.register(Registries.MENU,helper -> { helper.register(ModContent.id("copper_chest_8"),com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_8); helper.register(ModContent.id("copper_chest_16"),com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_16); });
        event.register(Registries.MOB_EFFECT, helper -> {
            for (String name : ModContent.EFFECTS) helper.register(ModContent.id(name), ModContent.createEffect(name));
        });
        event.register(Registries.ITEM, helper -> helper.register(ModContent.id("soap"), ModContent.createSoap()));
        event.register(Registries.ITEM, helper -> helper.register(ModContent.id("mud_ball"), ModContent.createMudBall()));
        event.register(Registries.ITEM, helper -> { for (String name : java.util.List.of("coin", "coin_roll")) helper.register(ModContent.id(name),ModContent.createCoin(name)); });
        event.register(Registries.MENU, helper -> helper.register(ModContent.id("trade"),com.sumirelabs.sum1rem0d.game.TradeMenu.TYPE));
        event.register(Registries.BLOCK, helper -> helper.register(ModContent.id("upside_down_grass"), ModContent.createGrass()));
        event.register(Registries.BLOCK, helper -> { for (String name : ModContent.VISUAL_NYLIUM) helper.register(ModContent.id("sideways_" + name), ModContent.createVisualNylium(name)); });
        event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> helper.register(com.sumirelabs.sum1rem0d.game.PathFarmer.ID, com.sumirelabs.sum1rem0d.game.PathFarmer.create()));
        event.register(Registries.BLOCK, helper -> {
            for (String name : java.util.List.of("furnace", "blast_furnace", "smoker")) helper.register(ModContent.id("visual_" + name), ModContent.createVisualFurnace(name));
        });
        event.register(Registries.RECIPE_SERIALIZER, helper -> helper.register(ModContent.id("configured_crafting"), com.sumirelabs.sum1rem0d.game.ConfiguredCraftingRecipe.SERIALIZER));
        event.register(Registries.MENU, helper -> helper.register(ModContent.id("fletching"), com.sumirelabs.sum1rem0d.game.FletchingMenu.TYPE));
    }
}
*///?}
