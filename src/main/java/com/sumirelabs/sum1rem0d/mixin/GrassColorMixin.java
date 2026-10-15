package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.ModContent;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BlockColors.class)
public abstract class GrassColorMixin {
    @Inject(method = "getTintSources", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$grass(BlockState state, CallbackInfoReturnable<List<BlockTintSource>> cir) {
        if (Sum1reM0d.initialized() && state.getBlock() == ModContent.upsideDownGrass()) cir.setReturnValue(((BlockColors) (Object) this).getTintSources(Blocks.GRASS_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY, state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY))));
    }
    @Inject(method = "getTintSource", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$grassTint(BlockState state, int tint, CallbackInfoReturnable<BlockTintSource> cir) {
        if (Sum1reM0d.initialized() && state.getBlock() == ModContent.upsideDownGrass()) cir.setReturnValue(((BlockColors) (Object) this).getTintSource(Blocks.GRASS_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY, state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SNOWY)), tint));
    }
}
