package com.af9.core.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Agriculture Simulation Bed: a garden of soil under open air behind a corrosion-proof shell (the structure is
 * Janne's, 11 x 3 x 11). It simulates the pack's seeds (Mystical Agriculture's essence seeds above all) and its rules:
 * <ul>
 * <li><b>Soil:</b> every soil block of the garden counts. Mystical inferium / prudentium farmland is plain soil (x1);
 * Farmer's Delight rich soil farmland and Mystical tertium / imperium / supremium farmland are rich (+25 % output, -10 %
 * time); Thermal's Phytosoil is the best (+50 % output, -25 % time). The bonus is the average over the garden's soil
 * ({@link #soilModifier}).</li>
 * <li><b>Parallel hatch or batch mode, never both:</b> an installed parallel hatch turns batch mode off (and it cannot
 * be switched on); without one, batch mode runs {@link #BATCH_RUNS} recipes in one long cycle ({@link #batchModifier}).
 * GT's own batch mode only batches recipes shorter than its limit, which a minute-long recipe never is.</li>
 * <li><b>The garden is seen:</b> the seed of the running recipe stands on every soil block, in its growth stage
 * ({@link com.af9.core.client.render.AgriSimRender}).</li>
 * </ul>
 */
public class AgriSimMachine extends WorkableElectricMultiblockMachine implements IAgriSimMachine, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(AgriSimMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** How many recipes one batch cycle holds (so one cycle is {@code BATCH_RUNS} times the recipe's time). */
    public static final int BATCH_RUNS = 8;

    /** The garden's soil cells, relative to the controller: aisles 1-9 (back 0-8 from its aisle), columns 1-9 (left -4..4). */
    public static final int GARDEN_BACK = 8;
    public static final int GARDEN_SIDE = 4;
    /** The soil's row above the controller's. */
    public static final int GARDEN_UP = 1;

    public static final String[] SOIL_NAMES = { "plain", "rich", "phyto" };
    /** Output multiplier and time multiplier per soil level (plain, rich, phyto). */
    public static final double[] SOIL_OUTPUT = { 1.0, 1.25, 1.5 };
    public static final double[] SOIL_TIME = { 1.0, 0.9, 0.75 };

    private static final Map<String, Integer> SOILS = new HashMap<>();

    static {
        SOILS.put("mysticalagriculture:inferium_farmland", 0);
        SOILS.put("mysticalagriculture:prudentium_farmland", 0);
        SOILS.put("farmersdelight:rich_soil_farmland", 1);
        SOILS.put("mysticalagriculture:tertium_farmland", 1);
        SOILS.put("mysticalagriculture:imperium_farmland", 1);
        SOILS.put("mysticalagriculture:supremium_farmland", 1);
        SOILS.put("thermal:phytosoil_tilled", 2);
    }

    /** The soil bonus of the bed: the garden's average, as output and time multipliers. */
    public static final RecipeModifier SOIL = AgriSimMachine::soilModifier;
    /** Batch mode: {@link #BATCH_RUNS} recipes in one cycle, unless a parallel hatch is installed. */
    public static final RecipeModifier BATCH = AgriSimMachine::batchModifier;

    @DescSynced
    private String agriSeed = "";
    @DescSynced
    private int agriGrowth;
    @DescSynced
    private int agriRun;
    @DescSynced
    private int agriTicks = 1200;
    /** The crop render of the garden, switched by the button of the screen. */
    @DescSynced
    @Persisted
    private boolean agriRenderOn = true;
    private int lastGrowth;
    private int idleTicks;

    private final int[] soilCounts = new int[3];
    private double outputMultiplier = 1.0;
    private double timeMultiplier = 1.0;
    private TickableSubscription syncSubs;

    /**
     * The seed slot of the bed's screen: the seed the bed grows, kept (never consumed; the recipes name it as a not
     * consumed input and run only with it here). No pipe access (capability IO NONE), which also makes the handler
     * itself refuse inserts: the screen's slot works on its {@code storage}.
     */
    @Persisted
    public final NotifiableItemStackHandler seedSlot;

    public AgriSimMachine(IMachineBlockEntity holder) {
        super(holder);
        seedSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE).setFilter(AgriSimMachine::isSeed);
    }

    /** A seed: the item of a crop (a bush-type block). */
    public static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BushBlock;
    }

    /** Broken controller: the seed drops. */
    @Override
    public void onMachineRemoved() {
        clearInventory(seedSlot.storage);
    }

    /** GT's screen with a seed and render panel on its right. */
    @Override
    public Widget createUIWidget() {
        Widget screen = super.createUIWidget();
        if (screen instanceof WidgetGroup group) {
            // a small panel on the right of the screen, beside the player inventory: nothing of the text display is covered
            int px = group.getSize().width + 4, py = group.getSize().height - 64;
            group.addWidget(new ImageWidget(px, py, 72, 62, GuiTextures.BACKGROUND));
            group.addWidget(new LabelWidget(px + 8, py + 5, "af9.agri_sim.seed"));
            group.addWidget(new SlotWidget(seedSlot.storage, 0, px + 8, py + 16, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT)
                    .setHoverTooltips(Component.translatable("af9.agri_sim.seed_tooltip")));
            group.addWidget(new ButtonWidget(px + 4, py + 42, 64, 14, GuiTextures.BUTTON, click -> {
                if (!click.isRemote) agriRenderOn = !agriRenderOn;
            }).setHoverTooltips(Component.translatable("af9.agri_sim.render_tooltip")));
            group.addWidget(new LabelWidget(px + 8, py + 45, () -> Component.translatable(
                    agriRenderOn ? "af9.agri_sim.render_on" : "af9.agri_sim.render_off").getString()));
        }
        return screen;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- soil ----

    /** The level of a soil block (0 plain, 1 rich, 2 phyto), or -1 when it is no soil of the bed. */
    public static int soilLevel(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id == null ? -1 : SOILS.getOrDefault(id.toString(), -1);
    }

    /** The garden's soil levels get counted when the bed forms. */
    private void scanSoil() {
        java.util.Arrays.fill(soilCounts, 0);
        outputMultiplier = 1.0;
        timeMultiplier = 1.0;
        if (getLevel() == null) return;
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction left = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        double output = 0, time = 0;
        int total = 0;
        for (int depth = 0; depth <= GARDEN_BACK; depth++) {
            for (int side = -GARDEN_SIDE; side <= GARDEN_SIDE; side++) {
                BlockPos pos = getPos().relative(up, GARDEN_UP).relative(back, depth).relative(left, side);
                int level = soilLevel(getLevel().getBlockState(pos));
                if (level < 0) continue;
                soilCounts[level]++;
                output += SOIL_OUTPUT[level];
                time += SOIL_TIME[level];
                total++;
            }
        }
        if (total > 0) {
            outputMultiplier = output / total;
            timeMultiplier = time / total;
        }
    }

    public double getOutputMultiplier() {
        return outputMultiplier;
    }

    public double getTimeMultiplier() {
        return timeMultiplier;
    }

    // ---- parallel hatch or batch mode ----

    @Override
    public boolean isBatchEnabled() {
        return batchEnabled && getParallelHatch().isEmpty();
    }

    @Override
    public void setBatchEnabled(boolean batch) {
        if (batch && getParallelHatch().isPresent()) return;
        super.setBatchEnabled(batch);
    }

    // ---- lifecycle ----

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        scanSoil();
        if (getParallelHatch().isPresent()) batchEnabled = false;
        syncSubs = subscribeServerTick(syncSubs, this::syncTick);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        java.util.Arrays.fill(soilCounts, 0);
        outputMultiplier = 1.0;
        timeMultiplier = 1.0;
        agriSeed = "";
        agriGrowth = 0;
        lastGrowth = 0;
        if (syncSubs != null) {
            syncSubs.unsubscribe();
            syncSubs = null;
        }
    }

    /** Every 5 ticks: the seed that grows now and how far, for the renderer. */
    private void syncTick() {
        if (!isFormed() || getOffsetTimer() % 5 != 0) return;
        String seed = "";
        int growth = 0;
        int single = 0;
        var logic = getRecipeLogic();
        if (logic.isWorking()) {
            GTRecipe recipe = logic.getLastRecipe();
            if (recipe != null && recipe.data != null) {
                seed = recipe.data.getString("seed");
                single = Math.max(1, logic.getDuration() / Math.max(1, recipe.batchParallels));
                growth = (int) Math.min(1000L, (long) (logic.getProgress() % single) * 1000L / single);
            }
        }
        if (seed.isEmpty()) {
            // a gap between two runs is not the end: the plants stay for two seconds
            idleTicks += 5;
            if (idleTicks < 40) return;
            if (!agriSeed.isEmpty()) agriSeed = "";
            lastGrowth = 0;
            return;
        }
        idleTicks = 0;
        if (!seed.equals(agriSeed) || growth < lastGrowth) agriRun++;
        lastGrowth = growth;
        if (!seed.equals(agriSeed)) agriSeed = seed;
        if (growth != agriGrowth) agriGrowth = growth;
        if (single != agriTicks) agriTicks = single;
    }

    // ---- recipe modifiers ----

    public static @NotNull ModifierFunction soilModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof AgriSimMachine bed)) {
            return RecipeModifier.nullWrongType(AgriSimMachine.class, machine);
        }
        String seed = recipe.data == null ? "" : recipe.data.getString("seed");
        if (!seed.isEmpty() && !seedInSlot(bed, seed)) {
            return ModifierFunction.cancel(Component.translatable("af9.agri_sim.seed_missing"));
        }
        if (bed.outputMultiplier == 1.0 && bed.timeMultiplier == 1.0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .outputModifier(ContentModifier.multiplier(bed.outputMultiplier))
                .durationMultiplier(bed.timeMultiplier)
                .build();
    }

    /** Whether the seed slot holds the given seed. */
    private static boolean seedInSlot(AgriSimMachine bed, String seed) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(bed.seedSlot.getStackInSlot(0).getItem());
        return id != null && id.toString().equals(seed);
    }

    public static @NotNull ModifierFunction batchModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof AgriSimMachine bed)) {
            return RecipeModifier.nullWrongType(AgriSimMachine.class, machine);
        }
        if (!bed.isFormed() || !bed.isBatchEnabled()) return ModifierFunction.IDENTITY;
        int runs = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, BATCH_RUNS);
        if (runs <= 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(runs))
                .outputModifier(ContentModifier.multiplier(runs))
                .durationMultiplier(runs)
                .batchParallels(runs)
                .build();
    }

    // ---- GUI ----

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        int total = soilCounts[0] + soilCounts[1] + soilCounts[2];
        textList.add(Component.translatable("af9.agri_sim.soil", total));
        if (soilCounts[2] > 0) textList.add(Component.translatable("af9.agri_sim.soil_phyto", soilCounts[2]));
        if (soilCounts[1] > 0) textList.add(Component.translatable("af9.agri_sim.soil_rich", soilCounts[1]));
        if (soilCounts[0] > 0) textList.add(Component.translatable("af9.agri_sim.soil_plain", soilCounts[0]));
        if (total > 0) {
            textList.add(Component.translatable("af9.agri_sim.soil_bonus",
                    Math.round((outputMultiplier - 1.0) * 100), Math.round((1.0 - timeMultiplier) * 100)));
        }
        if (getParallelHatch().isPresent()) {
            textList.add(Component.translatable("af9.agri_sim.batch_off"));
        }
    }

    // ---- the renderer's view ----

    @Override
    public boolean isAgriFormed() {
        return isFormed();
    }

    @Override
    public String getAgriSeed() {
        return agriSeed;
    }

    @Override
    public int getAgriGrowth() {
        return agriGrowth;
    }

    @Override
    public boolean isAgriRenderOn() {
        return agriRenderOn;
    }

    @Override
    public int getAgriRun() {
        return agriRun;
    }

    @Override
    public int getAgriTicks() {
        return agriTicks;
    }
}
