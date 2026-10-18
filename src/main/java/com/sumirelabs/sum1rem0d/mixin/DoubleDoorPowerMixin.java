package com.sumirelabs.sum1rem0d.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sumirelabs.sum1rem0d.game.DoubleDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(DoorBlock.class)
public abstract class DoubleDoorPowerMixin {
    @WrapOperation(method="neighborChanged", at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean sum1rem0d$pairedPower(Level level, BlockPos pos, Operation<Boolean> original) {
        return original.call(level, pos) || DoubleDoors.partnerPowered(level, pos);
    }
}
