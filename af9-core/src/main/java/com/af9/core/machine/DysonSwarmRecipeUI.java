package com.af9.core.machine;

import com.af9.core.machine.console.Labels;
import com.af9.core.machine.console.SliderWidget;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.resources.ResourceLocation;

/**
 * The Dyson Swarm's cycle in EMI / JEI: the hydrogen it takes and the arrow, then a slider for the number of sails in the swarm
 * and what that many sails of each tier give, all inside the page (GT's own lines of time, total and generation come under it).
 */
public class DysonSwarmRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEIGHT = 104;
    private static final long EU_PER_SAIL = 262144L;
    private static final String[] TIER_NAMES = { "Allthemodium", "Unobtainium Alloy", "Star Matter Tritan Alloy" };
    private static final double[] YIELD = { 1.0, 2.0, 3.5 };
    private static final int MAX_SAILS = 10000;

    private final GTRecipeType type;
    /** The slider's number of sails; each page keeps its own while it is open. */
    private int sails = MAX_SAILS;

    public DysonSwarmRecipeUI(GTRecipeType type) {
        super(type);
        this.type = type;
    }

    /** Gives the recipe type this page. */
    @SuppressWarnings("removal")
    public static void install() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", "dyson_swarm"));
        if (type == null) return;
        GTRecipeTypeUI old = type.getRecipeUI();
        DysonSwarmRecipeUI ui = new DysonSwarmRecipeUI(type);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
    }

    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, gt::setupUI);
    }

    private WidgetGroup layout() {
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        Widget fluid = FluidRecipeCapability.CAP.createWidget();
        fluid.setSelfPosition(new Position(8, 4));
        fluid.setBackground(getOverlaysForSlot(false, FluidRecipeCapability.CAP, false, false, false));
        fluid.setId(FluidRecipeCapability.CAP.slotName(IO.IN, 0));
        group.addWidget(fluid);
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, 34, 4, 20, 18, getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);

        group.addWidget(Labels.of(8, 28, "Sails in the swarm (slide):"));
        var slider = new SliderWidget(value -> sails = (int) Math.round(value), 8, 40, WIDTH - 16, 10);
        slider.setRange(0, MAX_SAILS);
        slider.setValue(MAX_SAILS);
        group.addWidget(slider);
        group.addWidget(Labels.live(8, 54, () -> grouped(sails) + " sails of one tier give:"));
        for (int i = 0; i < TIER_NAMES.length; i++) {
            int tier = i;
            group.addWidget(Labels.live(8, 66 + 11 * i,
                    () -> TIER_NAMES[tier] + ": " + grouped(Math.round(sails * EU_PER_SAIL * YIELD[tier])) + " EU/t"));
        }
        return group;
    }

    private static String grouped(long number) {
        return String.format("%,d", number);
    }
}
