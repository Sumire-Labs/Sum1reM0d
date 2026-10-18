package com.sumirelabs.sum1rem0d.network;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-validated crawl pose request. The client sends desired state, never arbitrary pose data. */
public record CrawlPayload(boolean crawling) implements CustomPacketPayload {
    public static final Type<CrawlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, "crawl"));
    public static final StreamCodec<FriendlyByteBuf, CrawlPayload> CODEC = StreamCodec.ofMember(CrawlPayload::write, CrawlPayload::read);
    private void write(FriendlyByteBuf buffer) { buffer.writeBoolean(crawling); }
    private static CrawlPayload read(FriendlyByteBuf buffer) { return new CrawlPayload(buffer.readBoolean()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
