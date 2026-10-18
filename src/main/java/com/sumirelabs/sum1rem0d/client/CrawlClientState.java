package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.network.CrawlStatePayload;

/** Connection-local acknowledged crawl state, reset at login/logout. */
public final class CrawlClientState {
    private static boolean crawling;
    private CrawlClientState() {}
    public static boolean crawling() { return crawling; }
    public static void accept(CrawlStatePayload state) { crawling = state.crawling(); }
    public static void clear() { crawling = false; }
}
