package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EconomyRequestPayload(String action, String target, long amount, long revision) implements CustomPacketPayload {
    public static final Type<EconomyRequestPayload> TYPE = new Type<>(ModContent.id("economy_request"));
    public static final StreamCodec<FriendlyByteBuf, EconomyRequestPayload> CODEC = StreamCodec.ofMember(EconomyRequestPayload::write, EconomyRequestPayload::read);
    private void write(FriendlyByteBuf b) { b.writeUtf(action, 24); b.writeUtf(target, 64); b.writeLong(amount); b.writeLong(revision); }
    private static EconomyRequestPayload read(FriendlyByteBuf b) { return new EconomyRequestPayload(b.readUtf(24), b.readUtf(64), b.readLong(), b.readLong()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
