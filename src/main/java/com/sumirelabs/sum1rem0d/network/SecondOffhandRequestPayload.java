package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Requests swapping the player's server-authoritative inventory cursor with the virtual extra slot. */
public record SecondOffhandRequestPayload() implements CustomPacketPayload {
    public static final Type<SecondOffhandRequestPayload> TYPE = new Type<>(ModContent.id("second_offhand_request"));
    public static final StreamCodec<FriendlyByteBuf, SecondOffhandRequestPayload> CODEC = StreamCodec.unit(new SecondOffhandRequestPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
