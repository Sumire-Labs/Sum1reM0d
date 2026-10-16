package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.network.ConfigRequestPayload;
import com.sumirelabs.sum1rem0d.network.ActivityPayload;

//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//?} elif neoforge {
/*import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

public final class PlatformClientNetwork {
    private PlatformClientNetwork() {}
    public static boolean send(ConfigRequestPayload request) {
        //? if fabric {
        if (!ClientPlayNetworking.canSend(ConfigRequestPayload.TYPE)) return false;
        ClientPlayNetworking.send(request);
        //?} elif neoforge {
        /*var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(ConfigRequestPayload.TYPE)) return false;
        ClientPacketDistributor.sendToServer(request);
        *///?}
        return true;
    }
    public static void economy(com.sumirelabs.sum1rem0d.network.EconomyRequestPayload request) {
        //? if fabric {
        if (ClientPlayNetworking.canSend(com.sumirelabs.sum1rem0d.network.EconomyRequestPayload.TYPE)) ClientPlayNetworking.send(request);
        //?} elif neoforge {
        /*var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(com.sumirelabs.sum1rem0d.network.EconomyRequestPayload.TYPE)) ClientPacketDistributor.sendToServer(request);
        *///?}
    }
    public static void refresh() { send(ConfigRequestPayload.refresh()); }
    public static void activity() {
        //? if fabric {
        if (ClientPlayNetworking.canSend(ActivityPayload.TYPE)) ClientPlayNetworking.send(new ActivityPayload());
        //?} elif neoforge {
        /*var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(ActivityPayload.TYPE)) ClientPacketDistributor.sendToServer(new ActivityPayload());
        *///?}
    }
    public static void shop(com.sumirelabs.sum1rem0d.network.ShopRequestPayload request) {
        //? if fabric {
        if(net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(com.sumirelabs.sum1rem0d.network.ShopRequestPayload.TYPE))net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(request);
        //?} elif neoforge {
        /*var listener=net.minecraft.client.Minecraft.getInstance().getConnection();if(listener!=null && listener.hasChannel(com.sumirelabs.sum1rem0d.network.ShopRequestPayload.TYPE))net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(request);
        *///?}
    }

}
