package com.sumirelabs.sum1rem0d.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Shared status effect and server-side eligibility for slime-ball chat muting. */
public final class SlimeMouthContent {
    private SlimeMouthContent() {}
    public static MobEffect createEffect() { return new MouthEffect(); }
    /** Changes only the recipient-visible unsigned text after the server has verified the signed message. */
    public static PlayerChatMessage visibleChat(ServerPlayer sender, PlayerChatMessage message) {
        if (!Rules.on("slime_mouth") || !sender.hasEffect(ModContent.effect("slime_mouth"))) return message;
        return message.withUnsignedContent(Component.literal(Rules.s("slime_mouth", "forced_text")));
    }
    private static final class MouthEffect extends MobEffect {
        private MouthEffect() { super(MobEffectCategory.HARMFUL, 0x83C94A); }
    }
}
