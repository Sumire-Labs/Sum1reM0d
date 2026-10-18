package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
public final class NetherNames {
 private NetherNames() {}
 public static Component name(Identifier id) {
  if(ClientGameState.received()&&com.sumirelabs.sum1rem0d.config.ClientRules.enabled("classic_dye_names")&&id.getNamespace().equals("minecraft")){
   if(java.util.Set.of("yellow_dye","red_dye","green_dye").contains(id.getPath()))return Component.translatable("sum1rem0d.classic."+id.getPath());
  }
  if(!ClientGameState.received() || !com.sumirelabs.sum1rem0d.config.ClientRules.enabled("ardite_disguise"))return null;
  if(id.equals(com.sumirelabs.sum1rem0d.game.ModContent.id("netherite_paxel")))return Component.translatable("sum1rem0d.disguise.manyullyn_paxel");
  if(!id.getNamespace().equals("minecraft")) return null;
  String path=id.getPath();
  if(path.equals("ancient_debris")) return Component.translatable("sum1rem0d.disguise.ardite_ore");
  if(path.equals("netherite_scrap")) return Component.translatable("sum1rem0d.disguise.ardite_ingot");
  if(path.startsWith("netherite_")) return Component.translatable("sum1rem0d.disguise.manyullyn_"+path.substring(10));
  return null;
 }
}
