package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
/** Fixed artwork for the optional material disguise; resource packs may replace the mod's PNGs. */
public final class DedicatedTextures {
 private DedicatedTextures(){}
 public static Resource material(Identifier id,Resource original,ResourceManager manager){
  String path=id.getPath(),target=null;
  if(path.equals("textures/block/ancient_debris_top.png") || path.equals("textures/block/ancient_debris_side.png"))target="textures/block/ardite_ore.png";
  else if(path.equals("textures/block/netherite_block.png"))target="textures/block/manyullyn_block.png";
  else if(path.equals("textures/item/netherite_scrap.png"))target="textures/item/ardite_ingot.png";
  else if(path.startsWith("textures/item/netherite_") && path.endsWith(".png"))target=path.replace("/netherite_","/manyullyn_");
  else if(path.startsWith("textures/entity/equipment/") && path.endsWith("/netherite.png"))target=path.replace("/netherite.png","/manyullyn.png");
  if(target==null)return original;
  return manager.getResource(ModContent.id(target)).orElse(original);
 }
}
