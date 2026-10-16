package com.sumirelabs.sum1rem0d.game;
/** NeoForge's tracked-entity attachment, using the same NBT field as Fabric. */
public final class NaturalFlipData {
 private NaturalFlipData() {}
 //? if neoforge {
 /*private static final net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> TYPES=net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES,"sum1rem0d");
 public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Boolean>> FLIP=TYPES.register("natural_flip",()->net.neoforged.neoforge.attachment.AttachmentType.builder(()->false).sync(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
 public static void register(net.neoforged.bus.api.IEventBus bus) {TYPES.register(bus);}
 public static boolean get(net.minecraft.world.entity.Mob mob) {return mob.getData(FLIP);}
 public static void set(net.minecraft.world.entity.Mob mob,boolean value) {mob.setData(FLIP,value);}
 *///?}
}
