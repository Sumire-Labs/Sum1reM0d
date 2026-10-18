package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Japanese composition styling layered over Minecraft's native SDL preedit and candidate positioning. */
public final class ImeInput {
    private static final int TEXT_COLOR = 0xFFF5F1EC;
    private static final int FOCUSED_COLOR = 0xFFFFC77A;
    private ImeInput() {}

    public static MutableComponent format(PreeditEvent event, Style vanillaFocusedStyle) {
        if (!PersonalConfig.store().bool("japanese_ime", "enabled")) return event.toFormattedText(vanillaFocusedStyle);
        var result = Component.empty();
        var blocks = event.blocks();
        if (blocks.isEmpty()) return Component.literal(event.fullText()).withStyle(Style.EMPTY.withColor(TEXT_COLOR).withUnderlined(true));
        for (int index = 0; index < blocks.size(); index++) {
            boolean focused = index == event.focusedBlock();
            Style style = Style.EMPTY.withColor(focused ? FOCUSED_COLOR : TEXT_COLOR)
                    .withUnderlined(true).withBold(focused);
            result.append(Component.literal(blocks.get(index)).withStyle(style));
        }
        return result;
    }
}
