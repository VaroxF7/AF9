package com.af9.core.machine;

import com.af9.core.litho.LithoFlowWidget;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static com.af9.core.machine.AcceleratorRecipeUI.*;

/**
 * The drawing behind a Particle Accelerator recipe's slots ({@link AcceleratorRecipeUI}): the items' pipe (dashes in
 * the mode's colour) and the coolant apart from it, marked as coolant whatever the fluid (an icy glow round its slot,
 * "COOLANT" over it, its own cryo line in ice and frost), meeting in a manifold that runs into the ring's west gate; the ring from above (a beam pipe of dots, four gates, the machine in the middle); particle bunches racing round
 * it in the mode's colour. Neutron irradiation: one bunch, a neutron spray off the target at the south gate on every
 * lap. Heavy-ion collision: two bunches against each other, a flash where they meet. Quark synthesis: the same, and
 * three colour charges circling the machine. The mode's name above the ring, the machine's below.
 */
public class AcceleratorFlowWidget extends Widget {

    private static final int STEEL = 0xFF9AA5B4, GATE = 0xFF1B2433, LABEL = 0xFFB8C2D6;
    /** The coolant's own colours, whatever the fluid: ice, frost, the cryo line's body and edge. */
    public static final int ICE = 0xFF7FE7FF, FROST = 0xFFD6FAFF, CRYO_BODY = 0xFF2B5E6E, CRYO_EDGE = 0xFF10323C;

    private final int mode;
    private int items;
    private int fluids;
    private FluidStack coolant = FluidStack.EMPTY;
    private ItemStack machine;

    public AcceleratorFlowWidget(int mode, int items, int fluids) {
        super(0, 0, WIDTH, HEIGHT);
        this.mode = mode;
        this.items = items;
        this.fluids = fluids;
    }

