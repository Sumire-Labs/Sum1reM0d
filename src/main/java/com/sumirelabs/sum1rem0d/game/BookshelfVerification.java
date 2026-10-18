package com.sumirelabs.sum1rem0d.game;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
final class BookshelfVerification {
    static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var saved=config.snapshot();var draft=saved.deepCopy();var features=draft.getAsJsonObject("features");
        for(var entry:features.entrySet())entry.getValue().getAsJsonObject().addProperty("enabled",false);
        var options=features.getAsJsonObject("compressed_bookshelves");options.addProperty("enabled",true);options.addProperty("expanded_levels",false);options.addProperty("power_multiplier",1);options.addProperty("max_tier",8);options.addProperty("maximum_level",120);config.update(draft,config.revision());
        var level=server.overworld();var actor=EconomyVerification.actor(server,level,"BookshelfFixture");
        try{
            var recipe=new ExtraCraftingRecipe("compressed_bookshelves");
            for(int tier=0;tier<8;tier++){
                var input=CraftingInput.of(3,3,Collections.nCopies(9,new ItemStack(BookshelfContent.item(tier))));var result=recipe.assemble(input);
                require(result.is(BookshelfContent.item(tier+1))&&result.getCount()==1,"Nine equal shelves compress at tier "+tier);
                var unpack=recipe.assemble(CraftingInput.of(1,1,List.of(result)));require(unpack.is(BookshelfContent.item(tier))&&unpack.getCount()==9,"Compression unpacks without item loss");
            }
            require(BookshelfContent.units(8)==43046721L&&recipe.assemble(CraftingInput.of(3,3,Collections.nCopies(9,new ItemStack(BookshelfContent.item(8))))).isEmpty(),"Eighth tier caps compression with exact unit count");
            var mixed=new ArrayList<ItemStack>(Collections.nCopies(9,new ItemStack(Items.BOOKSHELF)));mixed.set(0,new ItemStack(Items.CHISELED_BOOKSHELF));require(recipe.assemble(CraftingInput.of(3,3,mixed)).isEmpty(),"Chiseled and mixed shelves are not compressed");
            passed.accept("All eight 9x compression/unpacking stages preserve exact quantities and reject mixed shelves");
            var table=new BlockPos(3440,245,40);ServerVerification.place(level,table,Blocks.ENCHANTING_TABLE.defaultBlockState());
            var offsets=EnchantingTableBlock.BOOKSHELF_OFFSETS;for(var offset:offsets)ServerVerification.place(level,table.offset(offset),Blocks.AIR.defaultBlockState());
            for(int i=0;i<9;i++)ServerVerification.place(level,table.offset(offsets.get(i)),Blocks.BOOKSHELF.defaultBlockState());
            int[] normal=costs(actor,level,table);
            for(int i=0;i<9;i++)level.setBlock(table.offset(offsets.get(i)),Blocks.AIR.defaultBlockState(),3);
            var shelf=table.offset(offsets.getFirst());level.setBlock(shelf,BookshelfContent.block(1).defaultBlockState(),3);
            require(Arrays.equals(normal,costs(actor,level,table)),"One 9x bookshelf produces identical native menu costs to nine shelves");
            level.setBlock(shelf,BookshelfContent.block(2).defaultBlockState(),3);
            require(costs(actor,level,table)[2]==30,"81x shelf still respects default level 30 limit");
            passed.accept("Native table treats 9x shelf as nine shelves and retains the default level-30 cap");
            var offset=offsets.getFirst();var gap=table.offset(offset.getX()/2,offset.getY(),offset.getZ()/2);ServerVerification.place(level,gap,Blocks.STONE.defaultBlockState());
            require(!EnchantingTableBlock.isValidBookShelf(level,table,offset)&&costs(actor,level,table)[2]<30,"Blocked air gap prevents compressed shelf power");
            level.setBlock(gap,Blocks.AIR.defaultBlockState(),3);
            var loot=Block.getDrops(level.getBlockState(shelf),level,shelf,null,actor,ItemStack.EMPTY);require(loot.size()==1&&loot.getFirst().is(BookshelfContent.item(2)),"Breaking compressed bookshelf retains its tier");
            passed.accept("Vanilla air-gap obstruction and tier-preserving bookshelf drops work");
            options.addProperty("expanded_levels",true);config.update(draft,config.revision());
            var menu=new EnchantmentMenu(3600,actor.getInventory(),ContainerLevelAccess.create(level,table));menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));
            require(menu.costs[2]==120&&menu.enchantClue[2]>=0,"Expanded mode has level-120 requirement and usable candidates");
            actor.experienceLevel=1000;
            require(menu.clickMenuButton(actor,2)&&menu.getSlot(0).getItem().isEnchanted(),"Native high-level option actually enchants the item");
            menu.removed(actor);
            passed.accept("Optional high-level mode produces usable native enchanting results with bounded costs");
            options.addProperty("expanded_levels",false);options.addProperty("enabled",false);config.update(draft,config.revision());
            require(recipe.assemble(CraftingInput.of(3,3,Collections.nCopies(9,new ItemStack(Items.BOOKSHELF)))).isEmpty()&&BookshelfContent.power(2)==1,"OFF prevents new compression and restores one-shelf power");
            int previous=256;for(int i=1;i<=8;i++){int brightness=BookshelfContent.color(i)&255;require(brightness<previous&&brightness>0,"Every compression tier becomes darker without losing its texture");previous=brightness;}
            passed.accept("Config OFF preserves existing blocks; all eight default brightness levels decrease monotonically");
        }finally{actor.discard();config.update(saved,config.revision());}
    }
    private static int[] costs(net.minecraft.server.level.ServerPlayer actor,net.minecraft.server.level.ServerLevel level,BlockPos pos){
        var menu=new EnchantmentMenu(3599,actor.getInventory(),ContainerLevelAccess.create(level,pos));menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));int[] costs=menu.costs.clone();menu.removed(actor);return costs;
    }
}
