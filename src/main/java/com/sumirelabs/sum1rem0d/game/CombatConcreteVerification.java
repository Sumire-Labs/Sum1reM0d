package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

final class CombatConcreteVerification {
 private CombatConcreteVerification() {}
 private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)));}
 private static void near(float actual,float expected,String message){require(Math.abs(actual-expected)<0.001f,message+": "+actual+" != "+expected);}
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var store=Sum1reM0d.config();var original=store.snapshot();var level=server.overworld();
  var a=EconomyVerification.actor(server,level,"SumireGuardA");var b=EconomyVerification.actor(server,level,"SumireGuardB");
  var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityTypes.ARROW,level);
  try{
   var draft=original.deepCopy();for(var f:draft.getAsJsonObject("features").entrySet())f.getValue().getAsJsonObject().addProperty("enabled",false);
   var fs=draft.getAsJsonObject("features");var guard=fs.getAsJsonObject("sword_guard");guard.addProperty("enabled",true);guard.addProperty("delay_ticks",0);fs.getAsJsonObject("mixed_concrete").addProperty("enabled",true);store.update(draft,store.revision());
   var protection=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
   require(protection.value().canEnchant(new ItemStack(Items.IRON_SWORD)) && protection.value().isPrimaryItem(new ItemStack(Items.IRON_AXE)),"Protection eligible for weapons at anvil/table");
   require(!protection.value().canEnchant(new ItemStack(Items.STICK)),"Non-weapon excluded");
   require(!EnchantmentHelper.getAvailableEnchantmentResults(30,new ItemStack(Items.IRON_SWORD),java.util.stream.Stream.of(protection)).isEmpty(),"Native enchanting table candidate selection includes Protection");
   var anvil=new net.minecraft.world.inventory.AnvilMenu(24,a.getInventory());var book=new ItemStack(Items.ENCHANTED_BOOK);EnchantmentHelper.updateEnchantments(book,mutable->mutable.set(protection,2));
   anvil.getSlot(0).set(new ItemStack(Items.IRON_SWORD));anvil.getSlot(1).set(book);require(!anvil.getSlot(2).getItem().isEmpty() && SwordGuard.rank(anvil.getSlot(2).getItem())==2,"Native anvil applies Protection book to sword");
   anvil.removed(a);
   var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(protection,2);a.setItemInHand(InteractionHand.MAIN_HAND,sword);
   require(sword.getUseAnimation()==ItemUseAnimation.BLOCK && sword.getUseDuration(a)==72000,"Native block animation and use duration");
   var use=sword.getItem().use(level,a,InteractionHand.MAIN_HAND);require(a.isUsingItem() && a.getUseItem()==sword,"Native right-click starts sword guard");
   a.setYRot(0);a.setPos(0,245,0);b.setPos(0,245,2);a.setHealth(20);a.damageCooldownTime=0;
   a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),13,"Protection II guards thirty percent from front");require(sword.getDamageValue()==3,"Guard consumes durability for blocked damage");
   passed.accept("Protection weapon eligibility, native sword use, front guard and durability");
   a.setHealth(20);a.damageCooldownTime=0;b.setPos(0,245,-2);a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),10,"Rear attack not guarded");
   a.setXRot(90);a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),10,"Looking upward does not turn front guard omnidirectional");a.setXRot(0);
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().inFire(),4);near(a.getHealth(),16,"Fire is not guarded");
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().fall(),4);near(a.getHealth(),16,"Fall is not guarded");
   b.setPos(0,245,2);arrow.setPos(0,245,2);a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().arrow(arrow,b),10);near(a.getHealth(),13,"Arrow from front guarded");
   a.stopUsingItem();var fragile=sword.copy();fragile.setDamageValue(fragile.getMaxDamage()-1);a.setItemInHand(InteractionHand.MAIN_HAND,fragile);fragile.getItem().use(level,a,InteractionHand.MAIN_HAND);
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),13,"Final guard still reduces damage when sword breaks");require(a.getMainHandItem().isEmpty() && !a.isUsingItem(),"Broken sword releases the use state immediately");
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),10,"Broken sword cannot keep guarding");
   a.setItemInHand(InteractionHand.MAIN_HAND,sword);sword.getItem().use(level,a,InteractionHand.MAIN_HAND);
   var pierce=net.minecraft.world.entity.projectile.arrow.AbstractArrow.class.getDeclaredMethod("setPierceLevel",byte.class);pierce.setAccessible(true);pierce.invoke(arrow,(byte)1);
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().arrow(arrow,b),10);near(a.getHealth(),10,"Piercing arrow bypasses sword guard");
   guard.addProperty("delay_ticks",100);store.update(draft,store.revision());a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),10,"Guard startup delay enforced");guard.addProperty("delay_ticks",0);store.update(draft,store.revision());
   passed.accept("Guard direction, projectile, environment exclusions and startup delay");
   a.stopUsingItem();a.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));sword.getItem().use(level,a,InteractionHand.MAIN_HAND);require(!a.isUsingItem(),"Offhand shield takes priority");
   a.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);b.setItemInHand(InteractionHand.MAIN_HAND,sword.copy());a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),12,"Protection II reduces outgoing damage by twenty percent");
   var damageTypes=guard.get("damage_types").getAsString();guard.addProperty("damage_types","");store.update(draft,store.revision());
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),12,"Disabling defendable damage types does not remove weapon attack penalty");guard.addProperty("damage_types",damageTypes);
   guard.addProperty("enabled",false);store.update(draft,store.revision());require(!protection.value().canEnchant(new ItemStack(Items.IRON_SWORD)) && protection.value().canEnchant(new ItemStack(Items.IRON_CHESTPLATE)),"OFF restores weapon eligibility without affecting armor");
   a.setHealth(20);a.damageCooldownTime=0;a.hurtServer(level,a.damageSources().playerAttack(b),10);near(a.getHealth(),10,"OFF removes outgoing penalty");
   require(SwordGuard.rank(sword)==2 && sword.getUseAnimation()!=ItemUseAnimation.BLOCK,"OFF retains enchantment but removes guard animation");passed.accept("Offhand priority, outgoing penalty and reversible Config without armor changes");
   var input=CraftingInput.of(3,1,List.of(item("white_concrete"),item("red_dye"),item("blue_dye")));
   var recipe=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow();var result=recipe.value().assemble(input);
   var expected=DyedItemColor.applyDyes(new DyedItemColor(DyeColor.WHITE.getTextureDiffuseColor()&0xFFFFFF),List.of(DyeColor.RED,DyeColor.BLUE));
   require(result.is(MixedConcrete.item()) && result.getCount()==1 && result.get(DataComponents.DYED_COLOR).equals(expected),"Concrete uses native leather dye mixing");
   var repeated=CraftingInput.of(4,1,List.of(item("white_concrete"),item("red_dye"),item("red_dye"),item("blue_dye")));
   require(!recipe.value().assemble(repeated).get(DataComponents.DYED_COLOR).equals(expected),"Repeated dye slots change ratio");
   var redye=CraftingInput.of(2,1,List.of(result,item("black_dye")));require(recipe.value().matches(redye,level) && !recipe.value().assemble(redye).get(DataComponents.DYED_COLOR).equals(expected),"Existing mixed block can be dyed again");
   require(recipe.value().assemble(CraftingInput.of(2,1,List.of(item("white_concrete_powder"),item("red_dye")))).isEmpty(),"Powder excluded");
   require(recipe.value().assemble(CraftingInput.of(3,1,List.of(item("white_concrete"),item("white_concrete"),item("red_dye")))).isEmpty(),"Multiple concrete blocks excluded");passed.accept("Native crafting registration, leather mixing, dye ratios, redye and invalid input rejection");
   var pos=new BlockPos(1700,240,0);ServerVerification.place(level,pos,MixedConcrete.block().defaultBlockState());var entity=(MixedConcreteEntity)level.getBlockEntity(pos);
   require(entity!=null,"Native placed block entity created");entity.applyComponentsFromItemStack(result);require(entity.color()==expected.rgb(),"Placement transfers dyed color");
   var drops=Block.getDrops(MixedConcrete.block().defaultBlockState(),level,pos,entity,a,new ItemStack(Items.IRON_PICKAXE));require(drops.size()==1 && drops.getFirst().is(MixedConcrete.item()) && drops.getFirst().get(DataComponents.DYED_COLOR).equals(expected),"Native loot copies exact RGB");
   var data=entity.getUpdateTag(level.registryAccess());var restored=new MixedConcreteEntity(pos,MixedConcrete.block().defaultBlockState());restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));require(restored.color()==expected.rgb(),"Saved color and client update tag roundtrip");
   require(entity.getUpdatePacket()!=null && entity.getUpdatePacket().getType()==MixedConcrete.type(),"Native block entity packet uses registered type");
   require(entity.item().get(DataComponents.DYED_COLOR).equals(expected),"Pick block color retained");
   var encoded=ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),result).getOrThrow();var decoded=ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),encoded).getOrThrow();require(ItemStack.isSameItemSameComponents(result,decoded),"Item NBT roundtrip preserves exact dye");
   passed.accept("Native block placement, colored loot, pick block, NBT and sync packet color preservation");
   fs.getAsJsonObject("mixed_concrete").addProperty("maximum_dyes",1);store.update(draft,store.revision());require(!recipe.value().matches(input,level) && recipe.value().matches(redye,level),"Config dye count applies live");
   fs.getAsJsonObject("mixed_concrete").addProperty("enabled",false);store.update(draft,store.revision());require(!recipe.value().matches(redye,level) && recipe.value().assemble(redye).isEmpty(),"OFF blocks crafting and stale result extraction");
   require(entity.item().get(DataComponents.DYED_COLOR).equals(expected) && Block.getDrops(MixedConcrete.block().defaultBlockState(),level,pos,entity,a,new ItemStack(Items.IRON_PICKAXE)).getFirst().is(MixedConcrete.item()),"OFF preserves existing colored blocks and drops");
   passed.accept("Mixed concrete live dye limit and OFF preserve existing items and blocks");
  }finally{a.stopUsingItem();a.discard();b.discard();arrow.discard();store.update(original,store.revision());}
 }
}
