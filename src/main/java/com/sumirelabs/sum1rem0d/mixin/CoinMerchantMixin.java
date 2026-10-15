package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import java.util.Optional;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Convert denomination rather than overflowing the vanilla 64-item trade cost. */
@Mixin(MerchantOffer.class)
public abstract class CoinMerchantMixin implements CoinMerchantConversion {
    @Unique private boolean sum1rem0d$prepared;
    @Shadow @Final @Mutable private ItemCost baseCostA;
    @Shadow @Final @Mutable private Optional<ItemCost> costB;
    @Shadow @Final @Mutable private ItemStack result;
    @Unique private boolean sum1rem0d$updating;
    @Unique private ItemCost sum1rem0d$cost(ItemCost cost,boolean on) {
        Item source=on?Items.EMERALD:ModContent.coinRoll(),target=on?ModContent.coinRoll():Items.EMERALD;
        return cost.item().value()==source?new ItemCost(target.builtInRegistryHolder(),cost.count(),cost.components()):cost;
    }
    @Inject(method={"getBaseCostA","getCostA","getCostB","getItemCostA","getItemCostB","getResult","assemble","satisfiedBy","take"},at=@At("HEAD"))
    private void sum1rem0d$currency(CallbackInfoReturnable<?> cir) {
        if(!WorldState.serverThread() || sum1rem0d$prepared || sum1rem0d$updating || !com.sumirelabs.sum1rem0d.Sum1reM0d.initialized())return;
        sum1rem0d$prepareCurrency(Rules.on("economy")&&Rules.on("coin_trading"));
    }
    public void sum1rem0d$prepareCurrency(boolean on) {
        sum1rem0d$updating=true;
        try {
            sum1rem0d$prepared=true;
            baseCostA=sum1rem0d$cost(baseCostA,on);costB=costB.map(c->sum1rem0d$cost(c,on));
            Item source=on?Items.EMERALD:ModContent.coinRoll(),target=on?ModContent.coinRoll():Items.EMERALD;
            if(result.is(source))result=result.transmuteCopy(target);
        } finally {sum1rem0d$updating=false;}
    }
}
