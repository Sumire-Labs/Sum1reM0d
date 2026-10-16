package com.sumirelabs.sum1rem0d.network;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record EquipmentStatePayload(String json) implements CustomPacketPayload {
 public static final Type<EquipmentStatePayload> TYPE=new Type<>(ModContent.id("equipment_defaults"));
 public static final StreamCodec<FriendlyByteBuf,EquipmentStatePayload> CODEC=StreamCodec.ofMember((p,b)->b.writeByteArray(JsonCompression.encode(p.json)),b->new EquipmentStatePayload(JsonCompression.decode(b.readByteArray(JsonCompression.MAX_WIRE_BYTES))));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
