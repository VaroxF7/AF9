package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.part.ResonanceHatchPartMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ResearchData;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Certus Resonating Manufacturer, copied from TerraFirmaGreg's ME Assembler ({@code MEAssemblerMachine},
 * TerraFirmaGreg-Team/Core-Modern, LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/): a Certus-powered
 * assembler that runs on Liquid Resonating Fluix. The fluid is the stabiliser: while the Resonating Fluix Hatch has
 * some, the Manufacturer batches its crafts (as many parallel runs as the batch time allows), and every craft cycle
 * drinks a little of it; without it, each recipe runs alone. No overclocks: the fluid alone makes the difference.
 * Structure, hatches and recipes: the KubeJS script {@code startup_scripts/gtceu/me_resonance_fabricator.js}.
 */
public class MEResonanceFabricatorMachine extends WorkableElectricMultiblockMachine {

    public static final String TYPE = "me_assembler";
    /** The Resonating Fluix Hatch at HV, in the structure preview. */
    public static final String RESONANCE_HATCH = "hv_resonance_hatch";
    /** The stabiliser the machine runs on. */
    public static final String STABILISER = ResonanceHatchPartMachine.STABILISER;

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEResonanceFabricatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private final List<ResonanceHatchPartMachine> stabilisers = new ArrayList<>();

    public MEResonanceFabricatorMachine(IMachineBlockEntity holder) {
        super(holder);
        this.batchEnabled = true; // the machine batches on its own; the toggle in the GUI still switches it off
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        stabilisers.clear();
        for (IMultiPart part : getParts()) {
            if (part instanceof ResonanceHatchPartMachine hatch) {
                stabilisers.add(hatch);
            }
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        stabilisers.clear();
    }

    /** The millibuckets of Liquid Resonating Fluix in the machine's hatches. */
    public int stabiliserAmount() {
        int total = 0;
        for (ResonanceHatchPartMachine hatch : stabilisers) {
            total += hatch.amount();
        }
        return total;
    }

    /** While the stabiliser is there, the machine batches. */
    public boolean isStabilised() {
        return stabiliserAmount() > 0;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        if (recipe == null) return true;
        // the stabiliser pays for the craft: a few millibuckets per parallel run
        int runs = Math.max(1, recipe.batchParallels);
        int left = runs;
        for (ResonanceHatchPartMachine hatch : stabilisers) {
            if (left <= 0) break;
            left -= hatch.drain(left);
        }
        return true;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        int amount = stabiliserAmount();
        if (amount > 0) {
            textList.add(Component.translatable("af9.me_fabricator.stabilised",
                    Component.literal(String.format("%,d", amount)).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            textList.add(Component.translatable("af9.me_fabricator.unstabilised").withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * The Manufacturer's only recipe modifier (used with {@code alwaysTryModifyRecipe}): a recipe above the
     * machine's voltage tier does not run, and with the stabiliser in the hatch the craft is batched (as many
     * parallel runs as the batch time allows). Without it every recipe runs on its own.
     */
    public static final RecipeModifier RESONANCE = MEResonanceFabricatorMachine::resonanceModifier;

    public static @NotNull ModifierFunction resonanceModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof MEResonanceFabricatorMachine meMachine)) {
            return RecipeModifier.nullWrongType(MEResonanceFabricatorMachine.class, machine);
        }

        if (RecipeHelper.getRecipeEUtTier(recipe) > meMachine.getTier()) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
        }

        if (!meMachine.isBatchEnabled() || !meMachine.isStabilised()) return ModifierFunction.IDENTITY;
        if (recipe.duration >= ConfigHolder.INSTANCE.machines.batchDuration) return ModifierFunction.IDENTITY;

        int parallel = ConfigHolder.INSTANCE.machines.batchDuration / recipe.duration;
        parallel = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, parallel);
        if (parallel <= 1) return ModifierFunction.IDENTITY;

        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .durationMultiplier(parallel)
                .batchParallels(parallel)
                .build();
    }

    // ---- the recipe type: research slot, the data orb and the stabiliser on the recipe page ----

