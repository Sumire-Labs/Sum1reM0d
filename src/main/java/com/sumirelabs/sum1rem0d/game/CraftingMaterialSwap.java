package com.sumirelabs.sum1rem0d.game;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class CraftingMaterialSwap {
 private static final ThreadLocal<Boolean> MATCHING=ThreadLocal.withInitial(()->false);
 private static net.minecraft.server.MinecraftServer currentServer;
 private static RecipeManager currentManager;
 private static String currentOptions;
 private CraftingMaterialSwap() {}
 public static void close(net.minecraft.server.MinecraftServer server) {if(currentServer==server){currentServer=null;currentManager=null;currentOptions=null;} MATCHING.remove();}
 public static void tick(net.minecraft.server.MinecraftServer server) {
  var manager=server.getRecipeManager();String options=com.sumirelabs.sum1rem0d.Sum1reM0d.config().snapshot().getAsJsonObject("features").get("paper_leather_swap").toString();
  if(currentServer!=server || currentManager!=manager) {currentServer=server;currentManager=manager;currentOptions=options;return;}
  if(options.equals(currentOptions)) return;
  currentOptions=options;manager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());
  for(var player:server.getPlayerList().getPlayers()) {
   player.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(manager.getSynchronizedItemProperties(),manager.getSynchronizedStonecutterRecipes()));
   player.getRecipeBook().sendInitialRecipeBook(player);
   for(var container:java.util.Set.of(player.inventoryMenu)) if(container instanceof net.minecraft.world.inventory.AbstractCraftingMenu menu && !menu.getInputGridSlots().isEmpty()) menu.slotsChanged(menu.getInputGridSlots().getFirst().container);
   if(player.containerMenu!=player.inventoryMenu && player.containerMenu instanceof net.minecraft.world.inventory.AbstractCraftingMenu menu && !menu.getInputGridSlots().isEmpty()) menu.slotsChanged(menu.getInputGridSlots().getFirst().container);
  }
 }

 public static boolean excluded(RecipeHolder<?> holder) { return Rules.on("paper_leather_swap") && Rules.list("paper_leather_swap","excluded_recipes").contains(holder.id().identifier().toString()); }
 public static <T> T original(java.util.function.Supplier<T> action) { boolean previous=MATCHING.get();MATCHING.set(true);try{return action.get();}finally{MATCHING.set(previous);} }
 public static java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display(Recipe<?> recipe,Iterable<RecipeHolder<?>> recipes) {
  if(Rules.on("paper_leather_swap") && !Rules.list("paper_leather_swap","excluded_recipes").isEmpty()) for(var holder:recipes) if(holder.value()==recipe && excluded(holder)) return original(recipe::display);
  return recipe.display();
 }

 public static boolean ingredientSwap() { return !MATCHING.get() && Rules.on("paper_leather_swap"); }
 @SuppressWarnings({"unchecked","rawtypes"})
 public static boolean matches(Recipe recipe,RecipeInput input,Level level) {
  if(!(input instanceof CraftingInput crafting) || !Rules.on("paper_leather_swap")) return recipe.matches(input,level);
  boolean excluded=false;
  if(level instanceof net.minecraft.server.level.ServerLevel server && !Rules.list("paper_leather_swap","excluded_recipes").isEmpty()) excluded=server.getServer().getRecipeManager().getRecipes().stream().anyMatch(holder->holder.value()==recipe && Rules.list("paper_leather_swap","excluded_recipes").contains(holder.id().identifier().toString()));
  RecipeInput mapped=excluded ? input : CraftingInput.of(crafting.width(),crafting.height(),crafting.items().stream().map(NetherChanges::swapMaterial).toList());
  boolean previous=MATCHING.get(); MATCHING.set(true);
  try { return recipe.matches(mapped,level); }
  finally { MATCHING.set(previous); }
 }
}
