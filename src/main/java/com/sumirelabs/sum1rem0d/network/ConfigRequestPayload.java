package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigRequestPayload(boolean update, long revision, String json, long requestId) implements CustomPacketPayload {
    public static final int MAX_LENGTH = ConfigSchema.MAX_LENGTH;
    public static final Type<ConfigRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, "config_request"));
    public static final StreamCodec<FriendlyByteBuf, ConfigRequestPayload> CODEC = StreamCodec.ofMember(
        ConfigRequestPayload::write, ConfigRequestPayload::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(update);
        buffer.writeLong(revision);
        buffer.writeLong(requestId);
        buffer.writeByteArray(JsonCompression.encode(json));
    }
    private static ConfigRequestPayload read(FriendlyByteBuf buffer) {
        boolean update = buffer.readBoolean(); long revision = buffer.readLong(); long requestId = buffer.readLong();
        return new ConfigRequestPayload(update, revision, JsonCompression.decode(buffer.readByteArray(JsonCompression.MAX_WIRE_BYTES)), requestId);
    }
    public static ConfigRequestPayload refresh() { return new ConfigRequestPayload(false, -1, "", 0); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
