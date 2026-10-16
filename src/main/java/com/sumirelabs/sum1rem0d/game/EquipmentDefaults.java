package com.sumirelabs.sum1rem0d.game;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.sumirelabs.sum1rem0d.network.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerPlayer;
/** Sends registered values, including settings still waiting for a restart, without trusting the client config. */
public final class EquipmentDefaults {
 private EquipmentDefaults(){}
 public static void send(ServerPlayer p){if(p.connection==null || !p.connection.isAcceptingMessages())return;var root=new JsonObject();var ops=p.registryAccess().createSerializationContext(JsonOps.INSTANCE);for(String name:ExtraEquipment.ITEMS)root.add(name,DataComponentMap.CODEC.encodeStart(ops,ExtraContent.item(name).components()).getOrThrow());PlatformNetwork.send(p,new EquipmentStatePayload(root.toString()));}
}
