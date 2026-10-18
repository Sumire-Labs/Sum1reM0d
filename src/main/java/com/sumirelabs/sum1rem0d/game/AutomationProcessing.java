package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server-only processing with bounded item movement across already-loaded adjacent inventories. */
public final class AutomationProcessing {
    private static final Set<AutomationMachineEntity> LOADED=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<AutomationMachineEntity,ServerPlayer> CLICKER_ACTORS=new IdentityHashMap<>();
    private record WorldPos(String dimension,long pos){}
    private static final Map<WorldPos,Long> WASH_NEXT=new HashMap<>(),TESLA_NEXT=new HashMap<>(),CRAFT_NEXT=new HashMap<>(),GRIND_NEXT=new HashMap<>();
    private static final Set<WorldPos> TESLA_RODS=new HashSet<>();
    private AutomationProcessing(){}
    static void loaded(AutomationMachineEntity entity){LOADED.add(entity);}
    static void unloaded(AutomationMachineEntity entity){LOADED.remove(entity);var actor=CLICKER_ACTORS.remove(entity);if(actor!=null)actor.discard();}
    public static boolean enabled(String kind){return switch(kind){case "automated_crafting"->Rules.on("automated_crafting");case "jukebox_grinder"->Rules.on("jukebox_grinder");case "washing_cauldron"->Rules.on("washing_cauldron");case "tesla_coil"->Rules.on("tesla_coil");case "auto_clicker"->Rules.on("auto_clicker");default->false;};}
    public static void tick(MinecraftServer server){
        for(var machine:List.copyOf(LOADED)){if(!(machine.getLevel() instanceof ServerLevel level)||level.getServer()!=server||machine.isRemoved())continue;tick(machine,level);}
        tickNativeVanilla(server);
    }
    public static void trackTeslaRod(ServerLevel level,BlockPos pos){TESLA_RODS.add(new WorldPos(level.dimension().identifier().toString(),pos.asLong()));}
    private static void tickNativeVanilla(MinecraftServer server){
        for(var level:server.getAllLevels()){
            ((com.sumirelabs.sum1rem0d.mixin.ChunkMapAccessMixin)(Object)level.getChunkSource().chunkMap).sum1rem0d$forEachBlockTickingChunk(chunk->{for(var entity:chunk.getBlockEntities().values())if(entity instanceof Container container&&!(entity instanceof AutomationMachineEntity)){
                var chestPos=entity.getBlockPos();var cauldronPos=chestPos.below();if(!level.hasChunkAt(cauldronPos))continue;
                String containerId=BuiltInRegistries.BLOCK.getKey(level.getBlockState(chestPos).getBlock()).getPath();if(!(containerId.equals("chest")||containerId.equals("barrel")))continue;
                if(washableWater(level,cauldronPos)&&enabled("washing_cauldron")){
                    var key=new WorldPos(level.dimension().identifier().toString(),cauldronPos.asLong());if(level.getGameTime()>=WASH_NEXT.getOrDefault(key,Long.MIN_VALUE)&&tryNativeWash(level,cauldronPos))WASH_NEXT.put(key,level.getGameTime()+Math.max(1,Rules.i("washing_cauldron","interval_ticks")));
                }
            }});
        }
        tickNativeTesla(server);
    }
    private static ServerLevel findLevel(MinecraftServer server,String dimension){for(ServerLevel level:server.getAllLevels())if(level.dimension().identifier().toString().equals(dimension))return level;return null;}
    static void tickNativeTesla(MinecraftServer server){
        TESLA_RODS.removeIf(key->{ServerLevel level=findLevel(server,key.dimension());if(level==null)return true;var pos=BlockPos.of(key.pos());if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);if(!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("minecraft:lightning_rod"))return true;if(!Rules.on("tesla_coil"))return false;
            var heavy=pos.above();if(!level.hasChunkAt(heavy)||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(heavy).getBlock()).toString().equals("minecraft:heavy_core"))return false;
            boolean powered=level.hasNeighborSignal(pos)||(state.hasProperty(BlockStateProperties.POWERED)&&state.getValue(BlockStateProperties.POWERED));if(!powered)return false;
            long now=level.getGameTime();if(now<TESLA_NEXT.getOrDefault(key,Long.MIN_VALUE))return false;double radius=Rules.n("tesla_coil","range"),damage=Rules.n("tesla_coil","damage");attackTeslaTargets(level,pos,radius,(float)damage);TESLA_NEXT.put(key,now+Math.max(1,Rules.i("tesla_coil","interval_ticks")));return false;});
    }
    private static void tick(AutomationMachineEntity machine,ServerLevel level){
        String kind=machine.kind();if(kind.equals("auto_clicker")&&enabled(kind))clicker(machine,level);
    }
    private record SlotRef(Container container,int slot){}
    private static List<Container> containersAround(ServerLevel level,BlockPos origin,boolean above){
        var result=new ArrayList<Container>();
        if(above){var pos=origin.above();if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof Container c&&!(c instanceof AutomationMachineEntity))result.add(c);}
        else for(var dir:Direction.Plane.HORIZONTAL){var pos=origin.relative(dir);if(!level.hasChunkAt(pos))continue;if(level.getBlockEntity(pos) instanceof Container c&&!(c instanceof AutomationMachineEntity))result.add(c);}
        return result;
    }
    private static List<Container> sideContainersExcept(ServerLevel level,BlockPos origin,Direction excluded){var result=new ArrayList<Container>();for(var dir:Direction.Plane.HORIZONTAL){if(dir==excluded)continue;var pos=origin.relative(dir);if(!level.hasChunkAt(pos))continue;if(level.getBlockEntity(pos) instanceof Container c&&!(c instanceof AutomationMachineEntity))result.add(c);}return result;}
    private static List<SlotRef> slots(List<Container> containers){var slots=new ArrayList<SlotRef>();for(var c:containers)for(int i=0;i<c.getContainerSize();i++)slots.add(new SlotRef(c,i));return slots;}
    private static boolean output(List<Container> containers,ItemStack stack){
        if(stack.isEmpty())return true;var slots=slots(containers);var current=new IdentityHashMap<Container,ItemStack[]>();for(var c:containers){var items=new ItemStack[c.getContainerSize()];for(int i=0;i<items.length;i++)items[i]=c.getItem(i).copy();current.put(c,items);}
        int left=stack.getCount();for(var ref:slots){var at=current.get(ref.container());var value=at[ref.slot()];if(!value.isEmpty()&&ItemStack.isSameItemSameComponents(value,stack)){int n=Math.min(left,Math.max(0,value.getMaxStackSize()-value.getCount()));if(n>0){value.grow(n);left-=n;}if(left==0)break;}}
        for(var ref:slots){if(left==0)break;var at=current.get(ref.container());if(at[ref.slot()].isEmpty()){int n=Math.min(left,stack.getMaxStackSize());at[ref.slot()]=stack.copyWithCount(n);left-=n;}}
        if(left>0)return false;
        for(var c:containers){var after=current.get(c);for(int i=0;i<after.length;i++)if(!ItemStack.matches(c.getItem(i),after[i]))c.setItem(i,after[i]);c.setChanged();}return true;
    }
    private static boolean consume(SlotRef ref,int count){var stack=ref.container().getItem(ref.slot());if(stack.getCount()<count)return false;stack.shrink(count);if(stack.isEmpty())ref.container().setItem(ref.slot(),ItemStack.EMPTY);else ref.container().setChanged();return true;}
    /** Vanilla Crafter uses its 3x3 contents as a persistent recipe sample; side barrels fund the output. */
    public static boolean craftFromVanillaCrafter(ServerLevel level,BlockPos pos,CrafterBlockEntity crafter){
        if(!enabled("automated_crafting"))return false;var key=new WorldPos(level.dimension().identifier().toString(),pos.asLong());if(level.getGameTime()<CRAFT_NEXT.getOrDefault(key,Long.MIN_VALUE))return false;
        var pattern=new ArrayList<ItemStack>(9);for(int i=0;i<9;i++){var stack=crafter.getItem(i);pattern.add(crafter.isSlotDisabled(i)||stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1));}
        var input=CraftingInput.of(3,3,pattern);var match=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,level).orElse(null);if(match==null)return false;var recipe=match.value();var result=recipe.assemble(input);if(result.isEmpty())return false;var leftovers=recipe.getRemainingItems(input).stream().filter(item->!item.isEmpty()).toList();
        var forward=pos.relative(crafter.getBlockState().getValue(BlockStateProperties.ORIENTATION).front());if(!level.hasChunkAt(forward))return false;Container output=level.getBlockEntity(forward) instanceof Container c?c:null;var outputs=new ArrayList<ItemStack>();outputs.add(result.copy());outputs.addAll(leftovers.stream().map(ItemStack::copy).toList());ItemStack[] post=output==null?null:planInsertMany(output,outputs);if(output!=null&&post==null)return false;
        var sources=sideContainersExcept(level,pos,crafter.getBlockState().getValue(BlockStateProperties.ORIENTATION).front());var refs=slots(sources);var needed=new IdentityHashMap<Container,int[]>();for(var c:sources)needed.put(c,new int[c.getContainerSize()]);
        for(var sampleStack:pattern)if(!sampleStack.isEmpty()){SlotRef found=null;for(var ref:refs){var stack=ref.container().getItem(ref.slot());int used=needed.get(ref.container())[ref.slot()];if(stack.getCount()>used&&ItemStack.isSameItemSameComponents(stack,sampleStack)){found=ref;break;}}if(found==null)return false;needed.get(found.container())[found.slot()]++;}
        for(var c:sources){var counts=needed.get(c);for(int i=0;i<counts.length;i++)if(counts[i]>0&&!consume(new SlotRef(c,i),counts[i]))return false;}
        if(output!=null){for(int i=0;i<post.length;i++)if(!ItemStack.matches(output.getItem(i),post[i]))output.setItem(i,post[i]);output.setChanged();}
        else for(var stack:outputs)net.minecraft.world.level.block.Block.popResource(level,forward,stack);
        crafter.setChanged();CRAFT_NEXT.put(key,level.getGameTime()+Math.max(1,Rules.i("automated_crafting","interval_ticks")));return true;
    }
    private static ItemStack[] planInsertMany(Container container,List<ItemStack> sources){var items=new ItemStack[container.getContainerSize()];for(int i=0;i<items.length;i++)items[i]=container.getItem(i).copy();for(var source:sources){int left=source.getCount();for(var existing:items)if(!existing.isEmpty()&&ItemStack.isSameItemSameComponents(existing,source)){int move=Math.min(left,Math.max(0,existing.getMaxStackSize()-existing.getCount()));if(move>0){existing.grow(move);left-=move;}if(left==0)break;}for(int i=0;i<items.length&&left>0;i++)if(items[i].isEmpty()){int move=Math.min(left,source.getMaxStackSize());items[i]=source.copyWithCount(move);left-=move;}if(left>0)return null;}return items;}
    private static ItemStack[] planInsert(Container container,ItemStack source){
        var items=new ItemStack[container.getContainerSize()];for(int i=0;i<items.length;i++)items[i]=container.getItem(i).copy();int left=source.getCount();
        for(var item:items)if(!item.isEmpty()&&ItemStack.isSameItemSameComponents(item,source)){int n=Math.min(left,Math.max(0,item.getMaxStackSize()-item.getCount()));if(n>0){item.grow(n);left-=n;}if(left==0)return items;}
        for(int i=0;i<items.length&&left>0;i++)if(items[i].isEmpty()){int n=Math.min(left,source.getMaxStackSize());items[i]=source.copyWithCount(n);left-=n;}
        return left==0?items:null;
    }
    public static ItemStack grindResult(ItemStack in){
        if(in.is(Items.RAW_IRON))return new ItemStack(AutomationContent.item("iron_dust"),2);if(in.is(Items.RAW_COPPER))return new ItemStack(AutomationContent.item("copper_dust"),2);if(in.is(Items.RAW_GOLD))return new ItemStack(AutomationContent.item("gold_dust"),2);
        if(in.is(Items.COBBLESTONE))return new ItemStack(Items.GRAVEL);if(in.is(Items.GRAVEL))return new ItemStack(Items.SAND);if(in.is(Items.SAND))return new ItemStack(AutomationContent.item("dust"));if(in.is(Items.BLAZE_ROD))return new ItemStack(Items.BLAZE_POWDER,3);if(in.is(Items.SUGAR_CANE))return new ItemStack(Items.SUGAR,2);if(in.is(Items.BONE))return new ItemStack(Items.BONE_MEAL,5);
        String path=BuiltInRegistries.ITEM.getKey(in.getItem()).getPath();
        if(path.equals("white_wool"))return new ItemStack(Items.STRING,3);if(path.equals("white_carpet"))return new ItemStack(Items.STRING);if(in.is(Items.OAK_LOG)||in.is(Items.OAK_WOOD))return new ItemStack(Items.OAK_PLANKS,5);
        if(path.endsWith("_log")||path.endsWith("_wood")||path.endsWith("_stem")||path.endsWith("_hyphae")){String stem=path.replaceFirst("^stripped_","").replaceFirst("_(log|wood|stem|hyphae)$","");var id=net.minecraft.resources.Identifier.tryParse("minecraft:"+stem+"_planks");var plank=id==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);if(plank!=Items.AIR)return new ItemStack(plank,5);}
        if(path.endsWith("_wool")&&!path.equals("white_wool"))return new ItemStack(Items.STRING,3);if(path.endsWith("_carpet")&&!path.equals("white_carpet")||path.endsWith("_wool_slab"))return new ItemStack(Items.STRING);if(path.contains("wool")&&path.endsWith("_stairs"))return new ItemStack(Items.STRING,2);
        return ItemStack.EMPTY;
    }
    public static boolean grindFromHand(ServerPlayer player,BlockPos jukebox,net.minecraft.world.InteractionHand hand){if(!enabled("jukebox_grinder"))return false;var key=new WorldPos(player.level().dimension().identifier().toString(),jukebox.asLong());if(player.level().getGameTime()<GRIND_NEXT.getOrDefault(key,Long.MIN_VALUE))return false;var held=player.getItemInHand(hand);var result=grindResult(held);if(result.isEmpty())return false;var inventory=player.getInventory();var main=inventory.getNonEquipmentItems();var copy=new ItemStack[main.size()];for(int i=0;i<copy.length;i++)copy[i]=main.get(i).copy();int heldSlot=hand==net.minecraft.world.InteractionHand.MAIN_HAND?inventory.getSelectedSlot():Inventory.SLOT_OFFHAND;ItemStack offhandAfter=null;if(!player.getAbilities().instabuild){if(heldSlot<copy.length){copy[heldSlot].shrink(1);if(copy[heldSlot].isEmpty())copy[heldSlot]=ItemStack.EMPTY;}else{offhandAfter=inventory.getItem(heldSlot).copy();offhandAfter.shrink(1);if(offhandAfter.isEmpty())offhandAfter=ItemStack.EMPTY;}}var after=planInsertArray(copy,result);if(after==null)return false;for(int i=0;i<after.length;i++)if(!ItemStack.matches(main.get(i),after[i]))inventory.setItem(i,after[i]);if(offhandAfter!=null)inventory.setItem(heldSlot,offhandAfter);inventory.setChanged();player.inventoryMenu.broadcastChanges();GRIND_NEXT.put(key,player.level().getGameTime()+Math.max(1,Rules.i("jukebox_grinder","interval_ticks")));player.level().playSound(null,jukebox,net.minecraft.sounds.SoundEvents.GRINDSTONE_USE,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1F);return true;}
    private static ItemStack[] planInsertArray(ItemStack[] items,ItemStack source){var result=new ItemStack[items.length];for(int i=0;i<items.length;i++)result[i]=items[i].copy();int left=source.getCount();for(var current:result)if(!current.isEmpty()&&ItemStack.isSameItemSameComponents(current,source)){int move=Math.min(left,Math.max(0,current.getMaxStackSize()-current.getCount()));if(move>0){current.grow(move);left-=move;}if(left==0)return result;}for(int i=0;i<result.length&&left>0;i++)if(result[i].isEmpty()){int move=Math.min(left,source.getMaxStackSize());result[i]=source.copyWithCount(move);left-=move;}return left==0?result:null;}
    private static boolean fits(List<Container> containers,ItemStack stack){var slots=slots(containers);long capacity=0;for(var ref:slots){var current=ref.container().getItem(ref.slot());if(current.isEmpty())capacity+=stack.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(current,stack))capacity+=Math.max(0,current.getMaxStackSize()-current.getCount());if(capacity>=stack.getCount())return true;}return false;}
    private static ItemStack washed(ItemStack input,ServerLevel level){
        if(input.is(AutomationContent.item("iron_dust")))return new ItemStack(AutomationContent.item("washed_iron_dust"));if(input.is(AutomationContent.item("copper_dust")))return new ItemStack(AutomationContent.item("washed_copper_dust"));if(input.is(AutomationContent.item("gold_dust")))return new ItemStack(AutomationContent.item("washed_gold_dust"));
        if(input.is(AutomationContent.item("dust")))return level.getRandom().nextDouble()*100<Math.clamp(Rules.n("washing_cauldron","dust_mineral_chance"),0,100)?new ItemStack(List.of(Items.COAL,Items.RAW_IRON,Items.RAW_COPPER,Items.RAW_GOLD,Items.REDSTONE,Items.LAPIS_LAZULI,Items.EMERALD,Items.DIAMOND).get(level.getRandom().nextInt(8))):new ItemStack(Items.CLAY_BALL);
        if(input.is(Items.COARSE_DIRT))return new ItemStack(Items.DIRT);
        var key=BuiltInRegistries.ITEM.getKey(input.getItem());String path=key.getPath();
        if(input.has(DataComponents.DYED_COLOR)&&key.getNamespace().equals("minecraft")){
            if(path.equals("bundle")||path.endsWith("_bundle"))return whiteColorVariant(input,BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_bundle")));
            if(path.equals("shulker_box")||path.endsWith("_shulker_box"))return whiteColorVariant(input,BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_shulker_box")));
        }
        for(String suffix:List.of("_wool","_carpet","_bed","_banner","_terracotta","_glazed_terracotta","_concrete","_concrete_powder","_shulker_box","_bundle"))if(path.endsWith(suffix)){
            String color=path.substring(0,path.length()-suffix.length());if(!color.equals("white")&&Arrays.stream(net.minecraft.world.item.DyeColor.values()).anyMatch(dye->dye.getName().equals(color))){
                var id=net.minecraft.resources.Identifier.tryParse(key.getNamespace()+":white"+suffix);var item=id==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
                if(item!=Items.AIR)return whiteColorVariant(input,item);
            }
        }
        if(input.has(DataComponents.DYED_COLOR)&&knownWashableDyeComponent(path)){var clean=input.copyWithCount(1);clean.set(DataComponents.DYED_COLOR,new DyedItemColor(0xFFFFFF));return clean;}
        return ItemStack.EMPTY;
    }
    private static ItemStack whiteColorVariant(ItemStack input,net.minecraft.world.item.Item target){
        if(target==Items.AIR)return ItemStack.EMPTY;
        var clean=input.transmuteCopy(target,1);
        if(input.has(DataComponents.BUNDLE_CONTENTS))clean.set(DataComponents.BUNDLE_CONTENTS,input.get(DataComponents.BUNDLE_CONTENTS));
        if(input.has(DataComponents.CONTAINER))clean.set(DataComponents.CONTAINER,input.get(DataComponents.CONTAINER));
        if(input.has(DataComponents.BLOCK_ENTITY_DATA))clean.set(DataComponents.BLOCK_ENTITY_DATA,input.get(DataComponents.BLOCK_ENTITY_DATA));
        if(input.has(DataComponents.CUSTOM_NAME))clean.set(DataComponents.CUSTOM_NAME,input.get(DataComponents.CUSTOM_NAME));
        if(input.has(DataComponents.ITEM_NAME))clean.set(DataComponents.ITEM_NAME,input.get(DataComponents.ITEM_NAME));
        if(input.has(DataComponents.LORE))clean.set(DataComponents.LORE,input.get(DataComponents.LORE));
        if(input.has(DataComponents.ENCHANTMENTS))clean.set(DataComponents.ENCHANTMENTS,input.get(DataComponents.ENCHANTMENTS));
        if(input.has(DataComponents.STORED_ENCHANTMENTS))clean.set(DataComponents.STORED_ENCHANTMENTS,input.get(DataComponents.STORED_ENCHANTMENTS));
        if(input.has(DataComponents.DAMAGE))clean.set(DataComponents.DAMAGE,input.get(DataComponents.DAMAGE));
        clean.set(DataComponents.DYED_COLOR,new DyedItemColor(0xFFFFFF));
        return clean;
    }
    private static boolean knownWashableDyeComponent(String path){
        return path.startsWith("leather_")||path.equals("leather_horse_armor")||path.equals("wolf_armor")
                ||path.equals("mixed_wool")||path.equals("mixed_wool_slab")||path.equals("mixed_wool_stairs")
                ||path.equals("mixed_bed")||path.equals("mixed_concrete")||path.equals("mixed_shulker");
    }
    static boolean tryNativeWash(ServerLevel level,BlockPos cauldron){
        if(!enabled("washing_cauldron")||!level.hasChunkAt(cauldron)||!washableWater(level,cauldron))return false;
        var inputPos=cauldron.above();if(!level.hasChunkAt(inputPos)||!(level.getBlockEntity(inputPos) instanceof Container input))return false;
        String id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(inputPos).getBlock()).getPath();if(!id.equals("chest")&&!id.equals("barrel"))return false;
        return washOne(level,cauldron,input,level.getBlockState(cauldron));
    }
    private static boolean washableWater(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);
        if(state.is(Blocks.WATER_CAULDRON))return state.getValue(BlockStateProperties.LEVEL_CAULDRON)>0;
        if(state.getBlock() instanceof LargeCauldronBlock large)return large.isWater(level,pos)&&large.availableWater(level,pos)>0;
        return false;
    }
    private static boolean washOne(ServerLevel level,BlockPos cauldron,Container input,BlockState state){
        boolean vanilla=state.is(Blocks.WATER_CAULDRON);LargeCauldronBlock large=state.getBlock() instanceof LargeCauldronBlock candidate?candidate:null;
        int water=large!=null?large.availableWater(level,cauldron):state.getValue(BlockStateProperties.LEVEL_CAULDRON);
        if(water<=0||large!=null&&!large.isWater(level,cauldron))return false;var outputs=containersAround(level,cauldron,false);if(outputs.isEmpty())return false;
        for(int i=0;i<input.getContainerSize();i++){var source=input.getItem(i);if(source.isEmpty())continue;var result=washed(source,level);if(result.isEmpty()||!fits(outputs,result))continue;var before=source.copy();if(!consume(new SlotRef(input,i),1))return false;
            if(large!=null&&!large.consumeWater(level,cauldron,1)){input.setItem(i,before);return false;}
            if(!output(outputs,result))throw new IllegalStateException("Validated wash output no longer fits");
            if(vanilla){if(water==1)level.setBlock(cauldron,Blocks.CAULDRON.defaultBlockState(),3);else level.setBlock(cauldron,state.setValue(BlockStateProperties.LEVEL_CAULDRON,water-1),3);}
            level.playSound(null,cauldron,net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1F);return true;}return false;
    }
    private static net.minecraft.world.entity.Entity clickerEntity(ServerLevel level,BlockPos target,ServerPlayer actor){
        if(!Rules.b("auto_clicker","entity_interaction")||!level.getBlockState(target).getCollisionShape(level,target).isEmpty())return null;
        var bounds=new AABB(target);
        return level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,bounds,entity->entity!=actor&&entity.isPickable()&&entity.isAlive()&&(!(entity instanceof Player player)||!player.isSpectator()))
                .stream().min(Comparator.comparingDouble(entity->entity.distanceToSqr(Vec3.atCenterOf(actor.blockPosition())))).orElse(null);
    }
    static int attackTeslaTargets(ServerLevel level,BlockPos origin,double radius,float damage){
        var box=new AABB(origin).inflate(Math.max(0,radius));int hits=0;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,box,LivingEntity::isAlive)){
            if(target.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin))>radius*radius)continue;
            if(target instanceof Player&&!Rules.b("tesla_coil","affect_players"))continue;
            if(attackTeslaTarget(level,origin,target,damage))hits++;
        }
        if(hits>0){
        if(Rules.b("tesla_coil","attack_sound")){
            float volume=(float)Math.clamp(Rules.n("tesla_coil","sound_volume"),0,1);
            if(volume>0)level.playSound(null,origin,net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,net.minecraft.sounds.SoundSource.BLOCKS,volume,1F);
        }
        }
        return hits;
    }
    static boolean attackTeslaTarget(ServerLevel level,BlockPos origin,LivingEntity target,float damage){
        if(!target.isAlive()||!target.hurtServer(level,level.damageSources().lightningBolt(),damage))return false;
        if(Rules.b("tesla_coil","attack_effects")){
            int density=Math.clamp(Rules.i("tesla_coil","particle_density"),0,16);
            if(density>0){
                double sx=origin.getX()+.5,sy=origin.getY()+.7,sz=origin.getZ()+.5;
                double ex=target.getX(),ey=target.getY()+target.getBbHeight()*.5,ez=target.getZ();
                for(int i=0;i<density;i++){
                    double t=(i+.5)/density;
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                            sx+(ex-sx)*t,sy+(ey-sy)*t,sz+(ez-sz)*t,1,.025,.025,.025,.01);
                }
            }
        }
        return true;
    }
    private static void clicker(AutomationMachineEntity machine,ServerLevel level){if(machine.cooldown()>0){machine.cooldown(machine.cooldown()-1);if(machine.cooldown()>0)return;}boolean powered=level.hasNeighborSignal(machine.getBlockPos());if(!powered){if(machine.lastPowered())machine.lastPowered(false);return;}if(!machine.lastPowered())machine.lastPowered(true);var target=machine.getBlockPos().relative(machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));if(!level.hasChunkAt(target))return;
        var actor=CLICKER_ACTORS.computeIfAbsent(machine,ignored->AutomationMachineActor.create(level,machine));if(actor.isRemoved()){CLICKER_ACTORS.remove(machine);actor=AutomationMachineActor.create(level,machine);CLICKER_ACTORS.put(machine,actor);}
        actor.setPos(Vec3.atCenterOf(machine.getBlockPos()));actor.setYRot(machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot());
        actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,machine.getItem(0).copy());var hit=new BlockHitResult(new Vec3(target.getX()+.5,target.getY()+1.0,target.getZ()+.5),Direction.UP,target,false);
        try{
            var entity=clickerEntity(level,target,actor);
            if(entity!=null){
                var entityPos=entity.blockPosition();
                if(!level.mayInteract(actor,entityPos)||actor.blockActionRestricted(level,entityPos,net.minecraft.world.level.GameType.SURVIVAL))machine.lastClickResult("entity_action=denied_by_world_protection");
                else if(machine.leftClick()){actor.resetAttackStrengthTicker();actor.attack(entity);machine.lastClickResult("attack="+BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));}
                else machine.lastClickResult("interactOn="+actor.interactOn(entity,net.minecraft.world.InteractionHand.MAIN_HAND,Vec3.atCenterOf(target)));
            }else if(machine.leftClick()){
                var targetState=level.getBlockState(target);
                machine.lastClickResult(targetState.getDestroySpeed(level,target)<0?"destroyBlock=denied_negative_hardness":"destroyBlock="+actor.gameMode.destroyBlock(target));
            }else machine.lastClickResult("useItemOn="+actor.gameMode.useItemOn(actor,level,actor.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        }finally{
            machine.setItem(0,actor.getMainHandItem().copy());actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);actor.closeContainer();
            var inventory=actor.getInventory();for(int i=0;i<inventory.getContainerSize();i++){var extra=inventory.getItem(i);if(extra.isEmpty())continue;net.minecraft.world.level.block.Block.popResource(level,machine.getBlockPos(),extra.copy());inventory.setItem(i,ItemStack.EMPTY);}inventory.setChanged();actor.inventoryMenu.broadcastChanges();
        }
        machine.cooldown(Math.max(1,Rules.i("auto_clicker","interval_ticks")));
    }
}
