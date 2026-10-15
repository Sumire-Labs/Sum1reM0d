package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GameStatePayload(String json) implements CustomPacketPayload {
    public static final Type<GameStatePayload> TYPE = new Type<>(ModContent.id("game_state"));
    public static final StreamCodec<FriendlyByteBuf, GameStatePayload> CODEC = StreamCodec.ofMember(
        (state, buffer) -> buffer.writeByteArray(JsonCompression.encode(state.json())),
        buffer -> new GameStatePayload(JsonCompression.decode(buffer.readByteArray(JsonCompression.MAX_WIRE_BYTES))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
