package com.af9.core.compat.powah.client;

import java.util.Locale;

import com.af9.core.compat.powah.OrbMk2Menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Energizing Orb Mk2's screen: six slots that hold a stack each, the charge as a bar and the product. Drawn
 * with plain fills (the vanilla container look), so it needs no picture of its own.
 */
public class OrbMk2Screen extends AbstractContainerScreen<OrbMk2Menu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int BAR_BACK = 0xFF2B2B2B;
    private static final int BAR_FILL = 0xFF37E0FF;
    private static final int BAR_DONE = 0xFF6BFF6B;

    private static final int BAR_X = 30;
    private static final int BAR_Y = 62;
    private static final int BAR_W = 116;
    private static final int BAR_H = 8;

    public OrbMk2Screen(OrbMk2Menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // the panel, with the bevel of the vanilla containers
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth - 1, y + 1, LIGHT);
        graphics.fill(x, y, x + 1, y + imageHeight - 1, LIGHT);
        graphics.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, DARK);
        for (int i = 0; i < OrbMk2Menu.INPUTS; i++) {
            slotFrame(graphics, x + OrbMk2Menu.INPUT_X + (i % 3) * 18, y + OrbMk2Menu.INPUT_Y + (i / 3) * 18);
        }
        slotFrame(graphics, x + OrbMk2Menu.PRODUCT_X, y + OrbMk2Menu.PRODUCT_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slotFrame(graphics, x + 8 + col * 18, y + 84 + row * 18);
        }
        for (int col = 0; col < 9; col++) slotFrame(graphics, x + 8 + col * 18, y + 142);

        // an arrow from the inputs to the product
        int ax = x + 92;
        int ay = y + 33;
        graphics.fill(ax, ay + 3, ax + 24, ay + 7, DARK);
        for (int i = 0; i < 5; i++) graphics.fill(ax + 24 + i, ay + 5 - (4 - i), ax + 25 + i, ay + 5 + (4 - i) + 1, DARK);

        // the charge
        long stored = menu.stored();
        long required = menu.required();
        graphics.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, SLOT_EDGE);
        graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, BAR_BACK);
        if (required > 0) {
            int fill = (int) Math.min(BAR_W, BAR_W * stored / required);
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + fill, y + BAR_Y + BAR_H, stored >= required ? BAR_DONE : BAR_FILL);
        }
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        Component line;
        if (menu.hasRecipe()) {
            line = Component.translatable("af9.orb_mk2.charge", format(menu.stored()), format(menu.required()));
        } else {
            line = Component.translatable("af9.orb_mk2.no_recipe");
        }
        graphics.drawString(font, line, BAR_X, BAR_Y - 11, 0x404040, false);
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
