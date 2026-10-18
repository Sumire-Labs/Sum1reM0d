package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
//? if neoforge {
/*import net.neoforged.neoforge.transfer.transaction.TransactionContext;
*///?}

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceMixin extends BaseContainerBlockEntity implements InstantFurnace {
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow private int litTimeRemaining, litTotalTime, cookingTimer, cookingTotalTime;
    @Shadow public abstract void setRecipeUsed(RecipeHolder<?> recipe);
    @Unique private double sum1rem0d$credit;
    @Unique private boolean sum1rem0d$preview, sum1rem0d$editing, sum1rem0d$deferRefresh, sum1rem0d$breaking;
    @Unique private ItemStack sum1rem0d$input = ItemStack.EMPTY;
    @Unique private RecipeHolder<?> sum1rem0d$recipe, sum1rem0d$pendingRecipe;
    @Unique private int sum1rem0d$resultCount;
    @Unique private long sum1rem0d$operationTick = Long.MIN_VALUE;
    @Unique private int sum1rem0d$operations;
    //? if neoforge {
    /*@Unique private final NeoFurnaceTransactions sum1rem0d$transactions = new NeoFurnaceTransactions(this);
    *///?}
    protected FurnaceMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Unique private RecipeType<? extends AbstractCookingRecipe> sum1rem0d$type() {
        return (Object) this instanceof BlastFurnaceBlockEntity ? RecipeType.BLASTING : (Object) this instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
    }

    @Unique private void sum1rem0d$discard() {
        if (sum1rem0d$preview) items.set(2, ItemStack.EMPTY);
        sum1rem0d$preview = false;
    }
    @Unique private double sum1rem0d$cost() { return Rules.n("instant_smelting", "base_cost") * WorldState.get(((ServerLevel) level).getServer()).fuelMultiplier(); }
    @Unique private boolean sum1rem0d$canPay() {
        long tick = level.getGameTime();
        if (tick != sum1rem0d$operationTick) { sum1rem0d$operationTick = tick; sum1rem0d$operations = 0; }
        if (sum1rem0d$operations >= Rules.i("instant_smelting", "batch_per_tick")) return false;
        int required = SmeltingRules.requiredFuel(sum1rem0d$credit, sum1rem0d$cost());
        return required == 0 || Rules.matches(items.get(1), Rules.list("instant_smelting", "fuel_items")) && items.get(1).getCount() >= required;
    }
    @Override public void sum1rem0d$refresh() {
        if (!(level instanceof ServerLevel server) || sum1rem0d$editing || sum1rem0d$breaking) return;
        if (!Rules.on("instant_smelting")) { sum1rem0d$discard(); return; }
        if (sum1rem0d$preview && (!ItemStack.isSameItemSameComponents(items.get(0), sum1rem0d$input) || items.get(0).isEmpty() || !sum1rem0d$canPay() || !SmeltingRules.allowed(server, sum1rem0d$type(), items.get(0)))) sum1rem0d$discard();
        if (sum1rem0d$preview || !items.get(2).isEmpty() || sum1rem0d$deferRefresh) return;
        var recipe = SmeltingRules.recipe(server, sum1rem0d$type(), items.get(0));
        if (recipe == null || !sum1rem0d$canPay()) return;
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(items.get(0)));
        if (result.isEmpty()) return;
        sum1rem0d$recipe = recipe; sum1rem0d$input = items.get(0).copy(); sum1rem0d$resultCount = result.getCount();
        items.set(2, result); sum1rem0d$preview = true;
    }
    @Override public boolean sum1rem0d$isPreview() { return sum1rem0d$preview; }
    @Override public boolean sum1rem0d$consume(boolean transactional) {
        if (!sum1rem0d$preview) return true;
        if (!(level instanceof ServerLevel) || !Rules.on("instant_smelting") || items.get(0).isEmpty() || !ItemStack.isSameItemSameComponents(items.get(0), sum1rem0d$input) || !sum1rem0d$canPay()) return false;
        double cost = sum1rem0d$cost(); int paid = SmeltingRules.requiredFuel(sum1rem0d$credit, cost);
        sum1rem0d$editing = true;
        try {
            items.get(0).shrink(1); items.get(1).shrink(paid);
            sum1rem0d$credit = SmeltingRules.remainingCredit(sum1rem0d$credit, cost, paid);
            sum1rem0d$preview = false; sum1rem0d$pendingRecipe = sum1rem0d$recipe;
            sum1rem0d$operations++;
            if (!transactional) sum1rem0d$finishCommit();
        } finally { sum1rem0d$editing = false; }
        return true;
    }
    @Override public CostSnapshot sum1rem0d$snapshotCosts() { return new CostSnapshot(items.get(0).copy(), items.get(1).copy(), sum1rem0d$credit, sum1rem0d$preview, sum1rem0d$pendingRecipe, sum1rem0d$operationTick, sum1rem0d$operations); }
    @Override public void sum1rem0d$restoreCosts(CostSnapshot snapshot) {
        items.set(0, snapshot.input()); items.set(1, snapshot.fuel()); sum1rem0d$credit = snapshot.credit(); sum1rem0d$preview = snapshot.preview(); sum1rem0d$pendingRecipe = snapshot.pending();
        sum1rem0d$operationTick = snapshot.tick(); sum1rem0d$operations = snapshot.operations();
    }
    @Override public void sum1rem0d$finishCommit() { if (sum1rem0d$pendingRecipe != null) { setRecipeUsed(sum1rem0d$pendingRecipe); sum1rem0d$pendingRecipe = null; } super.setChanged(); }
    @Override public ItemStack getItem(int slot) { if (slot == 2) sum1rem0d$refresh(); return super.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot == 2) { sum1rem0d$refresh(); if (sum1rem0d$preview && !sum1rem0d$consume(false)) { sum1rem0d$discard(); return ItemStack.EMPTY; } }
        else if (!sum1rem0d$editing) sum1rem0d$discard();
        return super.removeItem(slot, amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 2) { sum1rem0d$refresh(); if (sum1rem0d$preview && !sum1rem0d$consume(false)) { sum1rem0d$discard(); return ItemStack.EMPTY; } }
        else sum1rem0d$discard();
        return super.removeItemNoUpdate(slot);
    }
    @Override public void setChanged() {
        if (!sum1rem0d$editing && !sum1rem0d$deferRefresh && sum1rem0d$preview && items.get(2).getCount() < sum1rem0d$resultCount) sum1rem0d$consume(false);
        super.setChanged();
    }
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void sum1rem0d$tick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        var self = (FurnaceMixin) (Object) entity; self.sum1rem0d$deferRefresh = false;
        if (Rules.on("instant_smelting")) {
            self.sum1rem0d$refresh(); self.litTimeRemaining = 0; self.litTotalTime = 0; self.cookingTimer = 0; self.cookingTotalTime = 1;
            if (state.getValue(AbstractFurnaceBlock.LIT)) level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, false), 2);
            ci.cancel();
        } else {
            self.sum1rem0d$discard();
            if (!SmeltingRules.allowed(level, self.sum1rem0d$type(), self.items.get(0))) { self.cookingTimer = 0; ci.cancel(); }
        }
    }
    @Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void sum1rem0d$set(int slot, ItemStack stack, CallbackInfo ci) {
        if (sum1rem0d$editing || !(level instanceof ServerLevel)) return;
        if (slot == 2 && sum1rem0d$preview && stack.getCount() < sum1rem0d$resultCount) sum1rem0d$consume(false);
        else if (slot != 2 && !sum1rem0d$deferRefresh) sum1rem0d$discard();
    }
    //? if neoforge {
    /*@Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;Z)V", at = @At("HEAD"))
    private void sum1rem0d$transactionSet(int slot, ItemStack stack, boolean insideTransaction, CallbackInfo ci) {
        if (insideTransaction) sum1rem0d$deferRefresh = true;
        else if (!sum1rem0d$editing && slot == 2 && sum1rem0d$preview && stack.getCount() < sum1rem0d$resultCount) sum1rem0d$consume(false);
    }
    @Inject(method = "onTransfer", at = @At("HEAD"))
    private void sum1rem0d$transfer(int slot, int amount, TransactionContext transaction, CallbackInfo ci) {
        if (Rules.on("instant_smelting") && slot == 2 && amount < 0) sum1rem0d$transactions.extraction(transaction);
    }
    *///?}
    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$duration(CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ServerLevel serverLevel && Rules.on("daily_fuel") && !Rules.on("instant_smelting")) cir.setReturnValue(Math.max(0, (int) Math.floor(cir.getReturnValueI() / WorldState.get(serverLevel.getServer()).fuelMultiplier())));
    }
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$fuel(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) { if (Rules.on("instant_smelting") && slot == 1) cir.setReturnValue(Rules.matches(stack, Rules.list("instant_smelting", "fuel_items"))); }
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void sum1rem0d$break(BlockPos pos, BlockState state, CallbackInfo ci) { sum1rem0d$breaking = true; sum1rem0d$discard(); }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void sum1rem0d$save(ValueOutput output, CallbackInfo ci) { output.putDouble("sum1rem0d_fuel_credit", sum1rem0d$credit); output.putBoolean("sum1rem0d_preview", sum1rem0d$preview); }
    @Redirect(method="saveAdditional",at=@At(value="INVOKE",target="Lnet/minecraft/world/ContainerHelper;saveAllItems(Lnet/minecraft/world/level/storage/ValueOutput;Lnet/minecraft/core/NonNullList;)V"))
    private void sum1rem0d$saveLargeFuel(ValueOutput output,NonNullList<ItemStack> original){
        var copy=NonNullList.withSize(original.size(),ItemStack.EMPTY);for(int i=0;i<original.size();i++)copy.set(i,original.get(i).copy());
        var fuel=copy.get(1);output.putInt("sum1rem0d_fuel_count",fuel.getCount());fuel.setCount(Math.min(fuel.getCount(),fuel.getMaxStackSize()));net.minecraft.world.ContainerHelper.saveAllItems(output,copy);
    }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void sum1rem0d$load(ValueInput input, CallbackInfo ci) {
        double credit = input.getDoubleOr("sum1rem0d_fuel_credit", 0); sum1rem0d$credit = Double.isFinite(credit) ? Math.max(0, Math.min(0.999999999, credit)) : 0;
        if (input.getBooleanOr("sum1rem0d_preview", false)) items.set(2, ItemStack.EMPTY);
        if(!items.get(1).isEmpty()){int count=input.getIntOr("sum1rem0d_fuel_count",items.get(1).getCount());if(count>=items.get(1).getCount()&&count<=1000000)items.get(1).setCount(count);}
        sum1rem0d$preview = false;
    }
}
