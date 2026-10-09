package com.af9.core.litho;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.IEditableUI;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ResearchData;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.common.base.Suppliers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The lithography recipes' page in EMI / JEI, laid out like an assembly line: the items on top (blank wafer, reticle,
 * light source or resist cartridge) and the track chemicals below in rows of four, every row piped into a manifold that
 * feeds the machine (its controller, drawn large in a frame of the node's colour, the node above it and the machine
 * below), then GT's arrow and the outputs stacked (the printed wafers, the chanced broken wafer). The pipes carry
 * dashes flowing to the machine, each fluid row's in the colours of its fluids ({@link LithoFlowWidget}). The machine
 * sits level with the first track row, so that row's pipe runs straight on into it; its name has the room under it,
 * clear of the slots.
 * <p>
 * A researched print (1 nm) shows its research: the data orb the Research Station writes (GT's own, with the recipe's
 * research on it, as GT's assembly line shows it), in a row of its own with a data line into the machine; its hover
 * says how to get it.
 * <p>
 * The slots are GT's own (same ids, so GT binds the recipe to them and EMI shows, looks up and moves them as usual);
 * only the template around them is ours. Replaces the recipe types' UI in common setup
 * ({@link #install}), keeping their slot overlays, progress bar and tooltip count.
 */
public class LithoRecipeUI extends GTRecipeTypeUI {

    public static final int WIDTH = 176, HEIGHT = 66;
    /** Layout: slot columns and rows, the manifold, the machine frame, the arrow, the outputs. */
    public static final int SLOTS_X = 4, ITEMS_Y = 4, FLUIDS_Y = 26, PER_ROW = 4, FLUID_ROWS = 2;
    public static final int MANIFOLD_X = 82, BOX_X = 94, BOX_SIZE = 34, ARROW_X = 131, OUT_X = 155;
    /**
     * The machine's line: the frame, the arrow and the outputs centre on it, level with the first track row's pipe, so
     * that pipe runs straight on through the manifold into the machine.
     */
    public static final int CENTER_Y = FLUIDS_Y + 9;
    public static final String FLOW_ID = "af9_litho_flow", RESEARCH_ID = "af9_litho_research";
    /** The research: its slot's frame and data line. */
    public static final int DATA = 0xFF38BDF8, DATA_DARK = 0xFF0B1E2E;

    private final GTRecipeType type;
    private final LithoMode mode;

    public LithoRecipeUI(GTRecipeType type, LithoMode mode) {
        super(type);
        this.type = type;
        this.mode = mode;
    }

    /** Gives the recipe type this page, keeping what its KubeJS definition set on GT's. */
    public static void install(GTRecipeType type, LithoMode mode) {
        GTRecipeTypeUI old = type.getRecipeUI();
        LithoRecipeUI ui = new LithoRecipeUI(type, mode);
        ui.setSlotOverlays(old.getSlotOverlays());
        ui.setProgressBarTexture(old.getProgressBarTexture());
        ui.setMaxTooltips(old.getMaxTooltips());
        type.setRecipeUI(ui);
    }

    /** The research row's top: under the track's rows (on the dry 1 nm page, the second track row). */
    public static int researchY(int fluids) {
        return FLUIDS_Y + 18 * Math.max(1, (fluids + PER_ROW - 1) / PER_ROW);
    }

    /** Our layout; GT's binding of the recipe to the slots (by their ids) and the arrow, then the research. */
    @Override
    public IEditableUI<WidgetGroup, RecipeHolder> createEditableUITemplate(boolean isSteam, boolean isHighPressure) {
        IEditableUI<WidgetGroup, RecipeHolder> gt = super.createEditableUITemplate(isSteam, isHighPressure);
        return new IEditableUI.Normal<>(this::layout, (group, holder) -> {
            gt.setupUI(group, holder);
            bindResearch(group, holder);
        });
    }

    private WidgetGroup layout() {
        int items = Math.min(PER_ROW, type.maxInputs.getInt(ItemRecipeCapability.CAP));
        int fluids = Math.min(PER_ROW * FLUID_ROWS, type.maxInputs.getInt(FluidRecipeCapability.CAP));
        int outputs = Math.min(2, type.maxOutputs.getInt(ItemRecipeCapability.CAP));
        int research = mode.needsResearch() ? researchY(fluids) : -1;
        int height = research < 0 ? HEIGHT : Math.max(HEIGHT, research + 22);
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, height);
        // the pipes and the machine first: the slots draw over them
        LithoFlowWidget flow = new LithoFlowWidget(mode, items, fluids, research, height);
        flow.setId(FLOW_ID);
        group.addWidget(flow);
        for (int i = 0; i < items; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.IN, i, items, SLOTS_X + 18 * i, ITEMS_Y);
        }
        for (int i = 0; i < fluids; i++) {
            slot(group, FluidRecipeCapability.CAP, IO.IN, i, fluids, SLOTS_X + 18 * (i % PER_ROW),
                    FLUIDS_Y + 18 * (i / PER_ROW));
        }
        if (fluids == 0) {
            // the dry process note's hover (the flow draws the note)
            var dry = new Widget(SLOTS_X, FLUIDS_Y, MANIFOLD_X - 2 - SLOTS_X, 16);
            dry.setHoverTooltips(Component.translatable("af9.recipe.litho_page.dry_hover"));
            group.addWidget(dry);
        }
        if (research >= 0) {
            // the data orb: its own id, so GT's binding leaves it to bindResearch
            SlotWidget orb = (SlotWidget) ItemRecipeCapability.CAP.createWidget();
            orb.setSelfPosition(new Position(SLOTS_X, research));
            orb.setBackground(new GuiTextureGroup(new ColorRectTexture(DATA_DARK), new ColorBorderTexture(1, DATA)));
            orb.setId(RESEARCH_ID);
            group.addWidget(orb);
        }
        for (int i = 0; i < outputs; i++) {
            slot(group, ItemRecipeCapability.CAP, IO.OUT, i, outputs, OUT_X,
                    CENTER_Y - 9 - (outputs - 1) * 10 + 20 * i);
        }
        var arrow = new ProgressWidget(ProgressWidget.JEIProgress, ARROW_X, CENTER_Y - 10, 20, 20,
                getProgressBarTexture());
        arrow.setId("progress");
        group.addWidget(arrow);
        // the machine frame's tooltip: which machine prints this node
        var machine = new Widget(BOX_X, CENTER_Y - BOX_SIZE / 2, BOX_SIZE, BOX_SIZE);
        List<Component> hover = new ArrayList<>();
        hover.add(Component.translatable("block.gtceu." + LithoFlowWidget.machineId(mode)));
        hover.add(Component.translatable("af9.recipe.litho_page.node", mode.nodeNm,
                Component.translatable("af9.litho.light." + mode.light)));
        if (mode.mk2Only()) hover.add(Component.translatable("af9.recipe.litho_page.mk2_only"));
        machine.setHoverTooltips(hover);
        group.addWidget(machine);
        return group;
    }

    private void slot(WidgetGroup group, RecipeCapability<?> cap, IO io, int index, int count, int x, int y) {
        Widget slot = cap.createWidget();
        slot.setSelfPosition(new Position(x, y));
        slot.setBackground(getOverlaysForSlot(io == IO.OUT, cap, index == count - 1, false, false));
        slot.setId(cap.slotName(io, index));
        group.addWidget(slot);
    }

    /**
     * The research slot shows the recipe's data orbs, written with its research the way GT's research slot writes them
     * (a catalyst: EMI lists the recipe under the orb's uses); hidden for a recipe without research. Its hover names the
     * research as its Research Station recipe does it: the item scanned and the data item, both in the Object Holder,
     * the power, the computation and the time.
     */
    private void bindResearch(WidgetGroup group, RecipeHolder holder) {
        List<ResearchData.ResearchEntry> entries = researchEntries(holder.conditions());
        List<ItemStack> orbs = new ArrayList<>();
        for (ResearchData.ResearchEntry entry : entries) {
            ItemStack orb = entry.getDataItem().copy();
            ResearchManager.writeResearchToNBT(orb.getOrCreateTag(), entry.getResearchId(), type);
            orbs.add(orb);
        }
        // looked up on the first hover (the recipes the client has)
        Supplier<ResearchInfo> info = Suppliers.memoize(() -> entries.isEmpty() ? null :
                ResearchInfo.find(entries.get(0)));
        WidgetUtils.widgetByIdForEach(group, "^" + RESEARCH_ID + "$", SlotWidget.class, slot -> {
            if (orbs.isEmpty()) {
                slot.setVisible(false);
                slot.setActive(false);
                return;
            }
            slot.setHandlerSlot(new CycleItemStackHandler(List.of(orbs)), 0);
            slot.setIngredientIO(IngredientIO.CATALYST);
            slot.setCanTakeItems(false);
            slot.setCanPutItems(false);
            slot.setOnAddedTooltips((widget, tooltips) -> {
                tooltips.add(Component.literal("\u25C8 ").append(
                        Component.translatable("af9.recipe.litho_page.research"))
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                ResearchInfo research = info.get();
                if (research != null) {
                    tooltips.add(Component.translatable("af9.recipe.litho_page.research_station",
                            Component.translatable("block.gtceu.research_station").withStyle(ChatFormatting.WHITE),
                            research.scanned().getHoverName().copy().withStyle(ChatFormatting.WHITE),
                            research.data().getHoverName().copy().withStyle(ChatFormatting.WHITE),
                            Component.translatable("block.gtceu.object_holder")).withStyle(ChatFormatting.GRAY));
                    tooltips.add(Component.translatable("af9.recipe.litho_page.research_cost",
                            FormattingUtil.formatNumbers(research.amperage()), GTValues.VNF[research.tier()],
                            FormattingUtil.formatNumbers(research.eut()),
                            FormattingUtil.formatNumbers(research.cwut()),
                            FormattingUtil.formatNumbers(research.totalCwu()),
                            FormattingUtil.formatNumbers(research.seconds())).withStyle(ChatFormatting.GRAY));
                } else {
                    tooltips.add(Component.translatable("af9.recipe.litho_page.research_scan")
                            .withStyle(ChatFormatting.GRAY));
                }
                tooltips.add(Component.translatable("af9.recipe.litho_page.research_hatch")
                        .withStyle(ChatFormatting.DARK_AQUA));
            });
        });
    }

    /** The recipe's research entries (none when GT's research is off). */
    private static List<ResearchData.ResearchEntry> researchEntries(List<? extends RecipeCondition<?>> conditions) {
        if (conditions == null || !ConfigHolder.INSTANCE.machines.enableResearch) return List.of();
        List<ResearchData.ResearchEntry> entries = new ArrayList<>();
        for (RecipeCondition<?> condition : conditions) {
            if (!(condition instanceof ResearchCondition research) || research.data == null) continue;
            for (ResearchData.ResearchEntry entry : research.data) entries.add(entry);
        }
        return entries;
    }

    /**
     * A research as its Research Station recipe does it: the item scanned, the data item written, the voltage and amps,
     * the computation per tick and in all.
     */
    private record ResearchInfo(ItemStack scanned, ItemStack data, long voltage, long amperage, int cwut,
                                int totalCwu) {

        long eut() {
            return voltage * amperage;
        }

        int tier() {
            return Math.min(GTUtil.getTierByVoltage(voltage), GTValues.VNF.length - 1);
        }

        /** Seconds at exactly the recipe's CWU/t. */
        long seconds() {
            return (long) Math.ceil(totalCwu / (double) Math.max(1, cwut) / 20.0);
        }

        /**
         * The Research Station recipe that writes the research (client: the recipes it has; found by the research on its
         * output data item, or by its id, research_station/&lt;research id&gt;), or null.
         */
        @OnlyIn(Dist.CLIENT)
        static ResearchInfo find(ResearchData.ResearchEntry entry) {
            var level = Minecraft.getInstance().level;
            if (level == null) return null;
            String researchId = entry.getResearchId();
            for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(GTRecipeTypes.RESEARCH_STATION_RECIPES)) {
                if (!recipe.getId().getPath().endsWith("/" + researchId) && !writes(recipe, researchId)) continue;
                ItemStack scanned = ItemStack.EMPTY;
                for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                    for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                        if (!stack.is(entry.getDataItem().getItem())) {
                            scanned = stack;
                            break;
                        }
                    }
                    if (!scanned.isEmpty()) break;
                }
                EnergyStack energy = RecipeHelper.getRealEUt(recipe);
                int cwut = 0;
                for (Content content : recipe.tickInputs.getOrDefault(CWURecipeCapability.CAP, List.of())) {
                    cwut += CWURecipeCapability.CAP.of(content.content);
                }
                // a research's duration is its total computation (GT's totalCWU)
                int total = recipe.data.getBoolean("duration_is_total_cwu") ? recipe.duration : recipe.duration * cwut;
                return new ResearchInfo(scanned, entry.getDataItem(), energy.voltage(), energy.amperage(), cwut,
                        total);
            }
            return null;
        }

        /** Whether the recipe's output data item carries the research. */
        private static boolean writes(GTRecipe recipe, String researchId) {
            for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
                for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                    ResearchManager.ResearchItem research = ResearchManager.readResearchId(stack);
                    if (research != null && researchId.equals(research.researchId())) return true;
                }
            }
            return false;
        }
    }

    /** The recipe to the pipes (its fluids' colours, whether it is researched), then the type's own page builder. */
    @Override
    public void appendJEIUI(GTRecipe recipe, WidgetGroup widgetGroup) {
        WidgetUtils.widgetByIdForEach(widgetGroup, "^" + FLOW_ID + "$", LithoFlowWidget.class,
                flow -> flow.setRecipe(recipe));
        super.appendJEIUI(recipe, widgetGroup);
    }
}
