package com.sumirelabs.sum1rem0d.network;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record ShopRequestPayload(String json)implements CustomPacketPayload{
 public static final Type<ShopRequestPayload> TYPE=new Type<>(ModContent.id("shop_request"));
 public static final StreamCodec<FriendlyByteBuf,ShopRequestPayload> CODEC=StreamCodec.ofMember((p,b)->b.writeByteArray(JsonCompression.encode(p.json)),b->new ShopRequestPayload(JsonCompression.decode(b.readByteArray(JsonCompression.MAX_WIRE_BYTES))));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
