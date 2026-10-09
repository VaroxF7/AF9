package com.af9.core.elevator;

import com.af9.core.machine.console.SpaceElevatorConsoleWidget;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.af9.core.elevator.SpaceMiningRecipeUI.*;

/**
 * The drawing behind a Space Elevator expedition's slots ({@link SpaceMiningRecipeUI}), as GTNH's Eye of Harmony page: nothing
 * but the empty slots of the grid, a short tube from the drone at the top down to it, and the drone's tier ("T3") on its slot.
 */
public class SpaceMiningFlowWidget extends Widget {

    private final int rows;
    private int tier = 1;

    public SpaceMiningFlowWidget(boolean liquid, int height, int rows) {
        super(0, 0, WIDTH, height);
        this.rows = rows;
    }

    /** The tier of the recipe's drone: the colour of its tier label, the stacks it brings. */
    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;

        // the tube from the drone to the grid
        int centre = x0 + WIDTH / 2;
        graphics.fill(centre - 3, y0 + TOP_Y + 18, centre + 3, y0 + GRID_Y - 1, 0xFF373737);
        graphics.fill(centre - 2, y0 + TOP_Y + 18, centre + 2, y0 + GRID_Y - 2, 0xFFFFFFFF);

        // the empty slots of the grid
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int x = x0 + GRID_X + 18 * column, y = y0 + GRID_Y + 18 * row;
                graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
                graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFF555555);
                graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFF555555);
            }
        }

        // the drone's tier on its slot
        String label = "T" + tier;
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(centre - 8, y0 + TOP_Y + 11, 200);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        graphics.drawString(font, label, 0, 0, SpaceElevatorConsoleWidget.tierColor(tier), true);
        graphics.pose().popPose();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
