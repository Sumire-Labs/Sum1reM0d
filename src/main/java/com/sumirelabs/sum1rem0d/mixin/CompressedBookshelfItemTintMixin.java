package com.sumirelabs.sum1rem0d.mixin;
import com.mojang.serialization.MapCodec;
import com.sumirelabs.sum1rem0d.client.CompressedBookshelfTint;
import com.sumirelabs.sum1rem0d.game.ModContent;
import net.minecraft.client.color.item.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemTintSources.class)
public abstract class CompressedBookshelfItemTintMixin {
    @Shadow @Final private static ExtraCodecs.LateBoundIdMapper<Identifier,MapCodec<? extends ItemTintSource>> ID_MAPPER;
    @Inject(method="bootstrap",at=@At("TAIL"))
    private static void sum1rem0d$register(CallbackInfo ci){ID_MAPPER.put(ModContent.id("compressed_bookshelf"),CompressedBookshelfTint.CODEC);}
}
