package com.af9.core.litho;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.af9.core.litho.LithoRecipeUI.*;

/**
 * The drawing behind a lithography recipe's slots ({@link LithoRecipeUI}): a pipe off every row (the items, the fluid
 * rows) into a manifold, the manifold into the machine's frame (the controller drawn large, the node's colour, the node
 * above and the machine's name below), and dashes flowing along all of it to the machine: in the node's colour from
 * the items, in the colours of each row's fluids from the track. The 1 nm station's dry process has no track: a note
 * stands where the fluids would be, clear of the manifold.
 */
public class LithoFlowWidget extends Widget {

    private static final int PIPE = 0xFF4B5563, PIPE_EDGE = 0xFF2A313C, LABEL = 0xFFB8C2D6;
    /** Labels are drawn at this scale; line height of wrapped text. */
    private static final float SMALL = 0.75F;
    private static final int LINE = 7;
    /** Dash length and spacing along the pipes, and how fast they flow (ms per pixel). */
    private static final int DASH = 3, PERIOD = 8, SPEED = 60;

    private final LithoMode mode;
    private int items;
    private int fluids;
    private List<FluidStack> fluidStacks = List.of();
    private ItemStack machine;

    public LithoFlowWidget(LithoMode mode, int items, int fluids) {
        super(0, 0, WIDTH, HEIGHT);
        this.mode = mode;
        this.items = items;
        this.fluids = fluids;
    }

    /** The machine that prints the node: its controller's id (gtceu namespace). */
    public static String machineId(LithoMode mode) {
        return switch (mode.machine) {
            case LINE -> "photolithography_line";
            case SCANNER -> "photolithography_scanner";
            case ORBITAL -> "orbital_lithography_station";
        };
    }

