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
        ClientPlayNetworking.registerGlobalReceiver(ConfigStatePayload.TYPE,
            (state, context) -> ClientRuntime.acceptRules(state));
        ClientPlayNetworking.registerGlobalReceiver(GameStatePayload.TYPE, (state, context) -> ClientRuntime.accept(state));
        ClientPlayNetworking.registerGlobalReceiver(FakeAdvancementPayload.TYPE, (state, context) -> FakeAdvancementToast.receive(state));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientRules.clear();
            ClientGameState.clear(); TradeInvitationToast.clear();
            PlatformClientNetwork.refresh();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { ClientRules.clear(); ClientGameState.clear(); TradeInvitationToast.clear(); });
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
        NeoForge.EVENT_BUS.addListener(ClientEntrypoint::login);
        NeoForge.EVENT_BUS.addListener(ClientEntrypoint::logout);
    }
    private static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(ConfigStatePayload.TYPE, (state, context) -> ClientRuntime.acceptRules(state));
        event.register(GameStatePayload.TYPE, (state, context) -> ClientRuntime.accept(state));
        event.register(FakeAdvancementPayload.TYPE, (state, context) -> FakeAdvancementToast.receive(state));
    }
    private static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) { event.register(com.sumirelabs.sum1rem0d.game.FletchingMenu.TYPE, FletchingScreen::new); event.register(com.sumirelabs.sum1rem0d.game.TradeMenu.TYPE, TradeScreen::new); }
    private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientRules.clear();
        ClientGameState.clear(); TradeInvitationToast.clear();
        PlatformClientNetwork.refresh();
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientRules.clear(); ClientGameState.clear(); TradeInvitationToast.clear(); }
}
*///?}
