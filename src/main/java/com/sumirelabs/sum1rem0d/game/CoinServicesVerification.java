package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.EconomyRequestPayload;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

final class CoinServicesVerification {
  private CoinServicesVerification() {}

  private static void bank(ServerPlayer p, String action, long amount) {
    Economy.handle(new EconomyRequestPayload(action, "", amount, Sum1reM0d.config().revision()), p);
  }

  private static void funds(ServerPlayer p, long value) {
    var state = WorldState.get(p.level().getServer());
    state.player(p).walletInitialized = true;
    state.player(p).coins = value;
  }

  private static AnvilMenu anvil(ServerPlayer p) {
    p.containerMenu.setCarried(ItemStack.EMPTY);
    var menu =
        new AnvilMenu(
            12,
            p.getInventory(),
            ContainerLevelAccess.create(p.level(), new BlockPos(544, 240, 0)));
    p.containerMenu = menu;
    menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
    menu.setItemName("コインの金床");
    require(
        menu.getCost() == 1 && menu.getSlot(2).hasItem(), "Native rename costs one vanilla level");
    return menu;
  }

  private static EnchantmentMenu enchant(ServerPlayer p, int lapis) {
    p.containerMenu.setCarried(ItemStack.EMPTY);
    var menu =
        new EnchantmentMenu(
            13,
            p.getInventory(),
            ContainerLevelAccess.create(p.level(), new BlockPos(560, 240, 0)));
    p.containerMenu = menu;
    menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
    menu.getSlot(1).set(lapis == 0 ? ItemStack.EMPTY : new ItemStack(Items.LAPIS_LAZULI, lapis));
    menu.costs[2] = 30;
    return menu;
  }

  private static void failedWrite(MinecraftServer server, Runnable action) throws Exception {
    var state = WorldState.get(server);
    state.saveStrict();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("data/sum1rem0d-state.json");
    var backup = path.resolveSibling("bank-native-" + UUID.randomUUID() + ".json");
    Files.move(path, backup);
    try {
      Files.createDirectory(path);
      action.run();
    } finally {
      Files.delete(path);
      Files.move(backup, path);
    }
  }

