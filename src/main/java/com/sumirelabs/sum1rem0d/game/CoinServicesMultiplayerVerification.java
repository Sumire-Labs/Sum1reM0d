package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.mixin.CoinEnchantmentSeedAccess;
import com.sumirelabs.sum1rem0d.network.ServerConfigHandler;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in single-client native services round trip in a disposable server world. */
public final class CoinServicesMultiplayerVerification {
  private static ServerPlayer actor;
  private static JsonObject original, checkpoint;
  private static List<ItemStack> inventory;
  private static PlayerExperience xp;
  private static String marker;
  private static net.minecraft.world.phys.Vec3 position;
  private static net.minecraft.world.level.GameType mode;
  private static boolean flying, mayfly, restored, finished;
  private static int seed, enchantStat, phase = -1, ack = -1;
  private static long started, finishAt;
  private static final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
  private static final List<String> passed = new ArrayList<>();

  private CoinServicesMultiplayerVerification() {}

  public static int phase() {
    return Boolean.getBoolean("sum1rem0d.coinServicesVerify") ? phase : -1;
  }

  public static boolean temporaryInventory(ServerPlayer p) {
    return Boolean.getBoolean("sum1rem0d.coinServicesVerify") && actor == p && !restored;
  }

  public static void acknowledge(ServerPlayer p, long value) {
    if (p == actor && value == phase) ack = (int) value;
  }

  private static void next(int value) {
    phase = value;
    ack = -1;
    Sum1reM0d.LOGGER.info("COIN_SERVICES_MULTI phase {}", phase);
    ServerConfigHandler.send(actor, "");
    Gameplay.sendState(actor);
  }

  private static void place(BlockPos pos, BlockState block) {
    var level = actor.level();
    level.getChunkAt(pos);
    if (!blocks.containsKey(pos)) {
      ServerVerification.require(
          level.getBlockState(pos).isAir(),
          "Coin services fixtures require empty sky positions: " + pos);
      blocks.put(pos.immutable(), level.getBlockState(pos));
    }
    level.setBlock(pos, block, 3);
  }

  private static BlockPos anvilPos() {
    return new BlockPos(0, 241, 0);
  }

  private static BlockPos enchantPos() {
    return new BlockPos(4, 241, 0);
  }

