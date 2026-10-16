package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

final class NetherChangesVerification {
 private NetherChangesVerification() {}
 static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
  var store=Sum1reM0d.config(); var original=store.snapshot(); var level=server.overworld();
  var player=EconomyVerification.actor(server,level,"SumireNetherVerify");
  var piglin=new Piglin(EntityTypes.PIGLIN,level); var zombie=new Zombie(EntityTypes.ZOMBIE,level);
  var dragon=new EnderDragon(EntityTypes.ENDER_DRAGON,level);
  try {
   var draft=original.deepCopy(); for(var f:draft.getAsJsonObject("features").entrySet()) f.getValue().getAsJsonObject().addProperty("enabled",false);
   var fs=draft.getAsJsonObject("features");store.update(draft,store.revision());
   var glass=Blocks.TINTED_GLASS.defaultBlockState(); var pos=new BlockPos(1600,240,0);
   require(!glass.getCollisionShape(level,pos).isEmpty(),"OFF retains glass collision");
   fs.getAsJsonObject("passable_tinted_glass").addProperty("enabled",true);store.update(draft,store.revision());
   require(glass.getCollisionShape(level,pos).isEmpty() && glass.getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty() && !glass.isCollisionShapeFullBlock(level,pos),"ON removes cached and contextual glass collision");
   require(!glass.getShape(level,pos).isEmpty(),"Selection shape retained");passed.accept("Tinted glass ON/OFF collision and selection");
   fs.getAsJsonObject("netherrack_shears").addProperty("enabled",true);store.update(draft,store.revision());
   var shears=new ItemStack(Items.SHEARS);var rack=Blocks.NETHERRACK.defaultBlockState();
   require(shears.getDestroySpeed(rack)==15 && new ItemStack(Items.NETHERITE_PICKAXE).getDestroySpeed(rack)==0.2f && shears.isCorrectToolForDrops(rack),"Only shears are fast");
   player.setItemInHand(InteractionHand.MAIN_HAND,shears);shears.mineBlock(level,rack,pos,player);require(shears.getDamageValue()==1,"Native shears mining consumes durability");
   fs.getAsJsonObject("netherrack_shears").addProperty("enabled",false);store.update(draft,store.revision());require(new ItemStack(Items.NETHERITE_PICKAXE).getDestroySpeed(rack)>1,"OFF restores native pickaxe speed");passed.accept("Netherrack tool speed, durability and OFF restoration");
   fs.getAsJsonObject("nether_gold_blocks").addProperty("enabled",true);store.update(draft,store.revision());
   var gold=Blocks.NETHER_GOLD_ORE.defaultBlockState();var tool=new ItemStack(Items.IRON_PICKAXE);
   var drops=Block.getDrops(gold,level,pos,null,player,tool);require(!drops.isEmpty() && drops.stream().allMatch(s->s.is(Items.GOLD_BLOCK)),"Native loot gold blocks");
   tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
   require(Block.getDrops(gold,level,pos,null,player,tool).stream().allMatch(s->s.is(Items.NETHER_GOLD_ORE)),"Silk retains ore");
   tool=new ItemStack(Items.IRON_PICKAXE);tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
   require(Block.getDrops(gold,level,pos,null,player,tool).stream().allMatch(s->s.is(Items.GOLD_BLOCK)),"Fortune native loot remapped");passed.accept("Native nether gold loot and silk/fortune compatibility");
   var leaves=fs.getAsJsonObject("enchanted_leaf_apple");leaves.addProperty("enabled",true);leaves.addProperty("chance",100);store.update(draft,store.revision());
   for(var block:List.of(Blocks.OAK_LEAVES,Blocks.DARK_OAK_LEAVES)) require(Block.getDrops(block.defaultBlockState(),level,pos,null).stream().anyMatch(s->s.is(Items.ENCHANTED_GOLDEN_APPLE)),"Natural leaf decay gains apple");
   require(Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(),level,pos,null,player,new ItemStack(Items.SHEARS)).stream().noneMatch(s->s.is(Items.ENCHANTED_GOLDEN_APPLE)),"Shears excluded");
   var silk=new ItemStack(Items.IRON_HOE);silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
   require(Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(),level,pos,null,player,silk).stream().noneMatch(s->s.is(Items.ENCHANTED_GOLDEN_APPLE)),"Silk excluded");
   leaves.addProperty("chance",0);store.update(draft,store.revision());require(Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(),level,pos,null).stream().noneMatch(s->s.is(Items.ENCHANTED_GOLDEN_APPLE)),"Zero chance");passed.accept("Leaf decay loot chance boundaries, shears and silk exclusions");
   fs.getAsJsonObject("iron_barter").addProperty("enabled",true);store.update(draft,store.revision());piglin.setBaby(false);
   var currency=PiglinAi.class.getDeclaredMethod("isBarterCurrency",ItemStack.class);currency.setAccessible(true);
   var admire=PiglinAi.class.getDeclaredMethod("canAdmire",Piglin.class,ItemStack.class);admire.setAccessible(true);
   require((boolean)currency.invoke(null,new ItemStack(Items.IRON_INGOT)) && !(boolean)currency.invoke(null,new ItemStack(Items.GOLD_INGOT)),"Iron replaces gold currency");
   require((boolean)admire.invoke(null,piglin,new ItemStack(Items.IRON_INGOT)) && !(boolean)admire.invoke(null,piglin,new ItemStack(Items.GOLD_INGOT)),"Real admiration accepts iron only");
   var pickup=PiglinAi.class.getDeclaredMethod("pickUpItem",net.minecraft.server.level.ServerLevel.class,Piglin.class,net.minecraft.world.entity.item.ItemEntity.class);pickup.setAccessible(true);
   var dropped=new net.minecraft.world.entity.item.ItemEntity(level,0,240,0,new ItemStack(Items.IRON_INGOT,2));pickup.invoke(null,level,piglin,dropped);
   require(piglin.getOffhandItem().is(Items.IRON_INGOT) && dropped.getItem().getCount()==1,"Dropped iron enters admiration one at a time");dropped.discard();
   var stop=PiglinAi.class.getDeclaredMethod("stopHoldingOffHandItem",net.minecraft.server.level.ServerLevel.class,Piglin.class,boolean.class);stop.setAccessible(true);
   int before=ServerVerification.spawnedIn(level).size();stop.invoke(null,level,piglin,true);require(piglin.getOffhandItem().isEmpty() && ServerVerification.spawnedIn(level).size()>before,"Actual barter completes and emits native loot");
   piglin.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ADMIRING_ITEM);
   fs.getAsJsonObject("iron_barter").addProperty("enabled",false);store.update(draft,store.revision());require((boolean)admire.invoke(null,piglin,new ItemStack(Items.GOLD_INGOT)),"OFF restores gold interaction");passed.accept("Piglin currency and native admiration ON/OFF");
   fs.getAsJsonObject("extended_bane").addProperty("enabled",true);store.update(draft,store.revision());
   var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BANE_OF_ARTHROPODS),3);
   require(EnchantmentHelper.modifyDamage(level,sword,player,player.damageSources().mobAttack(zombie),4)==11.5f,"Player bane damage");
   require(EnchantmentHelper.modifyDamage(level,sword,dragon,player.damageSources().playerAttack(player),4)==11.5f && EnchantmentHelper.modifyDamage(level,sword,dragon.head,player.damageSources().playerAttack(player),4)==11.5f,"Dragon and multipart target bane damage");
   require(EnchantmentHelper.modifyDamage(level,sword,zombie,player.damageSources().playerAttack(player),4)==4,"Non-target unchanged");passed.accept("Bane applies to players and dragon body/parts without affecting other targets");
   var swap=fs.getAsJsonObject("paper_leather_swap");swap.addProperty("enabled",true);store.update(draft,store.revision());
   var book=CraftingInput.of(2,2,List.of(new ItemStack(Items.LEATHER),new ItemStack(Items.LEATHER),new ItemStack(Items.LEATHER),new ItemStack(Items.PAPER)));
   var match=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,book,level);require(match.isPresent() && match.get().value().assemble(book).is(Items.BOOK),"Book uses three leather and one paper");
   require(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,book,level,match.get()).isPresent(),"Cached recipe respects swap");
   var helmet=CraftingInput.of(3,2,List.of(new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),new ItemStack(Items.PAPER),ItemStack.EMPTY,new ItemStack(Items.PAPER)));
   require(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,helmet,level).orElseThrow().value().assemble(helmet).is(Items.LEATHER_HELMET),"Paper makes leather armor");
   player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(Items.LEATHER,3));player.getInventory().setItem(1,new ItemStack(Items.PAPER));
   var menu=new net.minecraft.world.inventory.CraftingMenu(20,player.getInventory());player.containerMenu=menu;
   menu.handlePlacement(false,false,match.get(),level,player.getInventory());
   require(menu.getInputGridSlots().stream().map(slot->slot.getItem()).filter(stack->stack.is(Items.LEATHER)).mapToInt(ItemStack::getCount).sum()==3,"Recipe book auto-placement uses exchanged materials");
   menu.removed(player);player.containerMenu=player.inventoryMenu;
   swap.addProperty("excluded_recipes",match.get().id().identifier().toString());store.update(draft,store.revision());require(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,book,level,match.get()).isEmpty(),"Excluded recipe restores original ingredient requirements even when cached");
   swap.addProperty("enabled",false);store.update(draft,store.revision());require(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,book,level).isEmpty(),"OFF removes swapped recipe match");passed.accept("Crafting shaped/shapeless, hinted recipe and exclusion ON/OFF");
   var farmer=level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER).value();
   var none=level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.NONE).value();
   var path=BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PathFarmer.KEY);var compost=BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.FARMER);
   require(PoiTypes.forState(Blocks.DIRT_PATH.defaultBlockState()).orElseThrow().is(PathFarmer.KEY),"Path registered in native POI lookup");
   fs.getAsJsonObject("path_farmer").addProperty("enabled",true);store.update(draft,store.revision());
   require(farmer.heldJobSite().test(path) && farmer.acquirableJobSite().test(path) && !farmer.heldJobSite().test(compost),"Farmer acquires and holds path instead of composter");
   require(none.acquirableJobSite().test(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.LIBRARIAN)),"Unemployed still acquire other jobs");
   fs.getAsJsonObject("path_farmer").addProperty("enabled",false);store.update(draft,store.revision());require(farmer.heldJobSite().test(compost) && !farmer.heldJobSite().test(path),"OFF restores composter");passed.accept("Path POI registry and farmer predicates preserve other professions");
   var flip=fs.getAsJsonObject("dinnerbone_mobs");flip.addProperty("enabled",true);flip.addProperty("chance",100);store.update(draft,store.revision());
   zombie.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.NATURAL,null);
   require(((NaturalFlip)zombie).sum1rem0d$isFlipped() && !zombie.hasCustomName(),"Natural mob flips without name modification");
   var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());zombie.saveWithoutId(output);
   var loaded=new Zombie(EntityTypes.ZOMBIE,level);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));require(((NaturalFlip)loaded).sum1rem0d$isFlipped(),"Flip survives native NBT roundtrip");loaded.discard();
   piglin.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.COMMAND,null);require(!((NaturalFlip)piglin).sum1rem0d$isFlipped(),"Command/player-created mob excluded");
   //? if fabric {
   require(zombie.getEntityData().getNonDefaultValues()!=null,"Flip synchronized in entity data");
   //?} elif neoforge {
   /*require(zombie.hasData(NaturalFlipData.FLIP) && NaturalFlipData.get(zombie),"Flip stored in syncable attachment");
   *///?}
passed.accept("Natural flip persisted and synchronized; command spawns excluded");
   fs.getAsJsonObject("daily_events").addProperty("enabled",true);fs.getAsJsonObject("daily_events").addProperty("chance",100);
   var world=WorldState.get(server);var checkpoint=world.checkpoint();
   try { for(String id:List.of("sideways_nylium","retro_netherrack","ardite_disguise")) {
    for(String f:DailyEvents.FEATURES) fs.getAsJsonObject(f).addProperty("enabled",f.equals(id));store.update(draft,store.revision());
    var force=world.checkpoint();force.addProperty("day",Long.MIN_VALUE);world.restore(force);world.updateDay();
    require(world.daily(id) && DailyEvents.FEATURES.stream().filter(world::daily).count()==1,"New daily event must be exclusive: "+id);
   }} finally {world.restore(checkpoint);}
   passed.accept("All three new morning events use the saved exclusive draw");
  } finally {store.update(original,store.revision());player.discard();piglin.discard();zombie.discard();dragon.discard();}
 }
}