  static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
    var config = Sum1reM0d.config();
    var original = config.snapshot();
    var state = WorldState.get(server);
    var checkpoint = state.checkpoint();
    var p = EconomyVerification.actor(server, server.overworld(), "SumireBankVerify");
    try {
      var draft = config.snapshot();
      for (var entry : draft.getAsJsonObject("features").entrySet())
        entry.getValue().getAsJsonObject().addProperty("enabled", false);
      for (String feature : List.of("economy", "xp_bank", "coin_anvil", "coin_enchanting"))
        draft.getAsJsonObject("features").getAsJsonObject(feature).addProperty("enabled", true);
      config.update(draft, config.revision());
      funds(p, 1000);
      PlayerExperience.at(1445).apply(p);
      p.totalExperience = 10;
      bank(p, "xp_deposit", 50);
      require(
          Economy.balance(p) == 1050
              && PlayerExperience.capture(p).points() == 1395
              && p.experienceLevel == 30,
          "Bank consumes actual bar points rather than lifetime counter or levels");
      bank(p, "xp_withdraw", 50);
      require(
          Economy.balance(p) == 1000 && PlayerExperience.capture(p).points() == 1445,
          "Bank round trip conserves exact points and money");
      long current = Economy.balance(p);
      Economy.handle(new EconomyRequestPayload("xp_deposit", "", 50, config.revision() - 1), p);
      require(
          Economy.balance(p) == current && PlayerExperience.capture(p).points() == 1445,
          "Stale bank pricing revision cannot exchange");
      passed.accept("XP bank exact vanilla curve, reciprocal conversion and stale quote rejection");
      String marker = ((CoinInventoryRevision) p).sum1rem0d$coinRevision();
      PlayerExperience.at(0).apply(p);
      ((CoinInventoryRevision) p).sum1rem0d$coinRevision("");
      Economy.recover(p);
      require(
          PlayerExperience.capture(p).points() == 1445 && Economy.balance(p) == 1000,
          "Bank journal recovers XP and money together");
      p.giveExperiencePoints(10);
      Economy.recover(p);
      require(
          PlayerExperience.capture(p).points() == 1455,
          "Completed XP journal does not overwrite later XP gains");
      Economy.handle(new EconomyRequestPayload("withdraw", "coin", 1, 0), p);
      PlayerExperience.at(0).apply(p);
      ((CoinInventoryRevision) p).sum1rem0d$coinRevision("");
      Economy.recover(p);
      require(
          PlayerExperience.capture(p).points() == 1455 && Economy.balance(p) == 999,
          "Later wallet transactions retain the latest XP recovery state");
      passed.accept(
          "XP journal recovery is idempotent and survives subsequent wallet transactions");
      var respawn = new PlayerVerification.TestPlayer(server, p.level(), "SumireBankVerify", true);
      respawn.restoreFrom(p, false);
      PlayerExperience.at(0).apply(respawn);
      Economy.recover(respawn);
      require(
          PlayerExperience.capture(respawn).points() == 0,
          "Death respawn never resurrects banked XP from an old journal");
      respawn.discard();

      p.containerMenu = p.inventoryMenu;
      funds(p, 1000);
      PlayerExperience.at(100).apply(p);
      failedWrite(server, () -> bank(p, "xp_deposit", 50));
      require(
          Economy.balance(p) == 1000 && PlayerExperience.capture(p).points() == 100,
          "Failed bank save changes neither XP nor coins");
      bank(p, "xp_deposit", 101);
      require(
          Economy.balance(p) == 1000 && PlayerExperience.capture(p).points() == 100,
          "Insufficient XP cannot mint coins");
      funds(p, 0);
      bank(p, "xp_withdraw", 1);
      require(
          Economy.balance(p) == 0 && PlayerExperience.capture(p).points() == 100,
          "Insufficient coins cannot mint XP");
      passed.accept("bank insufficient funds and real filesystem failures preserve both balances");
      ServerVerification.place(
          p.level(), new BlockPos(544, 240, 0), Blocks.ANVIL.defaultBlockState());
      ServerVerification.place(
          p.level(), new BlockPos(560, 240, 0), Blocks.ENCHANTING_TABLE.defaultBlockState());
      p.getInventory().clearContent();
      funds(p, 100);
      PlayerExperience.at(0).apply(p);
      p.totalExperience = 177;
      var menu = anvil(p);
      menu.clicked(2, 0, ContainerInput.PICKUP, p);
      require(
          Economy.balance(p) == 90
              && p.experienceLevel == 0
              && p.totalExperience == 177
              && menu.getCarried().is(Items.IRON_SWORD)
              && menu.getSlot(0).getItem().isEmpty(),
          "Native anvil pickup charges ten coins without consuming XP");
      menu.clicked(2, 0, ContainerInput.PICKUP, p);
      require(Economy.balance(p) == 90, "Repeated pickup cannot charge or duplicate output");
      funds(p, 9);
      menu = anvil(p);
      menu.clicked(2, 0, ContainerInput.PICKUP, p);
      require(
          Economy.balance(p) == 9 && menu.getCarried().isEmpty() && menu.getSlot(0).hasItem(),
          "Anvil refuses insufficient coins even with XP bypass");
      funds(p, 100);
      for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
      menu.clicked(2, 0, ContainerInput.QUICK_MOVE, p);
      require(
          Economy.balance(p) == 100 && menu.getSlot(0).hasItem(),
          "Full inventory shift-click does not charge");
      p.getInventory().clearContent();
      menu.clicked(2, 0, ContainerInput.QUICK_MOVE, p);
      require(
          Economy.balance(p) == 90 && p.getInventory().contains(s -> s.is(Items.IRON_SWORD)),
          "Shift-click charges exactly once when output fits");
      passed.accept(
          "native anvil pickup, repeated pickup, insufficient coins and shift-click capacity"
              + " checks");
      p.getInventory().clearContent();
      funds(p, 100);
      menu = anvil(p);
      var failedAnvil = menu;
      var anvilPos = new BlockPos(544, 240, 0);
      p.level().setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState(), 3);
      long damageSeed = 0;
      while (net.minecraft.util.RandomSource.create(damageSeed).nextFloat() >= 0.12F) damageSeed++;
      p.getRandom().setSeed(damageSeed);
      failedWrite(server, () -> failedAnvil.clicked(2, 0, ContainerInput.PICKUP, p));
      require(
          Economy.balance(p) == 100
              && failedAnvil.getCarried().isEmpty()
              && failedAnvil.getSlot(0).hasItem()
              && failedAnvil.getSlot(2).hasItem()
              && failedAnvil.getCost() == 1,
          "Failed anvil save restores money, inputs, output, cursor and cost");
      require(
          p.level().getBlockState(anvilPos).is(Blocks.DAMAGED_ANVIL),
          "Failed payment must not damage or break the world anvil");
      passed.accept("native anvil filesystem failure rolls back extraction and world damage");
      var off = config.snapshot();
      off.getAsJsonObject("features").getAsJsonObject("coin_anvil").addProperty("enabled", false);
      config.update(off, config.revision());
      funds(p, 100);
      PlayerExperience.at(0).apply(p);
      menu = anvil(p);
      menu.clicked(2, 0, ContainerInput.PICKUP, p);
      require(menu.getCarried().isEmpty(), "Disabled coin anvil requires real XP");
      p.giveExperienceLevels(5);
      menu.clicked(2, 0, ContainerInput.PICKUP, p);
      require(
          p.experienceLevel == 4
              && Economy.balance(p) == 100
              && menu.getCarried().is(Items.IRON_SWORD),
          "Disabled coin anvil restores vanilla level consumption");
      config.update(draft, config.revision());
      passed.accept("disabling coin anvil restores vanilla XP eligibility and consumption");
      PlayerExperience.at(0).apply(p);
      funds(p, 299);
      var enchanting = enchant(p, 3);
      require(
          !enchanting.clickMenuButton(p, 2)
              && Economy.balance(p) == 299
              && enchanting.getSlot(1).getItem().getCount() == 3,
          "Level-30 enchant requires 300-coin balance");
      funds(p, 300);
      require(enchanting.clickMenuButton(p, 2), "Native enchant succeeds at required coin balance");
      require(
          Economy.balance(p) == 270
              && PlayerExperience.capture(p).points() == 0
              && enchanting.getSlot(1).getItem().isEmpty()
              && !enchanting.getSlot(0).getItem().getEnchantments().isEmpty(),
          "Native enchant consumes thirty coins and three lapis but no XP");
      funds(p, 1000);
      enchanting = enchant(p, 0);
      require(
          !enchanting.clickMenuButton(p, 2) && Economy.balance(p) == 1000,
          "Coin enchanting still requires lapis");
      passed.accept(
          "native enchant required balance, actual coin cost, lapis and zero-XP behavior");
      var loose = config.snapshot();
      loose
          .getAsJsonObject("features")
          .getAsJsonObject("coin_enchanting")
          .addProperty("require_balance", false);
      config.update(loose, config.revision());
      funds(p, 30);
      enchanting = enchant(p, 3);
      require(
          enchanting.clickMenuButton(p, 2) && Economy.balance(p) == 0,
          "Optional threshold removal still charges actual third-tier cost");
      config.update(draft, config.revision());
      PlayerExperience.at(0).apply(p);
      funds(p, 300);
      enchanting = enchant(p, 3);
      var failedEnchant = enchanting;
      int seed = p.getEnchantmentSeed();
      failedWrite(
          server,
          () -> require(!failedEnchant.clickMenuButton(p, 2), "Failed enchant returns false"));
      require(
          Economy.balance(p) == 300
              && failedEnchant.getSlot(1).getItem().getCount() == 3
              && failedEnchant.getSlot(0).getItem().getEnchantments().isEmpty()
              && p.getEnchantmentSeed() == seed
              && failedEnchant.costs[2] == 30,
          "Failed enchant save restores item, lapis, seed, costs and money");
      passed.accept(
          "optional enchant threshold and full rollback after native enchant save failure");
      off = config.snapshot();
      off.getAsJsonObject("features")
          .getAsJsonObject("coin_enchanting")
          .addProperty("enabled", false);
      config.update(off, config.revision());
      funds(p, 300);
      PlayerExperience.at(ExperienceAccounting.atLevel(31)).apply(p);
      enchanting = enchant(p, 3);
      require(
          enchanting.clickMenuButton(p, 2) && p.experienceLevel == 28 && Economy.balance(p) == 300,
          "Disabled coin enchanting restores vanilla three-level payment");
      passed.accept("disabling coin enchanting restores vanilla XP costs");
    } finally {
      p.containerMenu.setCarried(ItemStack.EMPTY);
      p.closeContainer();
      config.update(original, config.revision());
      state.restore(checkpoint);
      state.save();
      p.discard();
    }
  }
}
