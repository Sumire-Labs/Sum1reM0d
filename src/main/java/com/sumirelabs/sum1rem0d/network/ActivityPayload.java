package com.sumirelabs.sum1rem0d.network;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record ActivityPayload() implements CustomPacketPayload {
    public static final Type<ActivityPayload> TYPE = new Type<>(ModContent.id("activity"));
    public static final StreamCodec<FriendlyByteBuf, ActivityPayload> CODEC = StreamCodec.unit(new ActivityPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
