package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.network.FakeAdvancementPayload;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.*;

/** Vanilla appearance with no entry added to either side's advancement tree. */
public final class FakeAdvancementToast extends AdvancementToast {
    private FakeAdvancementToast(FakeAdvancementPayload payload) {
        super(new AdvancementHolder(payload.id(), new Advancement(Optional.empty(), Optional.of(payload.display()), AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.EMPTY, false)));
    }
    public static void receive(FakeAdvancementPayload payload) {
        if (ClientRules.enabled("sleep_advancement")) Minecraft.getInstance().gui.toastManager().addToast(new FakeAdvancementToast(payload));
    }
    @Override public void update(ToastManager manager, long visibleForMs) {
        super.update(manager, scaledTime(visibleForMs));
    }
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, long visibleForMs) { super.extractRenderState(graphics, font, scaledTime(visibleForMs)); }
    private static long scaledTime(long time) { return (long) (time * (double) DISPLAY_TIME / (ClientRules.integer("sleep_advancement", "fake_seconds") * 1000.0)); }
    @Override public Toast.Visibility getWantedVisibility() {
        return ClientRules.enabled("sleep_advancement") ? super.getWantedVisibility() : Toast.Visibility.HIDE;
    }
}
