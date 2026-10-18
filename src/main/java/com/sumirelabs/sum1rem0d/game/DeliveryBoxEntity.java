package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.mojang.serialization.JsonOps;

/** Owner and ACL live in the block entity; the durable mailbox container shares the order ledger. */
public final class DeliveryBoxEntity extends BlockEntity implements Container {
    private UUID owner;
    private final Set<UUID> permitted = new LinkedHashSet<>();

    public DeliveryBoxEntity(BlockPos pos, BlockState state) { super(DeliveryBoxContent.type(), pos, state); }

    public UUID owner() { return owner; }
    public Set<UUID> permitted() { return Set.copyOf(permitted); }
    public boolean mayAccess(Player player) { return owner == null || player.getUUID().equals(owner) || permitted.contains(player.getUUID()); }
    public boolean setOwner(UUID actor, UUID nextOwner) {
        if (owner != null && !owner.equals(actor)) return false;
        if (owner == null && !actor.equals(nextOwner)) return false;
        owner = Objects.requireNonNull(nextOwner);
        permitted.remove(owner);
        setChanged();
        if (level instanceof ServerLevel server) {
            var data = WorldState.get(server.getServer()).player(owner);
            data.deliveryBoxDimension = server.dimension().identifier().toString();
            data.deliveryBoxPos = worldPosition.asLong();
            WorldState.get(server.getServer()).save();
        }
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
    public boolean permit(UUID actor, UUID target, boolean allow) {
        if (owner == null || !owner.equals(actor) || target.equals(owner)) return false;
        if (allow) permitted.add(target); else permitted.remove(target);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
    public static void placed(Level level, BlockPos pos, LivingEntity placer) {
        if (level.isClientSide() || !(placer instanceof Player player)) return;
        if (level.getBlockEntity(pos) instanceof DeliveryBoxEntity box) box.setOwner(player.getUUID(), player.getUUID());
    }
    public boolean registeredDeliveryTarget(UUID player) {
        if (owner == null || !owner.equals(player) || !(level instanceof ServerLevel server)) return false;
        var data = WorldState.get(server.getServer()).player(player);
        return data.deliveryBoxDimension.equals(server.dimension().identifier().toString()) && data.deliveryBoxPos == worldPosition.asLong();
    }
    public boolean canDeliver(UUID player) { return owner != null && owner.equals(player) && level instanceof ServerLevel; }

    private JsonArray contents() {
        if (owner == null || !(level instanceof ServerLevel server)) return emptyContents();
        return WorldState.get(server.getServer()).deliveryItems(owner);
    }
    private static JsonArray emptyContents() { var a = new JsonArray(); for (int i=0;i<27;i++) a.add(JsonNull.INSTANCE); return a; }
    private ItemStack read(int slot) {
        if (slot < 0 || slot >= 27) return ItemStack.EMPTY;
        var value = contents().get(slot);
        if (value == null || value.isJsonNull()) return ItemStack.EMPTY;
        if (!(level instanceof ServerLevel server)) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC.parse(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), value).getOrThrow();
    }
    private JsonElement write(ItemStack stack) {
        if (stack.isEmpty()) return JsonNull.INSTANCE;
        if (!(level instanceof ServerLevel server)) throw new IllegalStateException("Delivery box is not on the server");
        return ItemStack.OPTIONAL_CODEC.encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
    }
    private void saveContents(JsonArray updated) {
        if (owner == null || !(level instanceof ServerLevel server)) return;
        var world = WorldState.get(server.getServer()); var before = world.checkpoint();
        try { world.player(owner).deliveryItems = updated; world.saveStrict(); setChanged(); }
        catch (RuntimeException failure) { world.restore(before); throw failure; }
    }

    /** Add as many ordered items as fit. Caller saves mailbox and order remainder in one world-state commit. */
    int insertDelivered(ItemStack incoming) {
        if (owner == null || incoming.isEmpty()) return 0;
        var updated = contents().deepCopy(); int left = incoming.getCount();
        for (int i=0;i<27 && left>0;i++) {
            var current = read(i);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, incoming)) continue;
            int move = Math.min(left, Math.max(0, current.getMaxStackSize()-current.getCount()));
            if (move > 0) { current.grow(move); updated.set(i, write(current)); left -= move; }
        }
        for (int i=0;i<27 && left>0;i++) if (updated.get(i).isJsonNull()) {
            int move = Math.min(left, incoming.getMaxStackSize());
            updated.set(i, write(incoming.copyWithCount(move))); left -= move;
        }
        int inserted = incoming.getCount()-left;
        if (inserted > 0) { if (!(level instanceof ServerLevel server)) return 0; WorldState.get(server.getServer()).player(owner).deliveryItems = updated; setChanged(); }
        return inserted;
    }
    void commitMailbox() { if (level instanceof ServerLevel server) WorldState.get(server.getServer()).saveStrict(); }
    ItemStack takeForPlayer(int slot) {
        var current = read(slot); if (current.isEmpty()) return ItemStack.EMPTY;
        var updated = contents().deepCopy(); updated.set(slot, JsonNull.INSTANCE);
        if (level instanceof ServerLevel server) WorldState.get(server.getServer()).player(owner).deliveryItems = updated;
        return current;
    }

    @Override public int getContainerSize() { return 27; }
    @Override public boolean isEmpty() { for (int i=0;i<27;i++) if (!getItem(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return read(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        var current = read(slot); if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        var taken = current.split(amount); var updated = contents().deepCopy(); updated.set(slot, write(current)); saveContents(updated); return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, Integer.MAX_VALUE); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= 27 || owner == null) return;
        var updated = contents().deepCopy(); updated.set(slot, write(stack)); saveContents(updated);
    }
    @Override public void setChanged() { super.setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && mayAccess(player); }
    @Override public void clearContent() { saveContents(emptyContents()); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.storeNullable("owner", UUIDUtil.CODEC, owner);
        out.store("permitted", UUIDUtil.CODEC.listOf(), List.copyOf(permitted));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        permitted.clear();
        permitted.addAll(in.read("permitted", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        if (owner != null) permitted.remove(owner);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
}
