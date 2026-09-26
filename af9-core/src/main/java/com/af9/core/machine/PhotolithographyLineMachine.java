package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller logic of the versioned lithography machines (structures and recipes are defined in KubeJS): the
 * Photolithography Line (Mk1, {@link #MK1}) and the Photolithography Scanner (Mk2, {@link #MK2}).
 * <p>
 * One recipe type per {@link LithoMode}; the active one is GT's machine mode (GT's mode tab). Vacuum, break roll and
 * counters come from {@link LithoMachine}.
 * <p>
 * Versions, like the Assembly Line's length: every version has one more projection-lens slice and needs a light source
 * that allows it. A version runs the modes up to its own level; lower modes run faster and break less.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends LithoMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, LithoMachine.MANAGED_FIELD_HOLDER);

    /**
     * What makes a versioned lithography machine.
     *
     * @param modes           the modes, in version order (version n runs the first n)
     * @param lensBlock       the projection-lens block; only the lens aisles hold it
     * @param lensPerSlice    lens blocks in one lens slice
     * @param lensSlicesV1    lens slices of version 1 (one more per version)
     * @param lights          light sources, lowest first
     * @param lightCaps       highest version each light source allows
     * @param vacuumLevelBase vacuum level of "version 0": the pump-down takes 10 s x (base + version)
     * @param titleKey        console title
     */
    public record Spec(List<LithoMode> modes, ResourceLocation lensBlock, int lensPerSlice, int lensSlicesV1,
                       ResourceLocation[] lights, int[] lightCaps, int vacuumLevelBase, String titleKey) {

        public int maxVersion() {
            return modes.size();
        }

        /** Light source a version needs at least (the preview pages show it). */
        public ResourceLocation lightSourceFor(int version) {
            for (int i = 0; i < lightCaps.length; i++) {
                if (version <= lightCaps[i]) return lights[i];
            }
            return lights[lights.length - 1];
        }
    }

    private static final ResourceLocation PURPLE_LAMP = new ResourceLocation("gtceu", "purple_lamp");
    private static final ResourceLocation KRF_LASER = new ResourceLocation("kubejs", "krf_excimer_laser");
    private static final ResourceLocation ARF_LASER = new ResourceLocation("kubejs", "arf_excimer_laser");

    /**
     * Mk1, the Photolithography Line: 350, 200 and 100 nm. 3-5 tempered-glass lens slices; mercury lamp (GT's purple
     * lamp) V1, KrF excimer laser V2, ArF excimer laser V3. Vacuum levels 1-3.
     */
    public static final Spec MK1 = new Spec(LithoMode.LINE_MODES, new ResourceLocation("gtceu", "tempered_glass"), 1,
            3, new ResourceLocation[] { PURPLE_LAMP, KRF_LASER, ARF_LASER }, new int[] { 1, 2, 3 }, 0,
            "af9.litho.console.title");
    /**
     * Mk2, the Photolithography Scanner: 80 and 65 nm. 4-5 lens slices of 6 laminated glass each; ArF excimer laser.
     * Vacuum levels 4-5 (it continues after the line's 3).
     */
    public static final Spec MK2 = new Spec(LithoMode.SCANNER_MODES,
            new ResourceLocation("gtceu", "laminated_glass"), 6, 4, new ResourceLocation[] { ARF_LASER },
            new int[] { 2 }, 3, "af9.scanner.console.title");

    /**
     * A machine above the mode's level runs it faster ({@link LithoMode#speedFactor}); the lower break chance is part
     * of the break roll. Before the overclock.
     */
    public static final RecipeModifier LITHO_VERSION = (machine, recipe) -> {
        if (!(machine instanceof PhotolithographyLineMachine line)) {
            return RecipeModifier.nullWrongType(PhotolithographyLineMachine.class, machine);
        }
        LithoMode mode = LithoMode.of(recipe.recipeType);
        if (mode == null) return ModifierFunction.IDENTITY;
        int surplus = line.surplusFor(mode);
        if (surplus <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().durationMultiplier(LithoMode.speedFactor(surplus)).build();
    };

    private final Spec spec;
    // rebuilt on every structure check
    private int version;

    /** The Mk1 line (the KubeJS definition of photolithography_line). */
    public PhotolithographyLineMachine(IMachineBlockEntity holder) {
        this(holder, MK1);
    }

    public PhotolithographyLineMachine(IMachineBlockEntity holder, Spec spec) {
        super(holder);
        this.spec = spec;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public Spec getSpec() {
        return spec;
    }

    @Override
    public List<LithoMode> getModes() {
        return spec.modes();
    }

    @Override
    public boolean canPrint(LithoMode mode) {
        return spec.modes().contains(mode) && mode.level() <= getVersion();
    }

    @Override
    public int surplusFor(LithoMode mode) {
        return Math.max(0, getVersion() - mode.level());
    }

    /** The bigger the machine, the longer the pump-down: 10 s per level. */
    @Override
    protected int vacuumLevel() {
        return spec.vacuumLevelBase() + getVersion();
    }

    @Override
    public String titleKey() {
        return spec.titleKey();
    }

    //////////////////////////////////////
    // ********** Version ***********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        version = detectVersion();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        version = 0;
    }

    /** min(lens slices - (V1 slices - 1), light source cap), 1 to the last version; 0 while not formed. */
    @Override
    public int getVersion() {
        return isFormed() ? version : 0;
    }

    private int detectVersion() {
        Level level = getLevel();
        if (level == null) return 1;
        Block lens = ForgeRegistries.BLOCKS.getValue(spec.lensBlock());
        int lensBlocks = 0;
        int lightCap = 1;
        for (BlockPos pos : getMultiblockState().getCache()) {
            Block block = level.getBlockState(pos).getBlock();
            if (block == lens) {
                lensBlocks++;
                continue;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            for (int i = 0; i < spec.lights().length; i++) {
                if (spec.lights()[i].equals(id)) lightCap = Math.max(lightCap, spec.lightCaps()[i]);
            }
        }
        int slices = lensBlocks / Math.max(1, spec.lensPerSlice());
        int byLens = slices - spec.lensSlicesV1() + 1;
        return Math.max(1, Math.min(spec.maxVersion(), Math.min(byLens, lightCap)));
    }

    /** Structure preview pages of the Mk1 line. */
    public static List<MultiblockShapeInfo> versionShapes(MultiblockMachineDefinition definition) {
        return versionShapes(definition, MK1);
    }

    /**
     * Structure preview pages, one per version like the Assembly Line's lengths: the lens slices GT would draw for that
     * repeat count, with the version's light source in place of the first one.
     */
    public static List<MultiblockShapeInfo> versionShapes(MultiblockMachineDefinition definition, Spec spec) {
        BlockPattern pattern = definition.getPatternFactory().get();
        int[][] repetitions = pattern.aisleRepetitions;
        int lensAisle = -1;
        for (int i = 0; i < repetitions.length; i++) {
            if (repetitions[i][1] > repetitions[i][0]) lensAisle = i;
        }
        Block firstLight = ForgeRegistries.BLOCKS.getValue(spec.lights()[0]);
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        for (int version = 1; version <= spec.maxVersion(); version++) {
            int[] repetition = new int[repetitions.length];
            for (int i = 0; i < repetitions.length; i++) repetition[i] = repetitions[i][0];
            if (lensAisle >= 0) {
                repetition[lensAisle] = Math.min(repetitions[lensAisle][1],
                        repetitions[lensAisle][0] + version - 1);
            }
            BlockInfo[][][] blocks = pattern.getPreview(repetition);
            Block light = ForgeRegistries.BLOCKS.getValue(spec.lightSourceFor(version));
            if (light != null && light != Blocks.AIR && light != firstLight) {
                BlockInfo lightInfo = BlockInfo.fromBlockState(light.defaultBlockState());
                for (BlockInfo[][] slice : blocks) {
                    for (BlockInfo[] row : slice) {
                        for (int k = 0; k < row.length; k++) {
                            if (row[k] != null && row[k].getBlockState().getBlock() == firstLight) row[k] = lightInfo;
                        }
                    }
                }
            }
            pages.add(new MultiblockShapeInfo(blocks));
        }
        return pages;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** Replaces GT's text display with the console; GT's side tabs (power, mode, parts) stay. */
    @Override
    public Widget createUIWidget() {
        return LithoConsoleWidget.create(this);
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /**
     * Adds node, light source, machine version, break chances and the coolant to the lithography recipes in EMI/JEI,
     * short enough for the page's width. The recipes with computation also get GT's "Min. Computation" line and (1 nm)
     * its "Requires Research" line: the page is made one line taller for each, and {@link #respaceTexts} fixes GT
     * putting both on the same row.
     */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            // rendered as plain labels, so the texts must not contain '%'
            Component light = Component.translatable("af9.litho.light." + mode.light);
            switch (mode.machine) {
                case ORBITAL -> type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node_orbital",
                        mode.nodeNm, light).getString());
                case SCANNER -> type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node_scanner",
                        mode.nodeNm, light, mode.level()).getString());
                default -> type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node", mode.nodeNm,
                        light, mode.level()).getString());
            }
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_break",
                    String.format(java.util.Locale.ROOT, "%.0f", mode.baseBreak / 100.0),
                    String.format(java.util.Locale.ROOT, "%.0f", (mode.baseBreak / 10000.0 + LithoMode.DIRT_BREAK) *
                            100)).getString());
            if (mode.minCoolant() != null) {
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_coolant", mode.coolantPerPrint(),
                        Component.translatable("af9.litho.coolant." + mode.minCoolant().id)).getString());
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_coolant_best",
                        Component.translatable("af9.litho.coolant." + mode.bestCoolant().id)).getString());
            }
            if (mode.computation() > 0) {
                // Duration, Total and Usage are GT's 3 default lines; + Min. Computation
                type.setMaxTooltips(4);
                if (mode.needsResearch()) type.setMinRecipeConditions(1);
                type.setUiBuilder(PhotolithographyLineMachine::respaceTexts);
            }
        }
    }

    /**
     * GT's recipe page writes the computation line and the first condition line on the same row (two counters for one
     * column). GT (re)builds the recipe's content (on opening, and again on every overclock-tier click) as a widget
     * group followed by the computation, condition and data lines: stack the lines after the last group one per row
     * again, in the order GT added them.
     */
    private static void respaceTexts(com.gregtechceu.gtceu.api.recipe.GTRecipe recipe,
                                     com.lowdragmc.lowdraglib.gui.widget.WidgetGroup page) {
        List<LabelWidget> labels = new ArrayList<>();
        for (Widget widget : page.widgets) {
            if (widget instanceof com.lowdragmc.lowdraglib.gui.widget.WidgetGroup) labels.clear();
            else if (widget instanceof LabelWidget label) labels.add(label);
        }
        if (labels.isEmpty()) return;
        int top = Integer.MAX_VALUE;
        for (LabelWidget label : labels) top = Math.min(top, label.getSelfPosition().y);
        for (int i = 0; i < labels.size(); i++) labels.get(i).setSelfPositionY(top + i * 10);
    }
}
