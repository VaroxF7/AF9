package com.af9.core.machine;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Controller logic for the Photolithography Line (structure and recipes are defined in KubeJS).
 * <p>
 * Mk I is the bare 3x3x20 line. The KrF Excimer Laser Module (on top of the lamp housing) makes it Mk II, and the
 * Twin-Stage Scanner Module (on top of the wafer stage) on top of that makes it Mk III. Every recipe carries its tier
 * in {@link #TIER_KEY}; the line only runs recipes of its operating tier, which the player can lower with the
 * controller buttons.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1; GT uses it too
public class PhotolithographyLineMachine extends WorkableElectricMultiblockMachine {

    public static final String TIER_KEY = "af9_litho_tier";
    public static final int MAX_TIER = 3;
    public static final ResourceLocation RECIPE_TYPE_ID = new ResourceLocation("gtceu", "photolithography");
    public static final ResourceLocation KRF_MODULE_ID = new ResourceLocation("kubejs", "krf_excimer_laser_module");
    public static final ResourceLocation TWIN_STAGE_MODULE_ID = new ResourceLocation("kubejs",
            "twin_stage_scanner_module");

    /** EU/t drawn by each tier's recipes (indexed by tier): always 4A, one voltage tier higher per module. */
    private static final long[] TIER_EUT = { 0, (long) GTValues.VA[GTValues.MV] * 4, (long) GTValues.VA[GTValues.HV] * 4,
            (long) GTValues.VA[GTValues.EV] * 4 };

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyLineMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Only lets a recipe run when it belongs to the line's operating tier and the energy hatches can actually supply
     * its EU/t (GT's own voltage check would let two MV hatches start an HV recipe and then starve).
     */
    public static final RecipeModifier LITHO_GATE = (machine, recipe) -> {
        if (!(machine instanceof PhotolithographyLineMachine line)) {
            return RecipeModifier.nullWrongType(PhotolithographyLineMachine.class, machine);
        }
        if (getRecipeTier(recipe) != line.getOperatingTier()) return ModifierFunction.NULL;
        if (line.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        return ModifierFunction.IDENTITY;
    };

    /** 0 = automatic (always the highest installed tier), otherwise the tier chosen with the controller buttons. */
    @Persisted
    private int selectedTier;
    @Persisted
    private long printedStandard;
    @Persisted
    private long printedHighGrade;
    @Persisted
    private long printedPremium;

    // runtime, refreshed whenever the structure (re)forms
    private boolean hasKrfModule;
    private boolean hasTwinStageModule;

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
    // ***** Structure and modules *****//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        detectModules();
        recipeLogic.markLastRecipeDirty();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        hasKrfModule = false;
        hasTwinStageModule = false;
    }

    /**
     * Module slots accept air or their module, so the structure re-forms whenever a module is placed or removed and
     * the slot is always part of the matched structure positions.
     */
    private void detectModules() {
        hasKrfModule = false;
        hasTwinStageModule = false;
        Level level = getLevel();
        if (level == null) return;
        Block krf = ForgeRegistries.BLOCKS.getValue(KRF_MODULE_ID);
        Block twinStage = ForgeRegistries.BLOCKS.getValue(TWIN_STAGE_MODULE_ID);
        for (BlockPos pos : getMultiblockState().getCache()) {
            Block block = level.getBlockState(pos).getBlock();
            // an unknown id resolves to air, and empty module slots are air too
            if (block == Blocks.AIR) continue;
            if (block == krf) hasKrfModule = true;
            else if (block == twinStage) hasTwinStageModule = true;
        }
    }

    /** 0 when unformed; the Twin-Stage module only counts on top of the KrF module. */
    public int getInstalledTier() {
        if (!isFormed()) return 0;
        if (!hasKrfModule) return 1;
        return hasTwinStageModule ? 3 : 2;
    }

    public int getOperatingTier() {
        int installed = getInstalledTier();
        if (installed == 0) return 0;
        return selectedTier <= 0 ? installed : Math.min(selectedTier, installed);
    }

    public long getAvailableEUt() {
        return energyContainer == null ? 0 : energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    public static int getRecipeTier(GTRecipe recipe) {
        return recipe.data.contains(TIER_KEY) ? recipe.data.getInt(TIER_KEY) : 1;
    }

    void recordPrinted(int tier) {
        switch (tier) {
            case 2 -> printedHighGrade++;
            case 3 -> printedPremium++;
            default -> printedStandard++;
        }
        markDirty();
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        int installed = getInstalledTier();
        int operating = getOperatingTier();

        textList.add(Component.translatable("af9.litho.display.config", tierName(installed))
                .withStyle(ChatFormatting.AQUA));
        textList.add(moduleLine("af9.litho.module.krf", hasKrfModule));
        textList.add(moduleLine("af9.litho.module.twin_stage", hasTwinStageModule));
        if (hasTwinStageModule && !hasKrfModule) {
            textList.add(Component.translatable("af9.litho.display.twin_needs_krf").withStyle(ChatFormatting.RED));
        }

        MutableComponent running = Component.translatable("af9.litho.display.operating");
        running.append(ComponentPanelWidget.withButton(Component.literal(" [<] "), "tier_down"));
        running.append(tierName(operating).withStyle(ChatFormatting.GOLD));
        running.append(ComponentPanelWidget.withButton(Component.literal(" [>] "), "tier_up"));
        textList.add(running);
        textList.add(ComponentPanelWidget.withButton(Component.translatable(
                selectedTier <= 0 ? "af9.litho.button.auto_on" : "af9.litho.button.auto_off"), "auto"));

        long needed = TIER_EUT[operating];
        long available = getAvailableEUt();
        textList.add(Component.translatable("af9.litho.display.power", FormattingUtil.formatNumbers(available),
                FormattingUtil.formatNumbers(needed))
                .withStyle(available >= needed ? ChatFormatting.GREEN : ChatFormatting.RED));
        textList.add(Component.translatable("af9.litho.display.grade",
                Component.translatable("af9.litho.grade." + operating)));

        textList.add(Component.translatable("af9.litho.display.printed", printedStandard, printedHighGrade,
                printedPremium));
        textList.add(ComponentPanelWidget.withButton(Component.translatable("af9.litho.button.reset"), "reset"));
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        // clicks arrive on the client first and are then forwarded; only act on the server copy
        if (clickData.isRemote) return;
        int installed = getInstalledTier();
        if (installed == 0) return;
        int operating = getOperatingTier();
        switch (componentData) {
            case "tier_down" -> selectedTier = Math.max(1, operating - 1);
            case "tier_up" -> selectedTier = Math.min(installed, operating + 1);
            case "auto" -> selectedTier = selectedTier <= 0 ? operating : 0;
            case "reset" -> {
                printedStandard = 0;
                printedHighGrade = 0;
                printedPremium = 0;
            }
            default -> {
                return;
            }
        }
        markDirty();
        // the cached recipe was checked against the old tier
        recipeLogic.markLastRecipeDirty();
    }

    private static Component moduleLine(String moduleKey, boolean installed) {
        return Component.translatable(installed ? "af9.litho.display.module_on" : "af9.litho.display.module_off",
                Component.translatable(moduleKey))
                .withStyle(installed ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static MutableComponent tierName(int tier) {
        return Component.translatable("af9.litho.tier." + Math.max(1, Math.min(tier, MAX_TIER)));
    }

    //////////////////////////////////////
    // ********* Recipe viewer ********//
    //////////////////////////////////////

    /** Adds a "Requires: Mk ..." line to every Photolithography recipe in EMI/JEI. */
    public static void registerRecipeInfo() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(RECIPE_TYPE_ID);
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type {} not found - is the AF9 KubeJS startup script loaded?", RECIPE_TYPE_ID);
            return;
        }
        // rendered as a plain label, so the text must not contain '%'
        type.addDataInfo(data -> Component.translatable("af9.recipe.litho_tier",
                tierName(data.contains(TIER_KEY) ? data.getInt(TIER_KEY) : 1)).getString());
    }
}
