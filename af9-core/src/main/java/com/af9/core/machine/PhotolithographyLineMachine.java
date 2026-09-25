package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller logic for the Photolithography Line (structure and recipes are defined in KubeJS).
 * <p>
 * The line has one recipe type per {@link LithoMode}; the active one is GT's machine mode, switchable with GT's mode
 * tab or the console's mode tiles ({@link LithoConsoleWidget}). What it prints are wafer packages that carry the
 * mode's node, the line version and the transistor count as NBT (see {@link LithoMode#TAG}).
 * <p>
 * Versions 1-5, like the Assembly Line's length: every version has one more projection-lens slice (3 to 7) and needs
 * the matching light source (mercury lamp up to version 1, KrF excimer laser up to 3, ArF excimer laser up to 5). A
 * version runs the modes up to its own level; for lower modes it is faster, yields more and shrinks the dies further.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** One lens slice more per version: version 1 has 3. */
    public static final int LENS_SLICES_V1 = 3;
    private static final ResourceLocation LENS_BLOCK = new ResourceLocation("gtceu", "tempered_glass");
    /** Light sources in version order; each allows versions up to {@link #LIGHT_CAPS}. */
    private static final ResourceLocation[] LIGHT_SOURCES = {
            new ResourceLocation("gtceu", "purple_lamp"),
            new ResourceLocation("kubejs", "krf_excimer_laser"),
            new ResourceLocation("kubejs", "arf_excimer_laser") };
    private static final int[] LIGHT_CAPS = { 1, 3, 5 };

    /**
     * Only starts a recipe the line's version is high enough for, and only when the energy hatches can actually supply
     * its EU/t (GT's own voltage check would let two hatches of the tier below start the recipe and then starve).
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof PhotolithographyLineMachine line)) {
            return RecipeModifier.nullWrongType(PhotolithographyLineMachine.class, machine);
        }
        LithoMode mode = modeOf(recipe.recipeType);
        if (mode != null && mode.level() > line.getVersion()) return ModifierFunction.NULL;
        if (line.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /**
     * A line above the mode's level: shorter run ({@link LithoMode#speedFactor}), better chance of the bonus packages
     * ({@link LithoMode#yieldBonus}) and more transistors per die ({@link LithoMode#shrinkFactor}), written into the
     * packages together with the line version. Before the overclock.
     */
    public static final RecipeModifier LITHO_VERSION = (machine, recipe) -> {
        if (!(machine instanceof PhotolithographyLineMachine line)) {
            return RecipeModifier.nullWrongType(PhotolithographyLineMachine.class, machine);
        }
        LithoMode mode = modeOf(recipe.recipeType);
        if (mode == null) return ModifierFunction.IDENTITY;
        int version = line.getVersion();
        int surplus = version - mode.level();
        if (surplus <= 0) return ModifierFunction.IDENTITY;
        return modified -> applyVersion(modified, version, surplus);
    };

    // wafer packages printed per mode (separate fields: LDLib persists primitives reliably)
    @Persisted
    private long printedMuv;
    @Persisted
    private long printedHuv;
    @Persisted
    private long printedEuv;
    @Persisted
    private long printedXuv;
    @Persisted
    private long printedLuv;

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
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new LithoRecipeLogic(this);
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

    /** min(lens slices - 2, light source cap), 1-5; 0 while not formed. */
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

    static GTRecipe applyVersion(GTRecipe recipe, int version, int surplus) {
        GTRecipe result = recipe.copy();
        result.duration = Math.max(1, (int) Math.round(recipe.duration * LithoMode.speedFactor(surplus)));
        List<Content> outputs = result.outputs.get(ItemRecipeCapability.CAP);
        if (outputs == null) return result;
        List<Content> changed = new ArrayList<>(outputs.size());
        for (Content content : outputs) {
            Object item = content.content;
            if (item instanceof Ingredient ingredient) item = withVersion(ingredient, version, surplus);
            int chance = content.chance;
            if (content.isChanced()) {
                long bonus = (long) LithoMode.yieldBonus(surplus) * content.maxChance / 10000;
                chance = (int) Math.min(content.maxChance, chance + bonus);
            }
            changed.add(new Content(item, chance, content.maxChance, content.tierChanceBoost));
        }
        result.outputs.put(ItemRecipeCapability.CAP, changed);
        return result;
    }

    /** The printed package with the line's version and the shrunk transistor count; other outputs unchanged. */
    private static Ingredient withVersion(Ingredient ingredient, int version, int surplus) {
        if (!(ingredient instanceof SizedIngredient sized)) return ingredient;
        ItemStack[] stacks = sized.getItems();
        if (stacks.length != 1 || LithoMode.getLithoTag(stacks[0]) == null) return ingredient;
        ItemStack stack = stacks[0].copy();
        CompoundTag litho = stack.getOrCreateTagElement(LithoMode.TAG);
        long transistors = Math.round(litho.getInt(LithoMode.TAG_TRANSISTORS) * LithoMode.shrinkFactor(surplus));
        litho.putInt(LithoMode.TAG_TRANSISTORS, (int) Math.min(Integer.MAX_VALUE, transistors));
        litho.putInt(LithoMode.TAG_VERSION, version);
        return SizedIngredient.create(stack);
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
    // ******* Line state ***********//
    //////////////////////////////////////

    public long getAvailableEUt() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    public static LithoMode modeOf(GTRecipeType type) {
        return type == null ? null : LithoMode.fromRecipeTypePath(type.registryName.getPath());
    }

    public LithoMode getActiveMode() {
        return modeOf(getRecipeType());
    }

    public boolean hasMaintenanceProblems() {
        return getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance &&
                maintenance.hasMaintenanceProblems());
    }

    /** Printed packages per mode, indexed by {@link LithoMode#ordinal()}. */
    public long[] getPrintedCounts() {
        return new long[] { printedMuv, printedHuv, printedEuv, printedXuv, printedLuv };
    }

    void recordPrinted(GTRecipe recipe) {
        LithoMode mode = modeOf(recipe.recipeType);
        if (mode == null) return;
        switch (mode) {
            case MUV -> printedMuv++;
            case HUV -> printedHuv++;
            case EUV -> printedEuv++;
            case XUV -> printedXuv++;
            case LUV -> printedLuv++;
        }
        markDirty();
    }

    /** Switches to the given mode, the same way GT's mode tab does, and drops the recipe cached for the old one. */
    public void selectMode(LithoMode mode) {
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (modeOf(types[i]) != mode) continue;
            if (i == getActiveRecipeType()) return;
            setActiveRecipeType(i);
            recipeLogic.updateTickSubscription();
            recipeLogic.markLastRecipeDirty();
            return;
        }
    }

    public void resetCounters() {
        printedMuv = 0;
        printedHuv = 0;
        printedEuv = 0;
        printedXuv = 0;
        printedLuv = 0;
        markDirty();
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** Replaces GT's text display with the console; GT's side tabs (power, mode, parts) stay. */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, LithoConsoleWidget.WIDTH, LithoConsoleWidget.HEIGHT);
        group.addWidget(new LithoConsoleWidget(this, 0, 0));
        for (LithoMode mode : LithoMode.values()) {
            // clicks arrive on the client first and are then forwarded; only act on the server copy
            var tile = new ButtonWidget(LithoConsoleWidget.tileX(mode.ordinal()), LithoConsoleWidget.TILE_Y,
                    LithoConsoleWidget.TILE_W, LithoConsoleWidget.TILE_H, IGuiTexture.EMPTY,
                    click -> {
                        if (!click.isRemote) selectMode(mode);
                    });
            tile.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
            tile.setHoverTooltips(
                    Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color),
                    Component.translatable("af9.litho.console.tile_version", mode.level()),
                    Component.translatable("af9.litho.console.tile_power",
                            Component.translatable("af9.litho.hatch." + mode.hatchTier)),
                    Component.translatable("af9.litho.console.tile_substrate",
                            Component.translatable("af9.litho.substrate." + mode.substrate)),
                    Component.translatable("af9.litho.console.tile_scaling",
                            LithoMode.formatFactor(mode.transistorDensity()),
                            LithoMode.formatFactor(mode.dieFactor())),
                    Component.translatable("af9.litho.console.tile_light",
                            Component.translatable("af9.litho.light." + mode.light + ".long")),
                    Component.translatable("af9.litho.console.tile_optics",
                            String.format(Locale.ROOT, "%.2f", mode.numericalAperture),
                            String.format(Locale.ROOT, "%.2f", mode.k1())),
                    Component.translatable("af9.litho.console.tile_resist",
                            Component.translatable("material.gtceu." + mode.resist)));
            group.addWidget(tile);
        }
        var reset = new ButtonWidget(LithoConsoleWidget.RESET_X, LithoConsoleWidget.RESET_Y, LithoConsoleWidget.RESET_W,
                LithoConsoleWidget.RESET_H, IGuiTexture.EMPTY, click -> {
                    if (!click.isRemote) resetCounters();
                });
        reset.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        reset.setHoverTooltips(Component.translatable("af9.litho.console.reset_tooltip"));
        group.addWidget(reset);
        return group;
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /** Adds the mode's node, light source and line version to its EMI/JEI recipes, as one short line. */
    public static void registerRecipeInfo() {
        for (LithoMode mode : LithoMode.values()) {
            GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", mode.recipeTypeId()));
            if (type == null) {
                AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                        mode.recipeTypeId());
                continue;
            }
            // rendered as a plain label, so the text must not contain '%'
            type.addDataInfo(data -> Component.translatable("af9.recipe.litho_node", mode.nodeNm,
                    Component.translatable("af9.litho.light." + mode.light), mode.level()).getString());
        }
    }
}
