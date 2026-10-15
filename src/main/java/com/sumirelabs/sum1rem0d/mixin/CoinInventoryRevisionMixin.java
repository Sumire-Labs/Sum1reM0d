package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.CoinInventoryRevision;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class CoinInventoryRevisionMixin implements CoinInventoryRevision {
    @Unique private String sum1rem0d$coinRevision="";
    public String sum1rem0d$coinRevision(){return sum1rem0d$coinRevision;}
    public void sum1rem0d$coinRevision(String revision){sum1rem0d$coinRevision=revision;}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void sum1rem0d$read(ValueInput input,CallbackInfo ci){sum1rem0d$coinRevision=input.getStringOr("sum1rem0d_coin_revision","");}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void sum1rem0d$write(ValueOutput output,CallbackInfo ci){output.putString("sum1rem0d_coin_revision",sum1rem0d$coinRevision);}
    @Inject(method="restoreFrom",at=@At("TAIL"))
    private void sum1rem0d$respawn(ServerPlayer old,boolean keep,CallbackInfo ci){sum1rem0d$coinRevision=((CoinInventoryRevision)old).sum1rem0d$coinRevision();}
}
