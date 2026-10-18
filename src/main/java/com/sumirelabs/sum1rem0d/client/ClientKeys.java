package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.ModContent;
import java.util.function.Consumer;
import net.minecraft.client.*;

/** Vanilla Controls bindings; default unbound so existing controls do not conflict. */
public final class ClientKeys {
  private static KeyMapping wiki, config, wallet, shop, crawl;

  private ClientKeys() {}

  public static void register(Consumer<KeyMapping> registrar) {
    var category = KeyMapping.Category.register(ModContent.id("controls"));
    wiki = new KeyMapping("key.sum1rem0d.wiki", InputConstants.UNKNOWN.getValue(), category);
    config = new KeyMapping("key.sum1rem0d.config", InputConstants.UNKNOWN.getValue(), category);
    wallet = new KeyMapping("key.sum1rem0d.wallet", InputConstants.UNKNOWN.getValue(), category);
    registrar.accept(wiki);
    registrar.accept(config);
    registrar.accept(wallet);
    shop=new KeyMapping("key.sum1rem0d.shop",InputConstants.UNKNOWN.getValue(),category);registrar.accept(shop);
    crawl = new KeyMapping("key.sum1rem0d.crawl", InputConstants.UNKNOWN.getValue(), category); registrar.accept(crawl);
  }

  //? if neoforge {
  /*public static void registerNeo(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) { register(event::register); }*/
  //?}
  static void tick(Minecraft mc) {
    if (wiki == null) return;
    while (wiki.consumeClick())
      if (available(mc) && WikiRuntime.enabled()) mc.gui.setScreen(new WikiScreen(null));
    while (config.consumeClick()) if (available(mc)) mc.gui.setScreen(new ConfigScreen(null));
    while(shop.consumeClick())if(available(mc))mc.gui.setScreen(new ShopScreen(null));
    while (crawl.consumeClick()) if (available(mc) && ClientRules.received() && ClientRules.snapshot().getAsJsonObject("features").has("crawl") && ClientRules.enabled("crawl")) {
      PlatformClientNetwork.crawl(new com.sumirelabs.sum1rem0d.network.CrawlPayload(!CrawlClientState.crawling()));
    }
    while (wallet.consumeClick())
      if (available(mc)
          && ClientRules.enabled("economy")
          && ClientGameState.snapshot().has("economy")) mc.gui.setScreen(new WalletScreen(null));
  }

  static void resetCrawl() { CrawlClientState.clear(); }

  private static boolean available(Minecraft mc) {
    return mc.player != null && mc.gui.screen() == null && mc.player.isAlive();
  }
}
