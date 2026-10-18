package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ModContent;
import com.sumirelabs.sum1rem0d.game.PaxelContent;
import com.sumirelabs.sum1rem0d.game.Rules;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds configured Netherite bonuses to the same modifier stream used by equipment and tooltips. */
@Mixin(ItemStack.class)
public abstract class NetheriteAttributeModifiersMixin {
    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void sum1rem0d$netheriteBonuses(EquipmentSlot slot,
                                             BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
                                             CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        sum1rem0d$addBonuses(slot.getName(), slot == EquipmentSlot.MAINHAND, isArmorForSlot(path, slot),
                (attribute, modifier) -> consumer.accept(attribute, modifier));
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("TAIL"))
    private void sum1rem0d$netheriteTooltipBonuses(EquipmentSlotGroup group,
                                                   TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer,
                                                   CallbackInfo ci) {
        boolean attackGroup = group == EquipmentSlotGroup.MAINHAND;
        String armorSlot = armorSlot(group);
        sum1rem0d$addBonuses(group.getSerializedName(), attackGroup, armorSlot != null,
                (attribute, modifier) -> consumer.accept(attribute, modifier, ItemAttributeModifiers.Display.attributeModifiers()));
    }

    private void sum1rem0d$addBonuses(String slotName, boolean attackGroup, boolean armorGroup,
                                      BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        if (!Rules.on("netherite_upgrade")) return;
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty()) return;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = key.getPath();
        boolean vanillaNetherite = key.getNamespace().equals("minecraft") && path.startsWith("netherite_");
        boolean netheritePaxel = key.getNamespace().equals("sum1rem0d") && PaxelContent.ITEMS.contains(path)
                && path.equals("netherite_paxel");
        if (!vanillaNetherite && !netheritePaxel) return;

        if (attackGroup && hasAttackModifier(stack)) {
            double bonus = Math.max(0, Rules.n("netherite_upgrade", "attack_bonus"));
            if (bonus > 0) consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    ModContent.id("netherite_attack_bonus_" + slotName), bonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (armorGroup && isArmorPath(path)) {
            double bonus = Math.max(0, Rules.n("netherite_upgrade", "armor_toughness_bonus"));
            if (bonus > 0) consumer.accept(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                    ModContent.id("netherite_toughness_" + slotName), bonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static boolean hasAttackModifier(ItemStack stack) {
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return modifiers != null && modifiers.modifiers().stream().anyMatch(entry ->
                entry.attribute().equals(Attributes.ATTACK_DAMAGE));
    }

    private static boolean isArmorForSlot(String path, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> path.startsWith("netherite_") && path.endsWith("_helmet");
            case CHEST -> path.startsWith("netherite_") && path.endsWith("_chestplate");
            case LEGS -> path.startsWith("netherite_") && path.endsWith("_leggings");
            case FEET -> path.startsWith("netherite_") && path.endsWith("_boots");
            default -> false;
        };
    }

    private static String armorSlot(EquipmentSlotGroup group) {
        return switch (group) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            default -> null;
        };
    }

    private static boolean isArmorPath(String path) {
        return path.startsWith("netherite_") && (path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots"));
    }
}