    /**
     * Finishes the recipe type {@code gtceu:me_assembler} (made by the KubeJS script): the recipe page with the data
     * orb of the recipe's research and the stabiliser next to it, the research recipes, a hint line.
     */
    public static void configureRecipeType() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?", TYPE);
            return;
        }
        type.onRecipeBuild(ResearchManager::createDefaultResearchRecipe);
        type.addDataInfo(data -> Component.translatable("af9.me_fabricator.recipe_hint").getString());
        type.setUiBuilder((recipe, widgetGroup) -> {
            // GregTech puts the inputs on the left, the arrow in the middle and the output on the right, with the
            // info lines under them; the data orb and the stabiliser go in one row under the arrow, above the lines
            int centre = widgetGroup.getSize().width / 2;
            int rowY = 62;

            List<ItemStack> sticks = new ArrayList<>();
            for (RecipeCondition<?> condition : recipe.conditions) {
                if (condition instanceof ResearchCondition research && research.data != null) {
                    for (ResearchData.ResearchEntry entry : research.data) {
                        sticks.add(entry.getDataItem());
                    }
                }
            }
            if (!sticks.isEmpty()) {
                widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(List.of(sticks)), 0,
                        centre - 20, rowY, false, false)
                        .setBackground(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.DATA_ORB_OVERLAY)));
            }
            // the stabiliser: the fluid that batches the craft
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(STABILISER));
            if (fluid != null) {
                FluidTank tank = new FluidTank(1000);
                tank.setFluid(new FluidStack(fluid, 1000));
                widgetGroup.addWidget(new TankWidget(tank, centre + 2, rowY, false, false)
                        .setShowAmount(false)
                        .setBackground(GuiTextures.FLUID_SLOT));
            }
        });
    }

    // ---- the structure preview: the cage with its hatches ----

    private static Block block(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return block == null ? Blocks.AIR : block;
    }

    /** The preview page: the cage of AE2 casings, the glass core and the hatches on the deck. */
    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        pages.add(shape(definition));
        return pages;
    }

    /**
     * The page's cells are laid out like the pattern: aisles back (north) to front (south), so the controller in the last
     * aisle faces SOUTH, and the hatches of the deck's edges face outward (west / east / north); the ones inside the deck
     * face down. A part facing into the cage hides its overlay.
     */
    private static MultiblockShapeInfo shape(MultiblockMachineDefinition definition) {
        MachineDefinition hatch = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", RESONANCE_HATCH));
        MultiblockShapeInfo.ShapeInfoBuilder builder = MultiblockShapeInfo.builder()
                .aisle(" CHC ", " C C ", " C C ", " C C ")
                .aisle("iCCMo", "CCGCC", "CCGCC", " C C ")
                .aisle("fCPre", "CC CC", "CCGCC", " C C ")
                .aisle("iCCCI", "CCGCC", "CCGCC", " C C ")
                .aisle(" CXC ", " C C ", " C C ", " C C ")
                .where('X', (IMachineBlock) definition.get(), Direction.SOUTH)
                .where('C', block("af9:ae2_machine_casing"))
                .where('G', block("ae2:quartz_vibrant_glass"))
                .where('P', block("gtceu:steel_pipe_casing"))
                .where('H', (IMachineBlock) GTResearchMachines.BASIC_DATA_ACCESS_HATCH.get(), Direction.NORTH)
                .where('i', (IMachineBlock) GTMachines.ITEM_IMPORT_BUS[GTValues.HV].get(), Direction.WEST)
                .where('I', (IMachineBlock) GTMachines.ITEM_IMPORT_BUS[GTValues.HV].get(), Direction.EAST)
                .where('o', (IMachineBlock) GTMachines.ITEM_EXPORT_BUS[GTValues.HV].get(), Direction.EAST)
                .where('M', (IMachineBlock) GTMachines.MAINTENANCE_HATCH.get(), Direction.DOWN)
                .where('f', (IMachineBlock) GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].get(), Direction.WEST)
                .where('e', (IMachineBlock) GTMachines.ENERGY_INPUT_HATCH[GTValues.HV].get(), Direction.EAST)
                .where(' ', Blocks.AIR);
        // the Resonating Fluix Hatch: the KubeJS script registers it, so it is there when the page is built
        if (hatch == null) {
            builder.where('r', Blocks.AIR);
        } else {
            builder.where('r', hatch.get(), Direction.DOWN);
        }
        return builder.build();
    }
}
