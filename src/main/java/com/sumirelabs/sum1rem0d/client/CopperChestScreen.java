package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.mixin.CopperChestSlotLayoutMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/** Scrollable copper-chest UI: all container rows remain addressable on short screens. */
public final class CopperChestScreen extends AbstractContainerScreen<ChestMenu> {
    private static final int WIDTH = 176;
    private static final int MAX_VIEW_ROWS = 6;
    private static final int CHEST_TOP = 18;
    private final int containerRows;
    private int visibleRows;
    private int firstRow;
    private boolean draggingScroll;

    public CopperChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, initialPanelHeight(menu.getRowCount()));
        containerRows = menu.getRowCount();
    }

    private static int initialPanelHeight(int rows) {
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int visible = visibleRows(rows, height);
        return 112 + visible * 18;
    }

    private static int visibleRows(int rows, int screenHeight) {
        int fit = Math.max(1, (screenHeight - 128) / 18);
        return Math.max(1, Math.min(Math.min(rows, MAX_VIEW_ROWS), fit));
    }

    private int panelHeight() { return 112 + visibleRows * 18; }
    int visibleRowsForVerification() { return visibleRows; }
    int firstRowForVerification() { return firstRow; }
    int scrollTestXForVerification() { return leftPos + 48; }
    int scrollTestYForVerification() { return topPos + 24; }
    int slotYForVerification(int index) { return menu.slots.get(index).y; }
    int panelHeightForVerification() { return panelHeight(); }
    private int scrollTrackY() { return topPos + CHEST_TOP; }
    private int scrollTrackHeight() { return visibleRows * 18; }
    private int scrollThumbHeight() { return Math.max(12, scrollTrackHeight() * visibleRows / containerRows); }
    private int scrollThumbY() {
        int range = containerRows - visibleRows;
        int travel = scrollTrackHeight() - scrollThumbHeight();
        return scrollTrackY() + (range <= 0 ? 0 : travel * firstRow / range);
    }

    @Override protected void init() {
        super.init();
        visibleRows = visibleRows(containerRows, height);
        firstRow = Math.max(0, Math.min(containerRows - visibleRows, firstRow));
        leftPos = Math.max(0, (width - WIDTH) / 2);
        topPos = Math.max(4, (height - panelHeight()) / 2);
        layoutSlots();
    }

    private void layoutSlots() {
        int chestSlots = containerRows * 9;
        int mainInventoryY = visibleRows * 18 + 34;
        int hotbarY = visibleRows * 18 + 88;
        for (int index = 0; index < menu.slots.size(); index++) {
            var slot = menu.slots.get(index);
            int x, y;
            if (index < chestSlots) {
                int row = index / 9, column = index % 9;
                x = 8 + column * 18;
                y = row >= firstRow && row < firstRow + visibleRows ? CHEST_TOP + (row - firstRow) * 18 : -10_000;
            } else if (index < chestSlots + 27) {
                int inventorySlot = index - chestSlots;
                x = 8 + inventorySlot % 9 * 18;
                y = mainInventoryY + inventorySlot / 9 * 18;
            } else {
                int hotbarSlot = index - chestSlots - 27;
                x = 8 + hotbarSlot % 9 * 18;
                y = hotbarY;
            }
            var access = (CopperChestSlotLayoutMixin) (Object) slot;
            access.sum1rem0d$setX(x);
            access.sum1rem0d$setY(y);
        }
    }

    private void scrollTo(double mouseY) {
        int range = containerRows - visibleRows;
        if (range <= 0) return;
        int travel = scrollTrackHeight() - scrollThumbHeight();
        if (travel <= 0) return;
        double thumbCenter = scrollThumbHeight() / 2.0;
        double fraction = Math.max(0, Math.min(1, (mouseY - scrollTrackY() - thumbCenter) / travel));
        int next = (int) Math.round(fraction * range);
        if (next != firstRow) { firstRow = next; layoutSlots(); }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        boolean overChest = mouseX >= leftPos + 6 && mouseX < leftPos + 170
                && mouseY >= scrollTrackY() && mouseY < scrollTrackY() + scrollTrackHeight();
        if (vertical != 0 && overChest && containerRows > visibleRows) {
            firstRow = Math.max(0, Math.min(containerRows - visibleRows, firstRow + (vertical < 0 ? 1 : -1)));
            layoutSlots();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT
                && event.x() >= leftPos + 170 && event.x() <= leftPos + 176
                && event.y() >= scrollTrackY() && event.y() < scrollTrackY() + scrollTrackHeight()
                && containerRows > visibleRows) {
            draggingScroll = true;
            scrollTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScroll) { scrollTo(event.y()); return true; }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScroll) { draggingScroll = false; return true; }
        return super.mouseReleased(event);
    }

    @Override protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return mouseX < leftPos || mouseY < topPos || mouseX >= leftPos + WIDTH || mouseY >= topPos + panelHeight();
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        VanillaGuiParts.panel(graphics, leftPos, topPos, WIDTH, panelHeight());
        int chestSlots = containerRows * 9;
        for (int index = 0; index < menu.slots.size(); index++) {
            var slot = menu.slots.get(index);
            if (slot.y < 0 || slot.y >= panelHeight() - 2) continue;
            if (index < chestSlots && (index / 9 < firstRow || index / 9 >= firstRow + visibleRows)) continue;
            VanillaGuiParts.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        graphics.text(font, title, leftPos + 8, topPos + 6, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle, leftPos + 8, topPos + visibleRows * 18 + 20, 0xFF404040, false);
        if (containerRows > visibleRows) {
            int trackX = leftPos + 172;
            graphics.fill(trackX, scrollTrackY(), trackX + 4, scrollTrackY() + scrollTrackHeight(), 0xFF8C8272);
            graphics.fill(trackX - 1, scrollThumbY(), trackX + 4, scrollThumbY() + scrollThumbHeight(), 0xFF524B40);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) { }
}
