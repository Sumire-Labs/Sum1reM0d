package com.sumirelabs.sum1rem0d.mixin;

import java.util.List;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface CoinMenuDataAccess {
  @Accessor("dataSlots")
  List<DataSlot> sum1rem0d$dataSlots();
}
