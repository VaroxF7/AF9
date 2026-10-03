package com.af9.core.elevator;

import com.af9.core.litho.LithoFlowWidget;
import com.af9.core.machine.AcceleratorFlowWidget;
import com.af9.core.machine.console.SpaceElevatorConsoleWidget;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.af9.core.elevator.SpaceMiningRecipeUI.*;

/**
 * The drawing behind a Space Elevator expedition's slots ({@link SpaceMiningRecipeUI}): the hydrogen's pipe and, apart
 * from it and in ice, the coolant's, into the foot of the tower; the tower on the ground with its cable up to orbit,
 * the running light on it and the climber; the drone's way to the asteroid and back with the ore, in the drone's
 * colour; over the fluids what they are, under the ore slots the stacks an expedition brings. A liquid mission's page
 * has a planet for the asteroid, and under its fluid slots that a mission brings one of them.
 */
public class SpaceMiningFlowWidget extends Widget {

    private static final int SKY = 0xFF070A11, EDGE = 0xFF25324A, LABEL = 0xFFB8C2D6;
    /** The scene: the ground, the tower on it, orbit, the asteroid. */
    private static final int GROUND_Y = 70, TOWER_X = SCENE_X + 12, TOWER_H = 20, ORBIT_Y = 12;
    private static final int ROCK_X = SCENE_X + 40, ROCK_Y = 27, ROCK_R = 7;

    /** A liquid mission's cargo on its way home, and the planet it comes from. */
    private static final int FLUID = 0xFF7DD3FC, PLANET = 0xFFC8A45A, PLANET_BAND = 0xFF9C7A3C;

    private final boolean liquid;
    private int tier = 1;

    public SpaceMiningFlowWidget(boolean liquid) {
        super(0, 0, WIDTH, HEIGHT);
        this.liquid = liquid;
    }

    /** The tier of the recipe's drone: the colour of what flies, the stacks it brings. */
    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        long time = System.currentTimeMillis();
        int color = SpaceElevatorConsoleWidget.tierColor(tier);

        // the scene: a piece of sky over the ground
        int sx = x0 + SCENE_X, sy = y0 + 2, ground = y0 + GROUND_Y;
        graphics.fill(sx, sy, sx + SCENE_W, y0 + HEIGHT - 2, SKY);
        border(graphics, sx, sy, SCENE_W, HEIGHT - 4, EDGE);
        for (int i = 0; i < 14; i++) {
            int px = sx + 2 + Math.floorMod(i * 7919 + 5, SCENE_W - 4);
            int py = sy + 2 + Math.floorMod(i * 104729 + 3, GROUND_Y - 8);
            graphics.fill(px, py, px + 1, py + 1, (time / 500 + i * 3L) % 9 == 0 ? 0xFFFFFFFF : 0x44FFFFFF);
        }
        graphics.fill(sx + 1, ground, sx + SCENE_W - 1, ground + 1, EDGE);
        graphics.fill(sx + 1, ground + 1, sx + SCENE_W - 1, y0 + HEIGHT - 3, 0xFF0D131E);

        // the fluids: hydrogen down a manifold, the coolant apart from it in its own cryo line, into the tower's foot
        int tx = x0 + TOWER_X, top = ground - TOWER_H;
        int hydrogen = y0 + HYDROGEN_Y + 9, coolant = y0 + COOLANT_Y + 9, manifold = sx - 6;
        LithoFlowWidget.hPipe(graphics, x0 + IN_X + 18, manifold + 2, hydrogen, time, new int[] { color });
        LithoFlowWidget.vPipe(graphics, manifold, hydrogen - 1, ground - 11, time, color, true);
        LithoFlowWidget.hPipe(graphics, manifold, tx - 5, ground - 12, time, new int[] { color });
        float pulse = 0.5F + 0.5F * Mth.sin(time / 500F);
        graphics.fill(x0 + IN_X - 2, y0 + COOLANT_Y - 2, x0 + IN_X + 20, y0 + COOLANT_Y + 20,
                withAlpha(AcceleratorFlowWidget.ICE, (int) (0x30 + 0x30 * pulse)));
        LithoFlowWidget.hPipe(graphics, x0 + IN_X + 18, tx - 6, coolant, time,
                new int[] { AcceleratorFlowWidget.FROST, AcceleratorFlowWidget.ICE }, AcceleratorFlowWidget.CRYO_BODY,
                AcceleratorFlowWidget.CRYO_EDGE);
        drawSmall(graphics, Component.translatable("af9.recipe.space_mining.fuel").getString(), x0 + IN_X,
                y0 + HYDROGEN_Y - 6, LABEL);
        drawSmall(graphics, "❄ " + Component.translatable("af9.recipe.space_mining.coolant").getString(),
                x0 + IN_X - 1, y0 + COOLANT_Y - 7, AcceleratorFlowWidget.ICE);
        // the drone: from its slot to the cable, at orbit
        int orbit = y0 + ORBIT_Y;
        LithoFlowWidget.hPipe(graphics, x0 + IN_X + 18, tx, orbit - 1, time, new int[] { color });

