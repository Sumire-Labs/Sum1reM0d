package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerLeashInteractionMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$leadPlayer(Entity target, InteractionHand hand, Vec3 hit, CallbackInfoReturnable<InteractionResult> cir) {
        Player holder = (Player) (Object) this;
        if (!(holder instanceof ServerPlayer serverHolder) || !(target instanceof ServerPlayer serverTarget)
                || !com.sumirelabs.sum1rem0d.game.WorldMobility.enabled("unbreakable_lead") || !Rules.b("unbreakable_lead", "allow_players")
                || !holder.getItemInHand(hand).is(Items.LEAD)) return;
        if (holder == target || holder.level() != target.level() || !target.isAlive() || target.isSpectator()
                || holder.distanceToSqr(target) > 64) return;
        var holderLeash = (Leashable) holder;
        var targetLeash = (Leashable) target;
        if (holderLeash.isLeashed() && holderLeash.getLeashHolder() == target) {
            holderLeash.removeLeash(); cir.setReturnValue(InteractionResult.SUCCESS); return;
        }
        if (targetLeash.isLeashed()) {
            if (targetLeash.getLeashHolder() == holder) targetLeash.removeLeash();
            cir.setReturnValue(InteractionResult.SUCCESS); return;
        }
        double maximum = Math.max(0, Math.min(256, Rules.n("unbreakable_lead", "max_length")));
        if (maximum > 0 && holder.distanceTo(target) > maximum) return;
        targetLeash.setLeashedTo(holder, true);
        holder.getItemInHand(hand).shrink(1);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
