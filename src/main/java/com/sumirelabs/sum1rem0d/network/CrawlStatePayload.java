package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server acknowledgement of the pose request after collision and game-state checks. */
public record CrawlStatePayload(boolean crawling) implements CustomPacketPayload {
    public static final Type<CrawlStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, "crawl_state"));
    public static final StreamCodec<FriendlyByteBuf, CrawlStatePayload> CODEC = StreamCodec.ofMember(CrawlStatePayload::write, CrawlStatePayload::read);
    private void write(FriendlyByteBuf buffer) { buffer.writeBoolean(crawling); }
    private static CrawlStatePayload read(FriendlyByteBuf buffer) { return new CrawlStatePayload(buffer.readBoolean()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
