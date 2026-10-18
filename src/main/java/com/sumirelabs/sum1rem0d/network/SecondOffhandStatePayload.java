package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.game.ModContent;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Server snapshot for one player's virtual second-offhand item. */
public record SecondOffhandStatePayload(UUID player, ItemStack stack) implements CustomPacketPayload {
    public static final Type<SecondOffhandStatePayload> TYPE = new Type<>(ModContent.id("second_offhand_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SecondOffhandStatePayload> CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUUID(payload.player()); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack()); },
            buffer -> new SecondOffhandStatePayload(buffer.readUUID(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
