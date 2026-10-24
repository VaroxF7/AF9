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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

import static com.af9.core.machine.PlasmaForgeRecipeUI.*;

/**
 * The drawing behind a plasma forge recipe's slots ({@link PlasmaForgeRecipeUI}): the pipes of what goes in (items
 * above, fluids below) meeting in a manifold in front of the bore, the bore itself (a glass tube through three coil
 * rings, a plasma bar down its middle in the colour of the recipe's first fluid, the field's hoops running down it),
 * and the pipes of the forged matter out to the right. Only the pipes of slots the recipe uses are drawn.
 */
public class PlasmaForgeFlowWidget extends Widget {

    private static final int GLASS = 0xFF16222E, GLASS_EDGE = 0xFF5E7A94, COIL = 0xFF2E6FD6, COIL_LIT = 0xFF8FD8FF,
            LABEL = 0xFFB8C2D6, FORGE = 0xFFFF9A3C;
    private static final int[] FLOW = { 0xFFFFD08A, 0xFFFF9A3C };

    private int itemsIn = 1, fluidsIn = 1, itemsOut = 1, fluidsOut = 1;
    private int plasma = FORGE;

    public PlasmaForgeFlowWidget() {
        super(0, 0, WIDTH, HEIGHT);
    }

    /** The recipe shown: how many slots of each kind it uses, and the colour of the plasma bar. */
    public void setRecipe(GTRecipe recipe) {
        itemsIn = used(recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()));
        fluidsIn = used(recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of()));
        itemsOut = used(recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of()));
        fluidsOut = used(recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of()));
        plasma = FORGE;
        // the bar burns in the tint of the recipe's first fluid, in or out
        for (var side : List.of(recipe.inputs, recipe.outputs)) {
            List<Content> fluids = side.getOrDefault(FluidRecipeCapability.CAP, List.of());
            if (fluids.isEmpty()) continue;
            FluidStack[] options = FluidRecipeCapability.CAP.of(fluids.get(0).content).getStacks();
            if (options.length > 0 && !options[0].isEmpty()) {
                plasma = 0xFF000000 | IClientFluidTypeExtensions.of(options[0].getFluid()).getTintColor(options[0]);
                break;
            }
        }
    }

    private static int used(List<Content> contents) {
        return Math.max(0, Math.min(SLOTS, contents.size()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        long time = System.currentTimeMillis();
        int left = x0 + LEFT_MANIFOLD, right = x0 + RIGHT_MANIFOLD;
        int cy = y0 + BORE_Y;
        int itemY = y0 + ITEMS_Y + 9, fluidY = y0 + FLUIDS_Y + 9;

        // the pipes in: items from the top row, fluids from the bottom row, into the manifold and the bore
        int top = cy, bottom = cy;
        if (itemsIn > 0) {
            LithoFlowWidget.hPipe(graphics, x0 + IN_X + 18 * itemsIn, left + 2, itemY, time, FLOW);
            top = itemY;
        }
        if (fluidsIn > 0) {
            LithoFlowWidget.hPipe(graphics, x0 + IN_X + 18 * fluidsIn, left + 2, fluidY, time, FLOW);
            bottom = fluidY;
        }
        LithoFlowWidget.vPipe(graphics, left, top - 1, cy, time, FLOW[0], true);
        LithoFlowWidget.vPipe(graphics, left, cy, bottom + 1, time, FLOW[0], false);
        LithoFlowWidget.hPipe(graphics, left, x0 + BORE_X0, cy, time, FLOW);

        // the pipes out: from the bore into a manifold that splits to the item row and the fluid row
        top = cy;
        bottom = cy;
        if (itemsOut > 0) top = itemY;
        if (fluidsOut > 0) bottom = fluidY;
        LithoFlowWidget.hPipe(graphics, x0 + BORE_X1, right + 2, cy, time, FLOW);
        LithoFlowWidget.vPipe(graphics, right, top - 1, cy, time, FLOW[1], false);
        LithoFlowWidget.vPipe(graphics, right, cy, bottom + 1, time, FLOW[1], true);
        if (itemsOut > 0) LithoFlowWidget.hPipe(graphics, right + 2, x0 + OUT_X, itemY, time, FLOW);
        if (fluidsOut > 0) LithoFlowWidget.hPipe(graphics, right + 2, x0 + OUT_X, fluidY, time, FLOW);

        // the bore: a glass tube, the plasma bar breathing down its middle, three coil rings round it
        int bx0 = x0 + BORE_X0, bx1 = x0 + BORE_X1;
        graphics.fill(bx0, cy - 8, bx1, cy + 8, GLASS_EDGE);
        graphics.fill(bx0, cy - 7, bx1, cy + 7, GLASS);
        float pulse = 0.5F + 0.5F * (float) Math.sin(time / 350.0);
        graphics.fill(bx0 + 1, cy - 3, bx1 - 1, cy + 3, withAlpha(plasma, (int) (0x38 + 0x30 * pulse)));
        graphics.fill(bx0 + 1, cy - 1, bx1 - 1, cy + 1, withAlpha(plasma, (int) (0xA0 + 0x5F * pulse)));
        graphics.fill(bx0 + 1, cy, bx1 - 1, cy + 1, withAlpha(0xFFFFFF, 0xC0));
        // the field's hoops run down the bore, two at a time, as they do in the hall
        for (int hoop = 0; hoop < 2; hoop++) {
            float phase = (time / 1600.0F + hoop * 0.5F) % 1F;
            int hx = bx0 + 1 + Math.round(phase * (bx1 - bx0 - 3));
            int alpha = (int) (0xE0 * Math.sin(phase * Math.PI));
            graphics.fill(hx, cy - 6, hx + 1, cy + 6, withAlpha(COIL_LIT, alpha));
        }
        for (int ring = 0; ring < 3; ring++) {
            int rx = bx0 + 6 + ring * 13;
            // the rings light up one after the other, in the direction of the flow
            boolean lit = (time / 260 + (2 - ring)) % 3 == 0;
            graphics.fill(rx, cy - 12, rx + 4, cy + 12, 0xFF10161E);
            graphics.fill(rx + 1, cy - 12, rx + 3, cy + 12, lit ? COIL_LIT : COIL);
        }

        // the machine's name under the bore
        drawSmallCentered(graphics, Component.translatable("af9.recipe.forge_page.machine").getString(),
                (bx0 + bx1) / 2, cy + 17, LABEL);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawSmallCentered(GuiGraphics graphics, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(Math.round(x - font.width(text) * 3 / 8F), y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
