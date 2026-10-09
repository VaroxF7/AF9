package com.af9.core.elevator;

import com.af9.core.machine.console.SpaceElevatorConsoleWidget;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.af9.core.elevator.SpaceMiningRecipeUI.*;

/**
 * The drawing behind a Space Elevator expedition's slots ({@link SpaceMiningRecipeUI}), still and plain: the names of
 * the three inputs under their slots, what an expedition of this drone brings beside them, and a panel under it all
 * that the grid of outputs sits on, with its heading. A liquid mission's page says the same of its fluids.
 */
public class SpaceMiningFlowWidget extends Widget {

    private static final int PANEL = 0xFF070A11, EDGE = 0xFF25324A, LABEL = 0xFFB8C2D6;

    private final boolean liquid;
    private int tier = 1;

    public SpaceMiningFlowWidget(boolean liquid, int height) {
        super(0, 0, WIDTH, height);
        this.liquid = liquid;
    }

    /** The tier of the recipe's drone: the colour of what it says, the stacks it brings. */
    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int height = getSize().height;
        int color = SpaceElevatorConsoleWidget.tierColor(tier);

        // the names of the inputs, under their slots
        drawSmall(graphics, Component.translatable("af9.recipe.space_mining.drone").getString(), x0 + IN_X, y0 + 24,
                color);
        drawSmall(graphics, Component.translatable("af9.recipe.space_mining.fuel").getString(), x0 + IN_X + 22,
                y0 + 24, LABEL);
        drawSmall(graphics, Component.translatable("af9.recipe.space_mining.coolant").getString(), x0 + IN_X + 44,
                y0 + 24, 0xFF7DD3FC);

        // what an expedition brings, beside the arrow
        String first = liquid ? Component.translatable("af9.recipe.space_pumping.one").getString() :
                Component.translatable("af9.recipe.space_mining.stacks", SpaceMissionMachine.minStacks(tier),
                        SpaceMissionMachine.maxStacks(tier)).getString();
        String second = Component.translatable(liquid ? "af9.recipe.space_pumping.pick" :
                "af9.recipe.space_mining.ore").getString();
        drawSmall(graphics, first, x0 + ARROW_X + 26, y0 + 9, 0xFFFFFFFF);
        drawSmall(graphics, second, x0 + ARROW_X + 26, y0 + 17, LABEL);

        // the panel of the outputs, with its heading
        graphics.fill(x0 + 2, y0 + GRID_Y - 12, x0 + WIDTH - 2, y0 + height - 2, PANEL);
        border(graphics, x0 + 2, y0 + GRID_Y - 12, WIDTH - 4, height - GRID_Y + 10, EDGE);
        drawSmall(graphics, Component.translatable(liquid ? "af9.recipe.space_pumping.grid" :
                "af9.recipe.space_mining.grid").getString(), x0 + 6, y0 + GRID_Y - 9, LABEL);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmall(GuiGraphics graphics, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void border(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
