package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.game.FletchingMenu;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Local rendering fixture; authoritative crafting and transfers are verified on the server. */
final class FletchingGuiVerification {
    private static JsonObject original;
    private static FletchingMenu menu;
    private static FletchingScreen screen;
    private FletchingGuiVerification() {}
    static void begin(Minecraft mc) {
        original = ClientRules.snapshot(); var fixture = original.deepCopy();
        var options = fixture.getAsJsonObject("features").getAsJsonObject("fletching");
        options.addProperty("enabled", true); options.addProperty("input_item", "minecraft:stick"); options.addProperty("input_count", 1);
        options.addProperty("output_item", "minecraft:arrow"); options.addProperty("output_count", 1000);
        ClientRules.accept(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, fixture.toString(), "", 0, false));
        menu = new FletchingMenu(9876, mc.player.getInventory()); menu.slots.getFirst().setByPlayer(new ItemStack(Items.STICK));
        screen = new FletchingScreen(menu, mc.player.getInventory(), Component.translatable("block.minecraft.fletching_table")); mc.gui.setScreen(screen);
    }
    private static Button button() { return screen.children().stream().filter(value -> value instanceof Button).map(value -> (Button) value).findFirst().orElseThrow(); }
    static void craft(Minecraft mc) {
        if (mc.gui.screen() != screen || !button().active || !menu.clickMenuButton(mc.player, 0)) throw new AssertionError("Native fletching screen must enable crafting for a stick");
    }
    static void verifyOutput() {
        int count = 0;
        for (int i = 1; i < 28; i++) { var stack = menu.slots.get(i).getItem(); if (!stack.isEmpty()) { if (!stack.is(Items.ARROW) || stack.getCount() > stack.getMaxStackSize()) throw new AssertionError("Invalid fletching output stack"); count += stack.getCount(); } }
        if (count != 1000 || button().active || !menu.slots.getFirst().getItem().isEmpty()) throw new AssertionError("Output screen must display exactly 1000 arrows and disable another craft");
    }
    static void restore() { if (original != null) { ClientRules.accept(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, original.toString(), "", 0, false)); original = null; } }
}
