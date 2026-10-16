package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;

final class MudBallVerification {
  private MudBallVerification() {}

  private static void clearCooldown(net.minecraft.server.level.ServerPlayer p) {
    for (int i = 0; i < 21; i++) p.getCooldowns().tick();
  }

  static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
    var store = Sum1reM0d.config();
    var original = store.snapshot();
    var world = WorldState.get(server);
    var checkpoint = world.checkpoint();
    var p = EconomyVerification.actor(server, server.overworld(), "SumireMudVerify");
    try {
      var draft = original.deepCopy();
      for (var f : draft.getAsJsonObject("features").entrySet())
        f.getValue().getAsJsonObject().addProperty("enabled", false);
      var mud = draft.getAsJsonObject("features").getAsJsonObject("mud_ball");
      var clean = draft.getAsJsonObject("features").getAsJsonObject("cleanliness");
      mud.addProperty("enabled", true);
      mud.addProperty("cleanliness_loss", 25);
      mud.addProperty("cooldown_ticks", 20);
      mud.addProperty("consume", true);
      mud.addProperty("recipe_enabled", true);
      mud.addProperty("mud_materials", "minecraft:dirt,minecraft:clay_ball");
      mud.addProperty("recipe_output", 4);
      clean.addProperty("enabled", true);
      clean.addProperty("maximum", 100);
      clean.addProperty("health_threshold", 50);
      clean.addProperty("minimum_health", 10);
      clean.addProperty("decay_per_minute", 0);
      clean.addProperty("soap_recovery", 50);
      store.update(draft, store.revision());
      p.setGameMode(GameType.SURVIVAL);
      var data = world.player(p);
      data.cleanliness = 100;
      p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.mudBall(), 3));
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 75 && p.getMainHandItem().getCount() == 2,
          "Mud consumes one and removes twenty-five cleanliness");
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 75 && p.getMainHandItem().getCount() == 2,
          "Repeated packets cannot bypass native cooldown");
      clearCooldown(p);
      data.cleanliness = 1;
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(data.cleanliness == 0 && p.getMainHandItem().getCount() == 1, "Mud clamps at zero");
      clearCooldown(p);
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 0 && p.getMainHandItem().getCount() == 1,
          "Zero cleanliness does not waste an item");
      Gameplay.tickPlayer(p, world, 20);
      require(
          p.getMaxHealth() == 10, "Mud participates in the existing cleanliness health penalty");
      p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.soap(), 2));
      ModContent.soap().use(p.level(), p, InteractionHand.MAIN_HAND);
      Gameplay.tickPlayer(p, world, 40);
      require(
          data.cleanliness == 50 && p.getMaxHealth() == 20,
          "Soap reverses mud dirt and restores maximum health");
      passed.accept(
          "mud native consumption, cooldown/replay protection, zero clamp, health penalty and soap"
              + " recovery");
      p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.mudBall(), 2));
      mud.addProperty("cooldown_ticks", 0);
      mud.addProperty("consume", false);
      store.update(draft, store.revision());
      data.cleanliness = 100;
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 75 && p.getMainHandItem().getCount() == 2,
          "No-consume mode is reusable");
      mud.addProperty("consume", true);
      store.update(draft, store.revision());
      p.setGameMode(GameType.CREATIVE);
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 50 && p.getMainHandItem().getCount() == 2,
          "Creative mode follows vanilla no-consume behavior");
      p.setGameMode(GameType.SPECTATOR);
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(data.cleanliness == 50, "Spectator cannot apply mud");
      p.setGameMode(GameType.SURVIVAL);
      mud.addProperty("cleanliness_loss", 0);
      store.update(draft, store.revision());
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(
          data.cleanliness == 50 && p.getMainHandItem().getCount() == 2,
          "Zero loss is free of item side effects");
      mud.addProperty("enabled", false);
      store.update(draft, store.revision());
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(data.cleanliness == 50, "Individual OFF prevents mud use");
      mud.addProperty("enabled", true);
      clean.addProperty("enabled", false);
      store.update(draft, store.revision());
      ModContent.mudBall().use(p.level(), p, InteractionHand.MAIN_HAND);
      require(data.cleanliness == 50, "Cleanliness OFF prevents mud use");
      passed.accept(
          "mud Config no-consume/zero-loss modes, creative/spectator behavior and dependency"
              + " switches");
      clean.addProperty("enabled", true);
      store.update(draft, store.revision());
      var input =
          CraftingInput.of(
              2,
              2,
              List.of(
                  new ItemStack(Items.DIRT),
                  new ItemStack(Items.CLAY_BALL),
                  ItemStack.EMPTY,
                  ItemStack.EMPTY));
      var recipe = p.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, p.level());
      require(
          recipe.isPresent()
              && recipe.get().value().assemble(input).is(ModContent.mudBall())
              && recipe.get().value().assemble(input).getCount() == 4,
          "Registered two-by-two recipe produces four mud balls");
      mud.addProperty("recipe_output", 7);
      store.update(draft, store.revision());
      require(
          recipe.get().value().assemble(input).getCount() == 7,
          "Live output configuration reaches native crafting");
      mud.addProperty("recipe_enabled", false);
      store.update(draft, store.revision());
      require(
          p.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, p.level()).isEmpty(),
          "Recipe OFF removes the craft");
      CreativeModeTabs.tryRebuildTabContents(p.level().enabledFeatures(), true, p.registryAccess());
      var tab =
          BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Rules.id("minecraft:tools_and_utilities"));
      require(
          tab.getDisplayItems().stream().anyMatch(s -> s.is(ModContent.mudBall()))
              && CreativeModeTabs.searchTab().contains(new ItemStack(ModContent.mudBall())),
          "Mud is discoverable in creative utility and search tabs");
      passed.accept(
          "mud registered crafting, live recipe configuration and creative/search tab"
              + " registration");
    } finally {
      store.update(original, store.revision());
      world.restore(checkpoint);
      world.save();
      p.discard();
    }
  }
}
