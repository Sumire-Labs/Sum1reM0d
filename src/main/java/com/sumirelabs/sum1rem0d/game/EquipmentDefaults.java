package com.sumirelabs.sum1rem0d.game;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.sumirelabs.sum1rem0d.network.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerPlayer;
/** Sends registered values, including settings still waiting for a restart, without trusting the client config. */
public final class EquipmentDefaults {
 private EquipmentDefaults(){}
 public static java.util.List<String> itemNames(){return java.util.stream.Stream.concat(ExtraEquipment.ITEMS.stream(),PaxelContent.ITEMS.stream()).toList();}
 public static net.minecraft.world.item.Item item(String name){return PaxelContent.ITEMS.contains(name)?PaxelContent.item(name.substring(0,name.length()-"_paxel".length())):ExtraContent.item(name);}
 public static void send(ServerPlayer p){if(p.connection==null || !p.connection.isAcceptingMessages())return;PaxelContent.refreshDefaults();var root=new JsonObject();var ops=p.registryAccess().createSerializationContext(JsonOps.INSTANCE);for(String name:itemNames())root.add(name,DataComponentMap.CODEC.encodeStart(ops,item(name).components()).getOrThrow());PlatformNetwork.send(p,new EquipmentStatePayload(root.toString()));}
}
