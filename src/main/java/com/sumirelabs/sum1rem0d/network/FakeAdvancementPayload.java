package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Display metadata only: no advancement progress or reward is transmitted. */
public record FakeAdvancementPayload(Identifier id, DisplayInfo display) implements CustomPacketPayload {
    public static final Type<FakeAdvancementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, "fake_advancement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FakeAdvancementPayload> CODEC = StreamCodec.ofMember(
        (payload, buffer) -> { buffer.writeIdentifier(payload.id()); DisplayInfo.STREAM_CODEC.encode(buffer, payload.display()); },
        buffer -> new FakeAdvancementPayload(buffer.readIdentifier(), DisplayInfo.STREAM_CODEC.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
