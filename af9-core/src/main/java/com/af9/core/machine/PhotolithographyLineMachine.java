package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.Coolant;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.BusPlacardWidget;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.ResearchManager;

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
 * Versions, like the Assembly Line's length: every version has one more lens slice (Mk1: a tempered-glass lens
 * element; Mk2: a window section of cleanroom glass) and, on the Mk1, a light source block that allows it. A version
 * runs the modes up to its own level; lower modes run faster and break less. The Mk2 keeps its ArF laser in a slot of
 * its screen instead ({@link PhotolithographyScannerMachine}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends LithoMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, LithoMachine.MANAGED_FIELD_HOLDER);

    /**
     * What makes a versioned lithography machine.
     *
     * @param modes           the modes, in version order (version n runs the first n)
     * @param lensBlock       the lens block the version is counted by
     * @param lensPerSlice    lens blocks in one lens slice
     * @param lensSlicesV1    lens slices of version 1 (one more per version)
     * @param aislesPerSlice  aisles of the repeatable lens aisle one slice takes (the preview pages' length step)
     * @param lights          light source blocks, lowest first; none: the light source is not part of the structure
     * @param lightCaps       highest version each light source allows
     * @param vacuumLevelBase vacuum level of "version 0": the pump-down takes 10 s x (base + version)
     * @param titleKey        console title
     */
    public record Spec(List<LithoMode> modes, ResourceLocation lensBlock, int lensPerSlice, int lensSlicesV1,
                       int aislesPerSlice, ResourceLocation[] lights, int[] lightCaps, int vacuumLevelBase,
                       String titleKey) {

        public int maxVersion() {
            return modes.size();
        }

        /** Whether the light source is a block of the structure (and caps the version). */
        public boolean hasLightBlocks() {
            return lights.length > 0;
        }

        /** Light source a version needs at least (the preview pages show it); only with light blocks. */
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
            3, 1, new ResourceLocation[] { PURPLE_LAMP, KRF_LASER, ARF_LASER }, new int[] { 1, 2, 3 }, 0,
            "af9.litho.console.title");
    /**
     * Mk2, the Photolithography Scanner: 80 and 65 nm. A cleanroom tube 3 x 3, 10 long at version 1 and 12 at version
     * 2: its window sections (2 aisles of cleanroom glass, 4 blocks) are the slices, 2 at version 1 and one more per
     * version, the back window run repeating 2 aisles a version. Its ArF excimer laser sits in its screen's slot, not in
     * the structure. Vacuum levels 4-5 (it continues after the line's 3).
     */
    public static final Spec MK2 = new Spec(LithoMode.SCANNER_MODES,
            new ResourceLocation("gtceu", "cleanroom_glass"), 4, 2, 2, new ResourceLocation[0], new int[0], 3,
            "af9.scanner.console.title");

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
        // a version above the mode and every doubling of the air conditioning above its heat load speed it up
        double factor = LithoMode.speedFactor(line.surplusFor(mode)) *
                Math.pow(Coolant.TIME_FACTOR, line.coolingSteps(mode));
        // a mode one version above the machine's own, exposed twice (multi-patterning)
        if (line.isMultiPatterned(mode)) factor *= LithoMode.MULTI_PATTERNING_TIME;
        if (Math.abs(factor - 1) < 1e-9) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().durationMultiplier(factor).build();
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
        // multi-patterning lets a machine run the mode one version above its own
        return spec.modes().contains(mode) && getVersion() > 0 &&
                mode.level() <= getVersion() + (isMultiPatterning() ? 1 : 0);
    }

    @Override
    public boolean canMultiPattern() {
        return true;
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

    /** min(lens slices - (V1 slices - 1), light source cap), 1 to the last version; 0 while not formed. Without light
     * blocks the lens slices alone. */
    @Override
    public int getVersion() {
        return isFormed() ? version : 0;
    }

    private int detectVersion() {
        Level level = getLevel();
        if (level == null) return 1;
        Block lens = ForgeRegistries.BLOCKS.getValue(spec.lensBlock());
        int lensBlocks = 0;
        int lightCap = spec.hasLightBlocks() ? 1 : spec.maxVersion();
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
     * repeat count ({@link Spec#aislesPerSlice()} aisles a version), with the version's light source in place of the
     * first one (machines with light blocks).
     */
    public static List<MultiblockShapeInfo> versionShapes(MultiblockMachineDefinition definition, Spec spec) {
        BlockPattern pattern = definition.getPatternFactory().get();
        int[][] repetitions = pattern.aisleRepetitions;
        int lensAisle = -1;
        for (int i = 0; i < repetitions.length; i++) {
            if (repetitions[i][1] > repetitions[i][0]) lensAisle = i;
        }
        Block firstLight = spec.hasLightBlocks() ? ForgeRegistries.BLOCKS.getValue(spec.lights()[0]) : null;
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        for (int version = 1; version <= spec.maxVersion(); version++) {
            int[] repetition = new int[repetitions.length];
            for (int i = 0; i < repetitions.length; i++) repetition[i] = repetitions[i][0];
            if (lensAisle >= 0) {
                repetition[lensAisle] = Math.min(repetitions[lensAisle][1],
                        repetitions[lensAisle][0] + (version - 1) * spec.aislesPerSlice());
            }
            BlockInfo[][][] blocks = pattern.getPreview(repetition);
            Block light = firstLight == null ? null : ForgeRegistries.BLOCKS.getValue(spec.lightSourceFor(version));
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
        return BusPlacardWidget.wrap(LithoConsoleWidget.create(this), this);
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /**
     * Gives the lithography recipes their own EMI/JEI page ({@link com.af9.core.litho.LithoRecipeUI}: items and track
     * chemicals piped into the machine, like an assembly line; a 1 nm print's research in its own slot) and adds node,
     * light source, machine version and the coolant to it, short enough for the page's width (no break chance: the
     * chanced broken wafer shows it). The recipes with computation also get GT's "Min. Computation" line and (1 nm)
     * its "Requires Research" line: the page is made one line taller for each, and {@link #respaceTexts} fixes GT
     * putting both on the same row.
     * <p>
     * A researched node's type (1 nm) also makes the Research Station recipes: a recipe's {@code stationResearch} only
     * becomes a Research Station recipe (reticle + empty data orb into the orb with the research) through its type's
     * build hook, which GT sets on its own assembly line type only. Before the recipes load (common setup), so the
     * KubeJS recipes, built from the type's builder, carry it.
     */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            if (mode.needsResearch()) type.onRecipeBuild(ResearchManager::createDefaultResearchRecipe);
            // first: the settings below go to the page's UI
            com.af9.core.litho.LithoRecipeUI.install(type, mode);
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
            if (mode.minCoolant() != null) {
                // the fluids by their full names (Supercooled Argon ...), not just the gas
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_coolant",
                        mode.coolantPerPrint()).getString());
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_coolant_min",
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
