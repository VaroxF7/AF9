package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.part.MERedstonePortPartMachine;
import com.af9.core.pattern.BuddingTiers;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
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
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The ME Resonance Fabricator, copied from TerraFirmaGreg's ME Assembler ({@code MEAssemblerMachine},
 * TerraFirmaGreg-Team/Core-Modern, LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/): a Certus-powered
 * assembler that runs on a Budding Certus in its middle. The grade of the block sets the speed (nothing for a Certus
 * Quartz Block, x8 / x32 / x128 / x512 for Chipped to Exquisite Budding Certus, as a recipe-time factor), every run
 * can wear the block down a grade (the higher the recipe's tier and the longer it runs, the likelier; each block
 * lasts a random 200 to 800 units of work), and a {@link MERedstonePortPartMachine} outputs the grade. No overclocks:
 * the speed comes from the block alone, and batch mode is always on. Structure, hatches and recipes: the KubeJS script
 * {@code startup_scripts/gtceu/me_resonance_fabricator.js}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class MEResonanceFabricatorMachine extends WorkableElectricMultiblockMachine implements IDropSaveMachine {

    public static final String TYPE = "me_assembler";
    public static final String PORT = "hv_me_assembler_redstone_port";

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEResonanceFabricatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int HEALTH_MIN = 200;
    private static final int HEALTH_MAX = 800;
    /** One unit of wear per this much work (recipe time x batched runs). */
    private static final int WORK_PER_UNIT = 500;

    private static final double[] BUDDING_SPEED_BONUS = { 0.0, 8.0, 32.0, 128.0, 512.0 };

    @Persisted
    @DescSynced
    private int buddingTier = 0;

    @Persisted
    @DescSynced
    private int buddingHealth = 0;

    @Nullable
    private BlockPos buddingPos = null;

    private final List<MERedstonePortPartMachine> redstonePorts = new ArrayList<>();
    private final ConditionalSubscriptionHandler buddingCheckSubscription;

    public MEResonanceFabricatorMachine(IMachineBlockEntity holder) {
        super(holder);
        this.buddingCheckSubscription = new ConditionalSubscriptionHandler(this, this::tickBuddingCheck,
                this::isFormed);
    }

    public int getBuddingTier() {
        return buddingTier;
    }

    public int getBuddingHealth() {
        return buddingHealth;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private void tickBuddingCheck() {
        if (getOffsetTimer() % 20 != 0) return;
        refreshBuddingTier();
    }

    private void refreshBuddingTier() {
        if (buddingPos == null || getLevel() == null || getLevel().isClientSide) return;
        buddingTier = BuddingTiers.tierOf(getLevel().getBlockState(buddingPos).getBlock());
        updateRedstone();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.buddingTier = 0;
        this.buddingPos = null;
        redstonePorts.clear();
        var ctx = getMultiblockState().getMatchContext();
        if (ctx.get("BuddingTier") instanceof Integer tier) {
            this.buddingTier = tier;
        }
        if (ctx.get("BuddingPos") instanceof BlockPos pos) {
            this.buddingPos = pos.immutable();
        }
        for (IMultiPart part : getParts()) {
            if (part instanceof MERedstonePortPartMachine port) {
                redstonePorts.add(port);
            }
        }
        updateRedstone();
        buddingCheckSubscription.updateSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        buddingCheckSubscription.initialize(getLevel());
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        for (var port : redstonePorts) port.trySetSignal(0);
        redstonePorts.clear();
        buddingCheckSubscription.updateSubscription();
    }

    /** The wear of the block in the middle travels with the item, nothing else of the machine does. */
    @Override
    public void saveToItem(CompoundTag tag) {
        tag.putInt("buddingHealth", buddingHealth);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        buddingHealth = tag.getInt("buddingHealth");
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        refreshBuddingTier();
        if (buddingTier < 0) {
            RecipeLogic.putFailureReason(this, recipe,
                    Component.translatable("af9.me_fabricator.budding_missing").withStyle(ChatFormatting.RED));
            return false;
        }
        tryDegradeBudding(recipe);
        return true;
    }

    /** The port's signal: 0 nothing in the middle, 1 Certus Quartz Block, 2 to 5 Chipped to Exquisite Budding Certus. */
    private void updateRedstone() {
        int signal = buddingTier < 0 ? 0 : buddingTier + 1;
        for (var port : redstonePorts) port.trySetSignal(signal);
    }

    private void rollHealth() {
        buddingHealth = HEALTH_MIN + getLevel().getRandom().nextInt(HEALTH_MAX - HEALTH_MIN + 1);
    }

    private void tryDegradeBudding(@Nullable GTRecipe recipe) {
        if (buddingTier <= 0 || buddingPos == null) return;
        if (getLevel() == null || getLevel().isClientSide) return;

        if (buddingHealth <= 0) rollHealth();

        int executions = 1;
        if (recipe != null) {
            int dur = recipe.data.contains("nominal_duration") ? recipe.data.getInt("nominal_duration") :
                    recipe.duration;
            long work = (long) dur * Math.max(1, recipe.batchParallels);
            executions = Math.max(1, (int) (work / WORK_PER_UNIT));
        }

        buddingHealth -= executions;

        int tiersLost = 0;
        while (buddingHealth <= 0 && (buddingTier - tiersLost) > 0) {
            tiersLost++;
            int overflow = -buddingHealth;
            rollHealth();
            buddingHealth -= overflow;
        }

        if (tiersLost == 0) return;

        int actual = BuddingTiers.tierOf(getLevel().getBlockState(buddingPos).getBlock());
        if (actual != buddingTier) return;

        Block next = BuddingTiers.blockOf(buddingTier - tiersLost);
        getLevel().setBlockAndUpdate(buddingPos, next.defaultBlockState());
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        if (buddingTier < 0) {
            textList.add(Component.translatable("af9.me_fabricator.budding_missing").withStyle(ChatFormatting.RED));
            return;
        }

        ChatFormatting color = switch (buddingTier) {
            case 0 -> ChatFormatting.GRAY;
            case 1, 2 -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GREEN;
        };
        int speedBonus = (int) (BUDDING_SPEED_BONUS[buddingTier] * 100);
        textList.add(Component.translatable("af9.me_fabricator.budding_tier",
                Component.literal(speedBonus + "%").withStyle(color)));
    }

    /**
     * The Fabricator's only recipe modifier (used with {@code alwaysTryModifyRecipe}): no overclocks, so the speed
     * comes from the Budding Certus alone, and batch mode is always on, because the speed gets so high. A recipe above
     * the machine's voltage tier does not run.
     */
    public static final RecipeModifier BUDDING = MEResonanceFabricatorMachine::buddingModifier;

    public static @NotNull ModifierFunction buddingModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof MEResonanceFabricatorMachine meMachine)) {
            return RecipeModifier.nullWrongType(MEResonanceFabricatorMachine.class, machine);
        }

        if (RecipeHelper.getRecipeEUtTier(recipe) > meMachine.getTier()) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
        }

        int tier = Math.max(0, Math.min(meMachine.getBuddingTier(), BuddingTiers.MAX_TIER));
        double factor = tier > 0 ? 1.0 / (1.0 + BUDDING_SPEED_BONUS[tier]) : 1.0;

        ModifierFunction speedModifier = r -> {
            var copy = r.copy();
            copy.data = r.data.copy();
            copy.data.putInt("nominal_duration", r.duration);
            copy.duration = Math.max(1, (int) (r.duration * factor));
            return copy;
        };

        GTRecipe sped = speedModifier.apply(recipe);
        if (sped == null) return ModifierFunction.NULL;

        if (sped.duration >= ConfigHolder.INSTANCE.machines.batchDuration) {
            return speedModifier;
        }

        int parallel = ConfigHolder.INSTANCE.machines.batchDuration / sped.duration;
        parallel = ParallelLogic.getParallelAmountWithoutEU(machine, sped, parallel);

        if (parallel == 0) return ModifierFunction.NULL;
        if (parallel == 1) return speedModifier;

        ModifierFunction batchModifier = ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .durationMultiplier(parallel)
                .batchParallels(parallel)
                .build();

        return speedModifier.andThen(batchModifier);
    }

    // ---- the recipe type: research slot, the data orb and the budding grades on the recipe page ----

    /**
     * Finishes the recipe type {@code gtceu:me_assembler} (made by the KubeJS script): the recipe page with the data
     * orb of the recipe's research and the budding blocks cycling under it, the research recipes, a hint line.
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
            // info lines under them; the data orb and the budding grades go in one row under the arrow, above the lines
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
            List<ItemStack> buddings = new ArrayList<>();
            for (int t = 1; t <= BuddingTiers.MAX_TIER; t++) {
                buddings.add(new ItemStack(BuddingTiers.blockOf(t)));
            }
            int buddingX = sticks.isEmpty() ? centre - 9 : centre + 2;
            if (!sticks.isEmpty()) {
                widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(List.of(sticks)), 0,
                        centre - 20, rowY, false, false)
                        .setBackground(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.DATA_ORB_OVERLAY)));
            }
            widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(List.of(buddings)), 0,
                    buddingX, rowY, false, false).setBackground(GuiTextures.SLOT));
        });
    }

    // ---- the structure preview: the empty middle and the full one ----

    private static Block block(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return block == null ? Blocks.AIR : block;
    }

    /** The preview pages: the Fabricator with a Certus Quartz Block and no spatial storage, and with an Exquisite Budding Certus between its Spatial Pylons. */
    public static List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> pages = new ArrayList<>();
        pages.add(shape(definition, false));
        pages.add(shape(definition, true));
        return pages;
    }

    private static MultiblockShapeInfo shape(MultiblockMachineDefinition definition, boolean spatial) {
        MachineDefinition port = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", PORT));
        MultiblockShapeInfo.ShapeInfoBuilder builder = MultiblockShapeInfo.builder()
                .aisle("  TTnTT  ", "TTr X TTT", "TCT   TCT", "TTT   TTT", "  TTmTT  ")
                .aisle("AAACCCAAA", "ICGD  GCA", "ICG   GCA", "ICG  DGCA", "AAACCCAAA")
                .aisle("ACCCCCCCA", "ICGDB GCA", "ICG q GCA", "ICG BDGCA", "ACCCCCCCA")
                .aisle("AAACCCAAA", "ICGFDDGCA", "ICG  DGCA", "ICGDDFGCA", "AAACCCAAA")
                .aisle("  TTpTT  ", "TTT H TTT", "TCT   TCT", "TTT   TTT", "  TToTT  ")
                .where('X', (IMachineBlock) definition.get(), Direction.NORTH)
                .where('A', block("af9:ptfe_black_casing"))
                .where('B', ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.StainlessSteel))
                .where('C', block("af9:ae2_machine_casing"))
                .where('G', block("ae2:quartz_vibrant_glass"))
                .where('H', (IMachineBlock) GTResearchMachines.BASIC_DATA_ACCESS_HATCH.get(), Direction.SOUTH)
                .where('I', (IMachineBlock) GTMachines.ITEM_IMPORT_BUS[GTValues.HV].get(), Direction.WEST)
                .where('T', block("af9:ptfe_black_casing"))
                .where('m', (IMachineBlock) GTMachines.ITEM_EXPORT_BUS[GTValues.HV].get(), Direction.NORTH)
                .where('n', (IMachineBlock) GTMachines.MAINTENANCE_HATCH.get(), Direction.NORTH)
                .where('r', (IMachineBlock) port.get(), Direction.NORTH)
                .where('o', (IMachineBlock) GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].get(), Direction.SOUTH)
                .where('p', (IMachineBlock) GTMachines.ENERGY_INPUT_HATCH[GTValues.HV].get(), Direction.SOUTH)
                .where(' ', Blocks.AIR);
        if (spatial) {
            builder.where('D', block("ae2:spatial_pylon"))
                    .where('F', block("ae2:spatial_io_port"))
                    .where('q', block("ae2:flawless_budding_quartz"));
        } else {
            builder.where('D', Blocks.AIR)
                    .where('F', Blocks.AIR)
                    .where('q', block("ae2:quartz_block"));
        }
        return builder.build();
    }
}
