package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigStatePayload(long revision, boolean editable, String json, String message, long requestId, boolean applied) implements CustomPacketPayload {
    public static final Type<ConfigStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, "config_state"));
    public static final StreamCodec<FriendlyByteBuf, ConfigStatePayload> CODEC = StreamCodec.ofMember(
        ConfigStatePayload::write, ConfigStatePayload::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeLong(revision);
        buffer.writeBoolean(editable);
        buffer.writeLong(requestId);
        buffer.writeBoolean(applied);
        buffer.writeByteArray(JsonCompression.encode(json));
        buffer.writeUtf(message, 256);
    }
    private static ConfigStatePayload read(FriendlyByteBuf buffer) {
        long revision = buffer.readLong(); boolean editable = buffer.readBoolean(); long requestId = buffer.readLong(); boolean applied = buffer.readBoolean();
        return new ConfigStatePayload(revision, editable, JsonCompression.decode(buffer.readByteArray(JsonCompression.MAX_WIRE_BYTES)), buffer.readUtf(256), requestId, applied);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