        // the tower, its cable with the light running up it, the climber riding up and down
        for (int row = 0; row < TOWER_H; row++) {
            int half = 8 - row * 6 / TOWER_H;
            graphics.fill(tx - half, ground - 1 - row, tx + half + 1, ground - row,
                    row % 5 == 2 ? 0xFF2B2E34 : 0xFF1F5FA8);
        }
        graphics.fill(tx, sy + 2, tx + 1, top, 0xFF4B5567);
        int light = top - (int) ((time % 3000) / 3000.0 * (top - sy - 2));
        graphics.fill(tx, Math.max(sy + 2, light - 2), tx + 1, light, 0xFF00A6FF);
        float ride = 0.5F - 0.5F * Mth.cos((time % 8000) / 8000F * Mth.TWO_PI);
        int cy = top - 5 - Math.round(ride * (top - 5 - orbit));
        graphics.fill(tx - 4, cy, tx + 5, cy + 1, 0xFFD2A542);
        graphics.fill(tx - 1, cy - 1, tx + 2, cy + 2, 0xFFBFC5CC);
        // orbit, where the drones leave the cable
        for (int dash = sx + 3; dash < sx + SCENE_W - 3; dash += 4) {
            graphics.fill(dash, orbit + 4, dash + 2, orbit + 5, 0x30FFFFFF);
        }

        // the asteroid (a liquid mission: the planet), and the drone out to it and home with the cargo
        int ax = x0 + ROCK_X, ay = y0 + ROCK_Y;
        for (int dy = -ROCK_R; dy <= ROCK_R; dy++) {
            int half = (int) Math.sqrt((double) ROCK_R * ROCK_R - dy * dy);
            if (liquid) {
                graphics.fill(ax - half, ay + dy, ax + half + 1, ay + dy + 1,
                        Math.floorMod(dy, 4) == 1 ? PLANET_BAND : PLANET);
            } else {
                graphics.fill(ax - half + Math.floorMod(dy * 37, 2), ay + dy,
                        ax + half + 1 - Math.floorMod(dy * 53, 3), ay + dy + 1, 0xFF4A5261);
            }
        }
        if (!liquid) {
            graphics.fill(ax - 3, ay - 3, ax, ay - 1, 0xFF353C49);
            graphics.fill(ax + 1, ay + 1, ax + 4, ay + 3, 0xFF353C49);
            int glint = (int) (time / 300 % 4);
            graphics.fill(ax - 2 + glint, ay - 4, ax - 1 + glint, ay - 3, 0xFFFFE08A);
        }
        double lap = (time % 4000) / 4000.0;
        double way = lap < 0.45 ? lap / 0.45 : lap > 0.55 ? (1 - lap) / 0.45 : 1;
        boolean home = lap > 0.55;
        int droneX = (int) Math.round(Mth.lerp(way, tx + 3, ax - ROCK_R - 3));
        int droneY = (int) Math.round(Mth.lerp(way, orbit + 4, ay) - Math.sin(Math.PI * way) * 5);
        graphics.fill(droneX, droneY - 1, droneX + 2, droneY + 1, !home ? color : liquid ? FLUID : 0xFFFFE08A);

        // under the slots: the stacks an expedition of this drone brings, or that a liquid mission brings one fluid
        int middle = x0 + ORES_X + 9 * ORE_COLUMNS;
        String first = liquid ? Component.translatable("af9.recipe.space_pumping.one").getString() :
                Component.translatable("af9.recipe.space_mining.stacks", SpaceElevatorMachine.minStacks(tier),
                        SpaceElevatorMachine.maxStacks(tier)).getString();
        String second = Component.translatable(liquid ? "af9.recipe.space_pumping.pick" :
                "af9.recipe.space_mining.ore").getString();
        drawSmallCentered(graphics, first, middle, y0 + ORES_Y + 18 * ORE_ROWS + 3, 0xFFFFFFFF);
        drawSmallCentered(graphics, second, middle, y0 + ORES_Y + 18 * ORE_ROWS + 10, LABEL);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmallCentered(GuiGraphics graphics, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        drawSmall(graphics, text, Math.round(x - font.width(text) * 3 / 8F), y, color);
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

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
