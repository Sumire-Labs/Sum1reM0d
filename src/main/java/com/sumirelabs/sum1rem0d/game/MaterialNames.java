package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
public final class MaterialNames {
 private MaterialNames() {}
 public static Component name(Identifier id) {
  if(!Rules.on("ardite_disguise") || !id.getNamespace().equals("minecraft")) return null;
  String path=id.getPath();
  if(path.equals("ancient_debris")) return Component.translatable("sum1rem0d.disguise.ardite_ore");
  if(path.equals("netherite_scrap")) return Component.translatable("sum1rem0d.disguise.ardite_ingot");
  if(path.startsWith("netherite_")) return Component.translatable("sum1rem0d.disguise.manyullyn_"+path.substring(10));
  return null;
 }
}