    /** The recipe shown: how many items it has (the pipe starts after the last) and its coolant. */
    public void setRecipe(GTRecipe recipe) {
        items = Math.min(2, recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).size());
        coolant = FluidStack.EMPTY;
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidStack[] options = FluidRecipeCapability.CAP.of(content.content).getStacks();
            if (options.length > 0) {
                coolant = options[0];
                break;
            }
        }
        fluids = coolant.isEmpty() ? 0 : 1;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        long time = System.currentTimeMillis();
        int color = ParticleAcceleratorMachine.modeColorOf(mode);
        int cold = ICE;
        int manifold = x0 + MANIFOLD_X;
        int cx = x0 + RING_X, cy = y0 + CENTER_Y;

        // the pipes: items from the top, coolant from the bottom, into the ring's west gate
        int top = cy, bottom = cy;
        if (items > 0) {
            int y = y0 + ITEMS_Y + 9;
            LithoFlowWidget.hPipe(graphics, x0 + ITEMS_X + 18 * items, manifold + 2, y, time, new int[] { color });
            top = y;
        }
        if (fluids > 0) {
            // the coolant apart from everything else: an icy glow round its slot, its label, its own cryo line
            int sx = x0 + COOLANT_X, sy = y0 + COOLANT_Y;
            float pulse = 0.5F + 0.5F * (float) Math.sin(time / 500.0);
            graphics.fill(sx - 3, sy - 3, sx + 21, sy + 21, withAlpha(ICE, (int) (0x30 + 0x30 * pulse)));
            drawSmall(graphics, "\u2744 " + Component.translatable("af9.recipe.accelerator_page.coolant_label")
                    .getString(), sx - 1, sy - 10, ICE);
            int y = sy + 9;
            LithoFlowWidget.hPipe(graphics, sx + 18, manifold + 2, y, time, new int[] { FROST, ICE }, CRYO_BODY,
                    CRYO_EDGE);
            bottom = y;
        }
        LithoFlowWidget.vPipe(graphics, manifold, top - 1, cy, time, color, true);
        LithoFlowWidget.vPipe(graphics, manifold, cy, bottom + 1, time, FROST, false, CRYO_BODY, CRYO_EDGE);
        LithoFlowWidget.hPipe(graphics, manifold, cx - RING_R - 3, cy, time, new int[] { color, cold });

        // the ring: a beam pipe of dots and four gates, glowing faintly in the mode's colour
        for (int i = 0; i < 72; i++) {
            double angle = i * Mth.TWO_PI / 72;
            int px = cx + (int) Math.round(Math.cos(angle) * RING_R);
            int py = cy + (int) Math.round(Math.sin(angle) * RING_R);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, STEEL);
        }
        for (int g = 0; g < 4; g++) {
            double angle = g * Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(angle) * RING_R);
            int py = cy + (int) Math.round(Math.sin(angle) * RING_R);
            graphics.fill(px - 3, py - 3, px + 3, py + 3, GATE);
            border(graphics, px - 3, py - 3, 6, 6, withAlpha(color, 0xD0));
        }
        // the machine in the middle
        ItemStack stack = machineStack();
        if (!stack.isEmpty()) graphics.renderItem(stack, cx - 8, cy - 8);

        // the particles: faster laps, a bunch head and its tail
        double speed = 3.2;
        double phase = time / 1000.0 * speed;
        bunch(graphics, cx, cy, Math.PI / 2 + phase, 1, color);
        if (mode == 0) {
            // the spallation target inside the south gate, a neutron spray off it on every lap
            int ty = cy + RING_R - 6;
            graphics.fill(cx - 2, ty, cx + 2, ty + 3, 0xFFB0B8C8);
            double lap = (phase % Mth.TWO_PI) / Mth.TWO_PI;
            if (lap < 0.3) {
                int alpha = (int) (0xFF * (1 - lap / 0.3));
                double reach = 3 + lap / 0.3 * 12;
                for (int i = 0; i < 7; i++) {
                    double a = -Math.PI / 2 + (i - 3) * 0.35;
                    int px = cx + (int) Math.round(Math.cos(a) * reach);
                    int py = ty + (int) Math.round(Math.sin(a) * reach);
                    graphics.fill(px, py, px + 1, py + 1, withAlpha(0xA8FFB8, alpha));
                }
            }
        } else {
            int second = mode == 1 ? 0xFFFF5A36 : 0xFF38E1FF;
            bunch(graphics, cx, cy, Math.PI / 2 - phase, -1, second);
            // they meet at the south and the north gate: a flash
            double meet = Math.abs(Math.sin(phase));
            if (meet < 0.18) {
                int alpha = (int) (0xFF * (1 - meet / 0.18));
                int fy = Math.cos(phase) > 0 ? cy + RING_R : cy - RING_R;
                graphics.fill(cx - 4, fy, cx + 5, fy + 1, withAlpha(0xFFFFFF, alpha));
                graphics.fill(cx, fy - 4, cx + 1, fy + 5, withAlpha(0xFFFFFF, alpha));
            }
            if (mode == 2) {
                int[] charges = { 0xFFFF4040, 0xFF40FF60, 0xFF4080FF };
                for (int i = 0; i < 3; i++) {
                    double a = time / 300.0 + i * Mth.TWO_PI / 3;
                    int px = cx + (int) Math.round(Math.cos(a) * 12), py = cy + (int) Math.round(Math.sin(a) * 12);
                    graphics.fill(px - 1, py - 1, px + 1, py + 1, charges[i]);
                }
            }
        }

        // the mode above the ring, the machine below it
        String[] keys = ParticleAcceleratorMachine.RECIPE_TYPE_KEYS;
        String modeText = Component.translatable(keys[Mth.clamp(mode, 0, keys.length - 1)] + ".short").getString();
        drawSmallCentered(graphics, modeText, cx, cy - RING_R - 11, color);
        drawSmallCentered(graphics, Component.translatable("af9.recipe.accelerator_page.machine").getString(), cx,
                cy + RING_R + 5, LABEL);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    /** A bunch on the beam pipe: a bright head and a fading tail behind it. */
    @OnlyIn(Dist.CLIENT)
    private static void bunch(GuiGraphics graphics, int cx, int cy, double angle, int direction, int color) {
        for (int i = 8; i >= 0; i--) {
            double a = angle - direction * i * 0.1;
            int px = cx + (int) Math.round(Math.cos(a) * RING_R), py = cy + (int) Math.round(Math.sin(a) * RING_R);
            int size = i == 0 ? 2 : 1;
            graphics.fill(px - size, py - size, px + size, py + size,
                    withAlpha(i == 0 ? 0xFFFFFF : color, i == 0 ? 0xFF : 0xD0 - i * 0x16));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private ItemStack machineStack() {
        if (machine == null) {
            @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "particle_accelerator"));
            machine = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        return machine;
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
