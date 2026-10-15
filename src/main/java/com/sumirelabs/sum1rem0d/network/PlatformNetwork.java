package com.sumirelabs.sum1rem0d.network;

import net.minecraft.server.level.ServerPlayer;

//? if fabric {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PlatformNetwork {
    private PlatformNetwork() {}
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ConfigRequestPayload.TYPE, ConfigRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActivityPayload.TYPE, ActivityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyRequestPayload.TYPE, EconomyRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EconomyRequestPayload.TYPE,(request,context)->com.sumirelabs.sum1rem0d.game.Economy.handle(request,context.player()));
        PayloadTypeRegistry.clientboundPlay().register(ConfigStatePayload.TYPE, ConfigStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GameStatePayload.TYPE, GameStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FakeAdvancementPayload.TYPE, FakeAdvancementPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ConfigRequestPayload.TYPE,
            (request, context) -> ServerConfigHandler.handle(request, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ActivityPayload.TYPE, (request, context) -> context.player().resetLastActionTime());
    }
    public static void send(ServerPlayer player, ConfigStatePayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (ServerPlayNetworking.canSend(player, ConfigStatePayload.TYPE)) ServerPlayNetworking.send(player, state);
    }
    public static void send(ServerPlayer player, GameStatePayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (ServerPlayNetworking.canSend(player, GameStatePayload.TYPE)) ServerPlayNetworking.send(player, state);
    }
    public static void send(ServerPlayer player, FakeAdvancementPayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (ServerPlayNetworking.canSend(player, FakeAdvancementPayload.TYPE)) ServerPlayNetworking.send(player, state);
    }
}
//?} elif neoforge {
/*import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlatformNetwork {
    private PlatformNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(ConfigRequestPayload.TYPE, ConfigRequestPayload.CODEC,
            (request, context) -> ServerConfigHandler.handle(request, (ServerPlayer) context.player()));
        registrar.playToServer(EconomyRequestPayload.TYPE, EconomyRequestPayload.CODEC,(request,context)->com.sumirelabs.sum1rem0d.game.Economy.handle(request,(ServerPlayer)context.player()));
        registrar.playToServer(ActivityPayload.TYPE, ActivityPayload.CODEC, (request, context) -> ((ServerPlayer) context.player()).resetLastActionTime());
        registrar.playToClient(ConfigStatePayload.TYPE, ConfigStatePayload.CODEC);
        registrar.playToClient(GameStatePayload.TYPE, GameStatePayload.CODEC);
        registrar.playToClient(FakeAdvancementPayload.TYPE, FakeAdvancementPayload.CODEC);
    }
    public static void send(ServerPlayer player, ConfigStatePayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (player.connection.hasChannel(ConfigStatePayload.TYPE)) PacketDistributor.sendToPlayer(player, state);
    }
    public static void send(ServerPlayer player, GameStatePayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (player.connection.hasChannel(GameStatePayload.TYPE)) PacketDistributor.sendToPlayer(player, state);
    }
    public static void send(ServerPlayer player, FakeAdvancementPayload state) {
        if (player.connection == null || !player.connection.isAcceptingMessages()) return;
        if (player.connection.hasChannel(FakeAdvancementPayload.TYPE)) PacketDistributor.sendToPlayer(player, state);
    }
}
*///?}
