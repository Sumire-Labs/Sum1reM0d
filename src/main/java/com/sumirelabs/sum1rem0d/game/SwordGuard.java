package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative guard with native item-use animation; no permanent stack mutation. */
public final class SwordGuard {
 private SwordGuard() {}
 public static int rank(ItemStack stack) {
  if(stack.isEmpty()) return 0;
  for(var e:stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).entrySet()) if(e.getKey().is(Enchantments.PROTECTION)) return e.getIntValue();
  return 0;
 }
 public static boolean weapon(ItemStack stack) {return Rules.on("sword_guard") && Rules.matches(stack,Rules.list("sword_guard","target_items"));}
 public static boolean canGuard(ItemStack stack) {return weapon(stack) && stack.is(ItemTags.SWORDS) && rank(stack)>0;}
 public static boolean allowed(DamageSource source) {
  for(String entry:Rules.list("sword_guard","damage_types")) {
   boolean tag=entry.startsWith("#");var id=Rules.id(tag?entry.substring(1):entry);
   if(id!=null && (tag?source.is(TagKey.create(Registries.DAMAGE_TYPE,id)):source.is(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE,id)))) return true;
  }
  return false;
 }
 public static float damage(LivingEntity victim,ServerLevel level,DamageSource source,float damage) {
  if(!Rules.on("sword_guard") || damage<=0) return damage;
  var weapon=source.getWeaponItem();
  if(weapon!=null && weapon(weapon) && !source.is(DamageTypes.THORNS)) damage *= (float)Math.max(0,1-rank(weapon)*Rules.n("sword_guard","attack_loss_per_level")/100);
  if(!allowed(source)) return damage;
  if(!victim.isUsingItem() || !canGuard(victim.getUseItem()) || victim.getTicksUsingItem()<Rules.i("sword_guard","delay_ticks") || source.is(DamageTypeTags.BYPASSES_SHIELD)) return damage;
  if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow && arrow.getPierceLevel()>0) return damage;
  Vec3 origin=source.getSourcePosition();if(origin==null) return damage;
  Vec3 toward=origin.subtract(victim.position()).multiply(1,0,1).normalize();
  Vec3 facing=Vec3.directionFromRotation(0,victim.getYRot());
  if(toward.lengthSqr()==0 || facing.dot(toward)<Math.cos(Math.toRadians(Rules.n("sword_guard","angle")/2))-1e-7) return damage;
  var stack=victim.getUseItem();float fraction=(float)Math.min(Rules.n("sword_guard","maximum_guard"),rank(stack)*Rules.n("sword_guard","guard_per_level"))/100;
  float blocked=damage*fraction;if(blocked<=0) return damage;
  int cost=(int)Math.ceil(blocked*Rules.n("sword_guard","durability_per_damage"));
  if(cost>0) {stack.hurtAndBreak(cost,victim,victim.getUsedItemHand().asEquipmentSlot());if(stack.isEmpty())victim.stopUsingItem();}
  level.playSound(null,victim.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(),victim.getSoundSource(),0.8f,1.2f);
  return Math.max(0,damage-blocked);
 }
}
