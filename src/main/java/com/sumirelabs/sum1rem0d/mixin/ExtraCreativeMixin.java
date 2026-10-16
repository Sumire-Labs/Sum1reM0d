package com.sumirelabs.sum1rem0d.mixin;
import java.util.*;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CreativeModeTab.class)
public abstract class ExtraCreativeMixin {
 @Shadow private Collection<ItemStack> displayItems;@Shadow private Set<ItemStack> displayItemsSearchTab;
 @Inject(method="buildContents",at=@At("TAIL"))private void sum1rem0d$entries(CallbackInfo ci){
  var key=BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab)(Object)this);if(key==null)return;String tab=key.getPath();var added=new ArrayList<ItemStack>();
  if(tab.equals("tools_and_utilities"))added.add(new ItemStack(ExtraContent.item("tamed_book")));
  if(tab.equals("redstone_blocks"))added.add(new ItemStack(ExtraContent.item("piston_base")));
  if(tab.equals("colored_blocks"))for(var color:DyeColor.values())added.add(DyeTextiles.stack("mixed_wool",color.getTextureDiffuseColor()));
  if(tab.equals("functional_blocks"))for(var color:DyeColor.values())added.add(DyeTextiles.stack("mixed_bed",color.getTextureDiffuseColor()));
  for(var name:ExtraEquipment.ITEMS){boolean armor=ExtraEquipment.isArmor(name);boolean combat=armor || name.endsWith("_sword") || name.endsWith("_axe");if(tab.equals("combat") && combat || tab.equals("tools_and_utilities") && !armor && !name.endsWith("_sword"))added.add(new ItemStack(ExtraContent.item(name)));}
  if(added.isEmpty())return;var visible=ItemStackLinkedSet.createTypeAndComponentsSet();visible.addAll(displayItems);visible.addAll(added);displayItems=visible;var search=ItemStackLinkedSet.createTypeAndComponentsSet();search.addAll(displayItemsSearchTab);search.addAll(added);displayItemsSearchTab=search;
 }
}
