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
 * Controller logic of the Photolithography Line (structure and recipes are defined in KubeJS).
 * <p>
 * One recipe type per line {@link LithoMode} (350 nm on silicon ... 7 nm on strange matter); the active one is GT's
 * machine mode, switchable with GT's mode tab or the console's mode tiles. Vacuum, break roll and counters come from
 * {@link LithoMachine}.
 * <p>
 * Versions 1-8, like the Assembly Line's length: every version has one more projection-lens slice (3 to 10) and needs
 * a light source that allows it (mercury lamp V1, KrF excimer laser V2, ArF excimer laser up to V6, EUV source up to
 * V8). A version runs the modes up to its own level; lower modes run faster and break less.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends LithoMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, LithoMachine.MANAGED_FIELD_HOLDER);

    /** One lens slice more per version: version 1 has 3. */
    public static final int LENS_SLICES_V1 = 3;
    private static final ResourceLocation LENS_BLOCK = new ResourceLocation("gtceu", "tempered_glass");
    /** Light sources in version order; each allows versions up to its {@link #LIGHT_CAPS} entry. */
    private static final ResourceLocation[] LIGHT_SOURCES = {
            new ResourceLocation("gtceu", "purple_lamp"),
            new ResourceLocation("kubejs", "krf_excimer_laser"),
            new ResourceLocation("kubejs", "arf_excimer_laser"),
            new ResourceLocation("kubejs", "euv_light_source") };
    private static final int[] LIGHT_CAPS = { 1, 2, 6, 8 };
    /** Share of the missing cleanliness the line's pumps recover per second. */
    public static final double PUMP_RATE = 0.08;

    /**
     * A line above the mode's level runs it faster ({@link LithoMode#speedFactor}); the lower break chance is part of
     * the break roll. Before the overclock.
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

    // rebuilt on every structure check
    private int version;

    public PhotolithographyLineMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public List<LithoMode> getModes() {
        return LithoMode.LINE_MODES;
    }

    @Override
    public boolean canPrint(LithoMode mode) {
        return !mode.isOrbital() && mode.level() <= getVersion();
    }

    @Override
    public int surplusFor(LithoMode mode) {
        return Math.max(0, getVersion() - mode.level());
    }

    @Override
    protected double pumpRate() {
        return PUMP_RATE;
    }

    @Override
    public String titleKey() {
        return "af9.litho.console.title";
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

    /** min(lens slices - 2, light source cap), 1-8; 0 while not formed. */
    @Override
    public int getVersion() {
        return isFormed() ? version : 0;
    }

    private int detectVersion() {
        Level level = getLevel();
        if (level == null) return 1;
        Block lens = ForgeRegistries.BLOCKS.getValue(LENS_BLOCK);
        int lensSlices = 0;
        int lightCap = 1;
        for (BlockPos pos : getMultiblockState().getCache()) {
            Block block = level.getBlockState(pos).getBlock();
            if (block == lens) {
                lensSlices++;
                continue;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            for (int i = 0; i < LIGHT_SOURCES.length; i++) {
                if (LIGHT_SOURCES[i].equals(id)) lightCap = LIGHT_CAPS[i];
            }
        }
        int byLens = lensSlices - LENS_SLICES_V1 + 1;
        return Math.max(1, Math.min(LithoMode.MAX_VERSION, Math.min(byLens, lightCap)));
    }

    /** Light source a version needs at least (the preview pages show it). */
    public static ResourceLocation lightSourceFor(int version) {
        for (int i = 0; i < LIGHT_CAPS.length; i++) {
            if (version <= LIGHT_CAPS[i]) return LIGHT_SOURCES[i];
        }
        return LIGHT_SOURCES[LIGHT_SOURCES.length - 1];
    }

    /**
     * Structure preview pages, one per version like the Assembly Line's lengths: the lens slices GT would draw for that
     * repeat count, with the version's light source in place of the lamp.
     */
    public static List<MultiblockShapeInfo> versionShapes(MultiblockMachineDefinition definition) {
        BlockPattern pattern = definition.getPatternFactory().get();
        int[][] repetitions = pattern.aisleRepetitions;
        int lensAisle = -1;
        for (int i = 0; i < repetitions.length; i++) {
            if (repetitions[i][1] > repetitions[i][0]) lensAisle = i;
        }
        Block lamp = ForgeRegistries.BLOCKS.getValue(LIGHT_SOURCES[0]);
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        for (int version = 1; version <= LithoMode.MAX_VERSION; version++) {
            int[] repetition = new int[repetitions.length];
            for (int i = 0; i < repetitions.length; i++) repetition[i] = repetitions[i][0];
            if (lensAisle >= 0) {
                repetition[lensAisle] = Math.min(repetitions[lensAisle][1],
                        repetitions[lensAisle][0] + version - 1);
            }
            BlockInfo[][][] blocks = pattern.getPreview(repetition);
            Block light = ForgeRegistries.BLOCKS.getValue(lightSourceFor(version));
            if (light != null && light != Blocks.AIR && light != lamp) {
                BlockInfo lightInfo = BlockInfo.fromBlockState(light.defaultBlockState());
                for (BlockInfo[][] slice : blocks) {
                    for (BlockInfo[] row : slice) {
                        for (int k = 0; k < row.length; k++) {
                            if (row[k] != null && row[k].getBlockState().getBlock() == lamp) row[k] = lightInfo;
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

    /** Adds node, light source, line version and break chances to the lithography recipes in EMI/JEI. */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            // rendered as plain labels, so the texts must not contain '%'
            if (mode.isOrbital()) {
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node_orbital", mode.nodeNm,
                        Component.translatable("af9.litho.light." + mode.light)).getString());
            } else {
                type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node", mode.nodeNm,
                        Component.translatable("af9.litho.light." + mode.light), mode.level()).getString());
            }
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_break",
                    String.format(java.util.Locale.ROOT, "%.0f", mode.baseBreak / 100.0),
                    String.format(java.util.Locale.ROOT, "%.0f", (mode.baseBreak / 10000.0 + LithoMode.DIRT_BREAK) *
                            100)).getString());
        }
    }
}
