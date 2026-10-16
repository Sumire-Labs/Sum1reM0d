package com.sumirelabs.sum1rem0d.network;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record ShopStatePayload(String json)implements CustomPacketPayload{
 public static final Type<ShopStatePayload> TYPE=new Type<>(ModContent.id("shop_state"));
 public static final StreamCodec<FriendlyByteBuf,ShopStatePayload> CODEC=StreamCodec.ofMember((p,b)->b.writeByteArray(JsonCompression.encode(p.json)),b->new ShopStatePayload(JsonCompression.decode(b.readByteArray(JsonCompression.MAX_WIRE_BYTES))));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