  public static void tick(MinecraftServer server) {
    if (!Boolean.getBoolean("sum1rem0d.coinServicesVerify") || finished) return;
    try {
      if (actor == null) {
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerVerification.require(
            server.getPlayerList().getPlayers().size() == 1,
            "Only one local verification client may join");
        actor = server.getPlayerList().getPlayers().getFirst();
        ServerVerification.require(
            actor.getGameProfile().name().equals("Developer"),
            "Only the local verification account may join");
        started = System.nanoTime();
        original = Sum1reM0d.config().snapshot();
        checkpoint = WorldState.get(server).checkpoint();
        inventory = new ArrayList<>();
        for (int i = 0; i < actor.getInventory().getContainerSize(); i++)
          inventory.add(actor.getInventory().getItem(i).copy());
        xp = PlayerExperience.capture(actor);
        marker = ((CoinInventoryRevision) actor).sum1rem0d$coinRevision();
        position = actor.position();
        mode = actor.gameMode.getGameModeForPlayer();
        flying = actor.getAbilities().flying;
        mayfly = actor.getAbilities().mayfly;
        seed = actor.getEnchantmentSeed();
        enchantStat = actor.getStats().getValue(Stats.CUSTOM.get(Stats.ENCHANT_ITEM));
        next(0);
        return;
      }
      if (System.nanoTime() - started > 240_000_000_000L)
        throw new AssertionError("Coin services round trip timeout phase=" + phase);
      switch (phase) {
        case 0 -> {
          if (ack != 0) return;
          var config = original.deepCopy();
          for (var entry : config.getAsJsonObject("features").entrySet())
            entry.getValue().getAsJsonObject().addProperty("enabled", false);
          for (String id : List.of("economy", "xp_bank", "coin_anvil", "coin_enchanting"))
            config.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled", true);
          Sum1reM0d.config().update(config, Sum1reM0d.config().revision());
          actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
          actor.getAbilities().mayfly = true;
          actor.getAbilities().flying = true;
          actor.onUpdateAbilities();
          actor.setNoGravity(true);
          actor.connection.teleport(.5, 242, 2.5, actor.getYRot(), actor.getXRot());
          actor.getInventory().clearContent();
          actor.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
          actor.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 3));
          actor.getInventory().setItem(2, new ItemStack(Items.IRON_SWORD));
          PlayerExperience.at(1395).apply(actor);
          PlayerExperience.capture(actor).send(actor);
          var data = WorldState.get(server).player(actor);
          data.walletInitialized = true;
          data.coins = 500;
          actor.inventoryMenu.broadcastFullState();
          next(1);
        }
        case 1 -> {
          if (Economy.balance(actor) == 600 && PlayerExperience.capture(actor).points() == 1295) {
            passed.add(
                "actual bank GUI converts 100 XP points into 100 coins with synchronized XP bar");
            next(2);
          }
        }
        case 2 -> {
          if (Economy.balance(actor) == 500 && PlayerExperience.capture(actor).points() == 1395) {
            passed.add(
                "actual bank GUI converts coins back without losing points at a level boundary");
            PlayerExperience.at(0).apply(actor);
            PlayerExperience.capture(actor).send(actor);
            place(anvilPos().below(), Blocks.STONE.defaultBlockState());
            place(anvilPos(), Blocks.ANVIL.defaultBlockState());
            actor.openMenu(
                new SimpleMenuProvider(
                    (id, inv, p) ->
                        new AnvilMenu(
                            id, inv, ContainerLevelAccess.create(actor.level(), anvilPos())),
                    net.minecraft.network.chat.Component.translatable("container.repair")));
            var menu = (AnvilMenu) actor.containerMenu;
            menu.getSlot(0).set(actor.getInventory().removeItem(0, 1));

            menu.broadcastFullState();
            next(3);
          }
        }
        case 3 -> {
          if (Economy.balance(actor) == 490) {
            ServerVerification.require(
                PlayerExperience.capture(actor).points() == 0
                    && actor.containerMenu.getCarried().is(Items.IRON_SWORD),
                "Anvil extraction uses coins with zero XP");
            passed.add("native anvil GUI and result click charge 10 coins while XP stays zero");
            actor.closeContainer();
            place(enchantPos().below(), Blocks.STONE.defaultBlockState());
            place(enchantPos(), Blocks.ENCHANTING_TABLE.defaultBlockState());
            int books = 0;
            for (int x = -2; x <= 2; x++)
              for (int z = -2; z <= 2; z++)
                if (Math.max(Math.abs(x), Math.abs(z)) == 2 && books++ < 15)
                  place(enchantPos().offset(x, 0, z), Blocks.BOOKSHELF.defaultBlockState());
            actor.openMenu(
                new SimpleMenuProvider(
                    (id, inv, p) ->
                        new EnchantmentMenu(
                            id, inv, ContainerLevelAccess.create(actor.level(), enchantPos())),
                    net.minecraft.network.chat.Component.translatable("container.enchant")));
            var menu = (EnchantmentMenu) actor.containerMenu;
            menu.getSlot(0).set(actor.getInventory().removeItem(2, 1));
            menu.getSlot(1).set(actor.getInventory().removeItem(1, 3));
            ServerVerification.require(
                menu.costs[2] == 30, "Fifteen real bookshelves produce a level-30 enchant offer");
            menu.broadcastFullState();
            next(4);
          }
        }
        case 4 -> {
          if (Economy.balance(actor) == 460) {
            var menu = (EnchantmentMenu) actor.containerMenu;
            ServerVerification.require(
                PlayerExperience.capture(actor).points() == 0
                    && menu.getSlot(1).getItem().isEmpty()
                    && !menu.getSlot(0).getItem().getEnchantments().isEmpty(),
                "Native enchant consumes 30 coins and lapis without XP");
            passed.add(
                "real enchantment GUI click succeeds with zero XP and native bookshelves, consuming"
                    + " 30 coins and three lapis");
            next(5);
          }
        }
        case 5 -> {
          if (ack != 5) return;
          restore(server);
          next(6);
        }
        case 6 -> {
          if (ack != 6) return;
          passed.add(
              "original inventory, XP, balance, config, abilities, enchant seed and fixture blocks"
                  + " restored");
          finishAt = server.overworld().getGameTime() + 40;
          next(7);
        }
        case 7 -> {
          if (server.overworld().getGameTime() >= finishAt) finish(server, true, "");
        }
      }
    } catch (Throwable failure) {
      Sum1reM0d.LOGGER.error("COIN_SERVICES_MULTI failed", failure);
      finish(server, false, failure.toString());
    }
  }

  private static void restore(MinecraftServer server) throws Exception {
    if (restored || original == null) return;
    actor.closeContainer();
    for (int i = 0; i < inventory.size(); i++)
      actor.getInventory().setItem(i, inventory.get(i).copy());
    xp.apply(actor);
    xp.send(actor);
    ((CoinInventoryRevision) actor).sum1rem0d$coinRevision(marker);
    ((CoinEnchantmentSeedAccess) actor).sum1rem0d$enchantmentSeed(seed);
    actor.getStats().setValue(actor, Stats.CUSTOM.get(Stats.ENCHANT_ITEM), enchantStat);
    actor.setGameMode(mode);
    actor.getAbilities().mayfly = mayfly;
    actor.getAbilities().flying = flying;
    actor.onUpdateAbilities();
    actor.setNoGravity(false);
    actor.connection.teleport(position.x, position.y, position.z, actor.getYRot(), actor.getXRot());
    for (var entry : new ArrayList<>(blocks.entrySet()).reversed())
      actor.level().setBlock(entry.getKey(), entry.getValue(), 3);
    WorldState.get(server).restore(checkpoint);
    WorldState.get(server).saveStrict();
    Sum1reM0d.config().update(original, Sum1reM0d.config().revision());
    actor.inventoryMenu.broadcastFullState();
    restored = true;
    server.getPlayerList().saveAll();
  }

  public static void leaving(ServerPlayer p) {
    if (Boolean.getBoolean("sum1rem0d.coinServicesVerify") && p == actor && !restored && !finished)
      finish(p.level().getServer(), false, "Client disconnected before fixture restoration");
  }

  public static void stopping(MinecraftServer server) {
    if (Boolean.getBoolean("sum1rem0d.coinServicesVerify") && !finished)
      finish(server, false, "Server stopped before verification completed");
  }

  private static void finish(MinecraftServer server, boolean success, String failure) {
    if (finished) return;
    finished = true;
    try {
      restore(server);
    } catch (Exception problem) {
      success = false;
      failure += "; restoration " + problem;
    }
    var result = new JsonObject();
    result.addProperty("success", success);
    result.addProperty("failure", failure);
    result.add("passed", new Gson().toJsonTree(passed));
    try {
      Files.writeString(
          server
              .getWorldPath(LevelResource.ROOT)
              .resolve("sum1rem0d-coin-services-multiplayer.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(result));
    } catch (Exception problem) {
      Sum1reM0d.LOGGER.error("Cannot save coin services report", problem);
    }
    server.halt(false);
  }
}
