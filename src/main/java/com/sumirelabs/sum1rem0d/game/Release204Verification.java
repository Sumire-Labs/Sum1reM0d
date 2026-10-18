package com.sumirelabs.sum1rem0d.game;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
final class Release204Verification {
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var saved=config.snapshot();var draft=saved.deepCopy();var fs=draft.getAsJsonObject("features");
  for(String name:List.of("stackable_fluids","creeper_shears","piston_base","netherite_upgrade","tesla_coil"))fs.getAsJsonObject(name).addProperty("enabled",true);
  fs.getAsJsonObject("stackable_fluids").addProperty("stack_size",16);fs.getAsJsonObject("piston_base").addProperty("force_air",true);config.update(draft,config.revision());
  var level=server.overworld();var actor=EconomyVerification.actor(server,level,"Release204");
  try{
   for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.WATER_BUCKET,Items.LAVA_BUCKET,Items.MILK_BUCKET,Items.POWDER_SNOW_BUCKET,Items.COD_BUCKET))require(new ItemStack(item).getMaxStackSize()==16,"Filled fluid container stacks to 16: "+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
   var used=ItemUtils.createFilledResult(new ItemStack(Items.WATER_BUCKET,16),actor,new ItemStack(Items.BUCKET));
   require(used.is(Items.WATER_BUCKET)&&used.getCount()==15&&actor.getInventory().countItem(Items.BUCKET)==1,"Using one filled bucket keeps fifteen and returns one empty bucket");
   passed.accept("All potion/bucket variants use 16 stacks and native container-return preserves remaining quantity");
   PaxelContent.refreshDefaults();
   for(String material:PaxelContent.MATERIALS){String namespace=List.of("emerald","redstone","lapis").contains(material)?"sum1rem0d":"minecraft";var sword=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace,material+"_sword")));var paxel=new ItemStack(PaxelContent.item(material));require(Math.abs(attack(paxel)-attack(sword)-1)<.0001,"Paxel attack is material sword plus one: "+material);}
   for(var pick:List.of(ExtraContent.item("emerald_pickaxe"),PaxelContent.item("emerald"))){var stack=new ItemStack(pick);require(stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())&&!stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"Emerald tool uses iron harvest tier");}
   var netherite=new ItemStack(Items.NETHERITE_SWORD);double boosted=attack(netherite);var off=config.snapshot();off.getAsJsonObject("features").getAsJsonObject("netherite_upgrade").addProperty("enabled",false);config.update(off,config.revision());require(Math.abs(boosted-attack(netherite)-Rules.n("netherite_upgrade","attack_bonus"))<.0001,"Native equipped attribute path reflects the configured netherite attack bonus exactly once");config.update(draft,config.revision());
   passed.accept("Paxel sword-plus-one attack, iron-tier emerald harvesting and native netherite attribute bonus");
   var root=new BlockPos(2310,245,100);var base=(PistonRootBlock)ExtraContent.block("piston_base");var baseState=base.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST);ServerVerification.place(level,root,baseState);var target=root.east();ServerVerification.place(level,target,Blocks.CHEST.defaultBlockState());((net.minecraft.world.Container)level.getBlockEntity(target)).setItem(0,new ItemStack(Items.DIAMOND));base.setPlacedBy(level,root,baseState,actor,new ItemStack(base));require(level.getBlockState(target).isAir()&&level.getBlockEntity(target)==null,"Piston root clears a block entity to air");ServerVerification.place(level,target,Blocks.NETHER_PORTAL.defaultBlockState());base.setPlacedBy(level,root,baseState,actor,new ItemStack(base));require(level.getBlockState(target).isAir(),"Piston root clears a portal to air");
   passed.accept("Forced-air piston handles containers and portals");
   var creeper=(net.minecraft.world.entity.monster.Creeper)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("creeper")).create(level,EntitySpawnReason.COMMAND);creeper.setPos(2315,245,100);actor.setShiftKeyDown(true);int before=actor.getInventory().countItem(Items.GUNPOWDER);require(WorldEntityRules.shearCreeper(actor,creeper),"Sneaking player harvests creeper gunpowder");require(actor.getInventory().countItem(Items.GUNPOWDER)==before+Rules.i("creeper_shears","initial_drop")&&!WorldEntityRules.shearCreeper(actor,creeper),"Creeper cooldown prevents immediate repeat harvest");creeper.discard();
   passed.accept("Sneak creeper interaction grants gunpowder once and enforces cooldown");
   var hugeDrops=Block.getDrops(MobilityContent.hugeCauldron().defaultBlockState(),level,new BlockPos(2320,245,100),null,actor,new ItemStack(Items.DIAMOND_PICKAXE));
   require(hugeDrops.stream().filter(stack->stack.is(Items.CAULDRON)).mapToInt(ItemStack::getCount).sum()==4,"Four-cell cauldron loot returns four vanilla cauldrons");
   CauldronFixVerification.verify(server,passed);
  }finally{config.update(saved,config.revision());actor.discard();}
 }
 private static double attack(ItemStack stack){double[] total={0};stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(Attributes.ATTACK_DAMAGE)&&modifier.operation()==AttributeModifier.Operation.ADD_VALUE)total[0]+=modifier.amount();});return total[0];}
}
