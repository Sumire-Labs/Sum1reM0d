package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
public final class NetherNames {
 private NetherNames() {}
 public static Component name(Identifier id) {
  if(!ClientGameState.daily("ardite_disguise") || !id.getNamespace().equals("minecraft")) return null;
  String path=id.getPath();
  if(path.equals("ancient_debris")) return Component.translatable("sum1rem0d.disguise.ardite_ore");
  if(path.equals("netherite_scrap")) return Component.translatable("sum1rem0d.disguise.ardite_ingot");
  if(path.startsWith("netherite_")) return Component.translatable("sum1rem0d.disguise.manyullyn_"+path.substring(10));
  return null;
 }
}
