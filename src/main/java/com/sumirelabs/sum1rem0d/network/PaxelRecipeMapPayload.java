package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.game.ModContent;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sends only the selected tool IDs, never the private world seed. */
public record PaxelRecipeMapPayload(List<String> toolIds) implements CustomPacketPayload {
    public static final Type<PaxelRecipeMapPayload> TYPE = new Type<>(ModContent.id("paxel_recipe_map"));
    public static final StreamCodec<FriendlyByteBuf, PaxelRecipeMapPayload> CODEC = StreamCodec.ofMember(
            (payload, buffer) -> {
                buffer.writeVarInt(payload.toolIds().size());
                for (String id : payload.toolIds()) buffer.writeUtf(id, 256);
            }, buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > 128) throw new IllegalArgumentException("Invalid paxel recipe map size: " + size);
                var ids = new java.util.ArrayList<String>(size);
                for (int i = 0; i < size; i++) ids.add(buffer.readUtf(256));
                return new PaxelRecipeMapPayload(List.copyOf(ids));
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
