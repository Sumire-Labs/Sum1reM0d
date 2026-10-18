package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import com.sumirelabs.sum1rem0d.network.GameStatePayload;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import com.sumirelabs.sum1rem0d.network.FakeAdvancementPayload;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        com.sumirelabs.sum1rem0d.config.PersonalConfig.initialize(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        ClientKeys.register(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper::registerKeyMapping);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.FletchingMenu.TYPE, FletchingScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.TradeMenu.TYPE, TradeScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.MENU, DeliveryBoxScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.AutomationContent.MENU, AutomationScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_8, CopperChestScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_16, CopperChestScreen::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.sumirelabs.sum1rem0d.game.MobilityContent.type(), SeatEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(ConfigStatePayload.TYPE,
            (state, context) -> ClientRuntime.acceptRules(state));
        ClientPlayNetworking.registerGlobalReceiver(GameStatePayload.TYPE, (state, context) -> ClientRuntime.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(com.sumirelabs.sum1rem0d.network.ShopStatePayload.TYPE,(state,context)->ShopClient.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(com.sumirelabs.sum1rem0d.network.EquipmentStatePayload.TYPE,(state,context)->EquipmentDefaultsClient.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(FakeAdvancementPayload.TYPE, (state, context) -> FakeAdvancementToast.receive(state));
        ClientPlayNetworking.registerGlobalReceiver(com.sumirelabs.sum1rem0d.network.CrawlStatePayload.TYPE, (state, context) -> CrawlClientState.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(com.sumirelabs.sum1rem0d.network.SecondOffhandStatePayload.TYPE, (state, context) -> SecondOffhandClient.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(com.sumirelabs.sum1rem0d.network.PaxelRecipeMapPayload.TYPE, (state, context) -> com.sumirelabs.sum1rem0d.game.PaxelRecipeMapState.accept(state.toolIds()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientRuntime.resetWorldMediaSerials();
            ClientRules.clear(); ShopClient.clear(); EquipmentDefaultsClient.clear(); ClientKeys.resetCrawl();
            ClientGameState.clear(); TradeInvitationToast.clear();
            EquipmentDefaultsClient.mismatchForVerification();
            PlatformClientNetwork.refresh();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { ClientRuntime.resetWorldMediaSerials(); ClientRules.clear(); ShopClient.clear(); EquipmentDefaultsClient.clear(); ClientGameState.clear(); TradeInvitationToast.clear(); ClientKeys.resetCrawl(); SecondOffhandClient.clear(); });
    }
}
//?} elif neoforge {
/*import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = Sum1reM0d.MOD_ID, dist = Dist.CLIENT)
public final class ClientEntrypoint {
    public ClientEntrypoint(IEventBus bus) {
        com.sumirelabs.sum1rem0d.config.PersonalConfig.initialize(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        bus.addListener(ClientKeys::registerNeo);
        bus.addListener(ClientEntrypoint::register);
        bus.addListener(ClientEntrypoint::screens);
        bus.addListener(ClientEntrypoint::renderers);
        NeoForge.EVENT_BUS.addListener(ClientEntrypoint::login);
        NeoForge.EVENT_BUS.addListener(ClientEntrypoint::logout);
    }
    private static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(ConfigStatePayload.TYPE, (state, context) -> ClientRuntime.acceptRules(state));
        event.register(GameStatePayload.TYPE, (state, context) -> ClientRuntime.accept(state));
        event.register(com.sumirelabs.sum1rem0d.network.ShopStatePayload.TYPE,(state,context)->ShopClient.accept(state));
        event.register(com.sumirelabs.sum1rem0d.network.EquipmentStatePayload.TYPE,(state,context)->EquipmentDefaultsClient.accept(state));
        event.register(FakeAdvancementPayload.TYPE, (state, context) -> FakeAdvancementToast.receive(state));
        event.register(com.sumirelabs.sum1rem0d.network.CrawlStatePayload.TYPE, (state, context) -> CrawlClientState.accept(state));
        event.register(com.sumirelabs.sum1rem0d.network.SecondOffhandStatePayload.TYPE, (state, context) -> SecondOffhandClient.accept(state));
        event.register(com.sumirelabs.sum1rem0d.network.PaxelRecipeMapPayload.TYPE, (state, context) -> com.sumirelabs.sum1rem0d.game.PaxelRecipeMapState.accept(state.toolIds()));
    }
    private static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) { event.register(com.sumirelabs.sum1rem0d.game.FletchingMenu.TYPE, FletchingScreen::new); event.register(com.sumirelabs.sum1rem0d.game.TradeMenu.TYPE, TradeScreen::new); event.register(com.sumirelabs.sum1rem0d.game.DeliveryBoxContent.MENU, DeliveryBoxScreen::new); event.register(com.sumirelabs.sum1rem0d.game.AutomationContent.MENU, AutomationScreen::new); event.register(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_8, CopperChestScreen::new); event.register(com.sumirelabs.sum1rem0d.game.MobilityContent.COPPER_CHEST_16, CopperChestScreen::new); }
    private static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(com.sumirelabs.sum1rem0d.game.MobilityContent.type(), SeatEntityRenderer::new); }
    private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientRuntime.resetWorldMediaSerials();
        ClientRules.clear(); ShopClient.clear(); EquipmentDefaultsClient.clear(); ClientKeys.resetCrawl();
        ClientGameState.clear(); TradeInvitationToast.clear();
        EquipmentDefaultsClient.mismatchForVerification();
            PlatformClientNetwork.refresh();
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientRuntime.resetWorldMediaSerials(); ClientRules.clear(); ShopClient.clear(); EquipmentDefaultsClient.clear(); ClientGameState.clear(); TradeInvitationToast.clear(); ClientKeys.resetCrawl(); SecondOffhandClient.clear(); }
}
*///?}