    /** The recipe shown: how many items and which fluids it really has (the pipes start after its last slot). */
    public void setRecipe(GTRecipe recipe) {
        int itemCount = 0;
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (itemCount < PER_ROW) itemCount++;
        }
        List<FluidStack> stacks = new ArrayList<>();
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidStack[] options = FluidRecipeCapability.CAP.of(content.content).getStacks();
            if (options.length > 0 && stacks.size() < PER_ROW * FLUID_ROWS) stacks.add(options[0]);
        }
        items = itemCount;
        fluids = stacks.size();
        fluidStacks = stacks;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        long time = System.currentTimeMillis();
        int node = mode.argb;
        int manifold = x0 + MANIFOLD_X;
        int centre = y0 + CENTER_Y;

        // the rows: items, then the fluid rows; each piped from its last slot to the manifold
        int top = centre, bottom = centre;
        if (items > 0) {
            int y = y0 + ITEMS_Y + 9;
            hPipe(graphics, x0 + SLOTS_X + 18 * items, manifold + 2, y, time, new int[] { node });
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        int rows = (fluids + PER_ROW - 1) / PER_ROW;
        for (int row = 0; row < rows; row++) {
            int inRow = Math.min(PER_ROW, fluids - row * PER_ROW);
            int[] colors = new int[inRow];
            for (int i = 0; i < inRow; i++) colors[i] = fluidColor(row * PER_ROW + i);
            int y = y0 + FLUIDS_Y + 18 * row + 9;
            hPipe(graphics, x0 + SLOTS_X + 18 * inRow, manifold + 2, y, time, colors);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        if (fluids == 0) {
            // the dry process: no track chemicals. The note stays clear of the manifold (its hover says the rest)
            int width = MANIFOLD_X - 3 - (SLOTS_X + 2);
            drawSmall(graphics, Component.translatable("af9.recipe.litho_page.dry").getString(),
                    x0 + SLOTS_X + 2, y0 + FLUIDS_Y + 1, LABEL);
            drawWrapped(graphics, Component.translatable("af9.recipe.litho_page.dry_detail").getString(),
                    x0 + SLOTS_X + 2, y0 + FLUIDS_Y + 9, width, 2, 0xFF8A94A8);
        }
        // the manifold: down from the top row, up from the bottom one, into the machine
        vPipe(graphics, manifold, top - 1, centre, time, node, true);
        vPipe(graphics, manifold, centre, bottom + 1, time, node, false);
        hPipe(graphics, manifold, x0 + BOX_X, centre, time, new int[] { node });

        // the machine: a glowing frame in the node's colour, the controller drawn large
        int bx = x0 + BOX_X, by = centre - BOX_SIZE / 2;
        float pulse = 0.5F + 0.5F * (float) Math.sin(time / 400.0);
        graphics.fill(bx - 2, by - 2, bx + BOX_SIZE + 2, by + BOX_SIZE + 2, withAlpha(node, (int) (0x18 + 0x28 * pulse)));
        graphics.fill(bx, by, bx + BOX_SIZE, by + BOX_SIZE, 0xFF0E131C);
        int edge = withAlpha(node, (int) (0xA0 + 0x5F * pulse));
        graphics.fill(bx, by, bx + BOX_SIZE, by + 1, edge);
        graphics.fill(bx, by + BOX_SIZE - 1, bx + BOX_SIZE, by + BOX_SIZE, edge);
        graphics.fill(bx, by, bx + 1, by + BOX_SIZE, edge);
        graphics.fill(bx + BOX_SIZE - 1, by, bx + BOX_SIZE, by + BOX_SIZE, edge);
        ItemStack stack = machineStack();
        if (!stack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(bx + 1, by + 1, 0);
            graphics.pose().scale(2F, 2F, 1F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }
        // the node above it, the machine below it (under the frame's glow and the manifold's end)
        drawSmallCentered(graphics, mode.nodeNm + " nm", bx + BOX_SIZE / 2, by - 9, node);
        drawSmallCentered(graphics, Component.translatable("af9.recipe.litho_page.machine." +
                mode.machine.name().toLowerCase(Locale.ROOT)).getString(), bx + BOX_SIZE / 2, by + BOX_SIZE + 5, LABEL);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    /**
     * A pipe from xa to xb at y, dashes flowing right, coloured in turn by the colours given (the accelerator's page
     * draws with these too).
     */
    @OnlyIn(Dist.CLIENT)
    public static void hPipe(GuiGraphics graphics, int xa, int xb, int y, long time, int[] colors) {
        hPipe(graphics, xa, xb, y, time, colors, PIPE, PIPE_EDGE);
    }

    /** The same pipe in other colours: its body and its edge. */
    @OnlyIn(Dist.CLIENT)
    public static void hPipe(GuiGraphics graphics, int xa, int xb, int y, long time, int[] colors, int body,
                             int edge) {
        if (xb <= xa) return;
        graphics.fill(xa, y - 2, xb, y + 2, edge);
        graphics.fill(xa, y - 1, xb, y + 1, body);
        int offset = (int) (time / SPEED % PERIOD);
        int index = 0;
        for (int d = xa - PERIOD + offset; d < xb; d += PERIOD, index++) {
            int s = Math.max(d, xa), e = Math.min(d + DASH, xb);
            if (e > s) graphics.fill(s, y - 1, e, y + 1, colors[Math.floorMod(index, colors.length)]);
        }
    }

    /** A vertical pipe from ya to yb at x, dashes flowing down (or up) towards the machine. */
    @OnlyIn(Dist.CLIENT)
    public static void vPipe(GuiGraphics graphics, int x, int ya, int yb, long time, int color, boolean down) {
        vPipe(graphics, x, ya, yb, time, color, down, PIPE, PIPE_EDGE);
    }

    /** The same pipe in other colours: its body and its edge. */
    @OnlyIn(Dist.CLIENT)
    public static void vPipe(GuiGraphics graphics, int x, int ya, int yb, long time, int color, boolean down,
                             int body, int edge) {
        if (yb <= ya) return;
        graphics.fill(x - 1, ya, x + 3, yb, edge);
        graphics.fill(x, ya, x + 2, yb, body);
        int offset = (int) (time / SPEED % PERIOD);
        for (int d = ya - PERIOD + (down ? offset : PERIOD - offset); d < yb; d += PERIOD) {
            int s = Math.max(d, ya), e = Math.min(d + DASH, yb);
            if (e > s) graphics.fill(x, s, x + 2, e, color);
        }
    }

    /** The colour of the fluid in the slot (its tint), the node's colour while unknown. */
    @OnlyIn(Dist.CLIENT)
    private int fluidColor(int slot) {
        if (slot >= fluidStacks.size()) return mode.argb;
        FluidStack stack = fluidStacks.get(slot);
        int tint = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        return 0xFF000000 | tint;
    }

    @OnlyIn(Dist.CLIENT)
    private ItemStack machineStack() {
        if (machine == null) {
            @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", machineId(mode)));
            machine = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        return machine;
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmall(GuiGraphics graphics, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(SMALL, SMALL, 1F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmallCentered(GuiGraphics graphics, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        drawSmall(graphics, text, x - Math.round(font.width(text) * SMALL / 2), y, color);
    }

    /** Small text wrapped to {@code width} (on screen), at most {@code maxLines} lines. */
    @OnlyIn(Dist.CLIENT)
    private static void drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int maxLines,
                                    int color) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(FormattedText.of(text), (int) (width / SMALL));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(SMALL, SMALL, 1F);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 0, Math.round(i * LINE / SMALL), color, true);
        }
        graphics.pose().popPose();
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
