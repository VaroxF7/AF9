package com.af9.core.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Dyson Swarm (structure in KubeJS: {@code startup_scripts/gtceu/dyson_swarm.js}; the structure, the three-part
 * layout of receiver, deployment unit and command centre and the rules are GTNH Intergalactic's). Sails put into the
 * input buses are sent up and stay in the swarm; the receiver turns the light they catch into power for the output
 * hatches. Each sail gives {@value #EU_PER_SAIL} EU/t at the base yield (Allthemodium; the alloy sails give
 * {@code 200 %}, the star matter alloy {@code 350 %}, see {@link DysonSails}), times the light of the dimension the
 * swarm stands in ({@link #lightFactor()}); at most {@value #MAX_SAILS} sails fly. Every hour a cycle runs on
 * supercooled hydrogen for the receiver, and at its end some of the sails are lost to collisions
 * (GTNH's formula, {@link #destroyedShare}): more sails collide more, computation (a computation hatch) steers them
 * clear.
 */
public class DysonSwarmMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(DysonSwarmMachine.class,
            ProcessMachine.MANAGED_FIELD_HOLDER);

    /** EU/t of a sail at 100 %: an eighth of a UHV amp, so 8,000 sails are 1,000 A of UHV at the base yield. */
    public static final long EU_PER_SAIL = GTValues.V[GTValues.UHV] / 8;
    public static final int MAX_SAILS = 10000;
    /** GTNH's loss formula: base chance, a (more sails: more collisions), b (computation), computation cap, CWU/t. */
    private static final double LOSS_CHANCE = 0.066, LOSS_A = 0.00005, LOSS_B = 0.00003, LOSS_MAX_CWUT = 100000;
    /** EU/t of the recipe the swarm's output is scaled from (its output is the modifier's, not the recipe's). */
    public static final long BASE_EUT = GTValues.V[GTValues.UHV];
    private static final int ABSORB_INTERVAL = 20;
    private static final int COLOR = 0xFFFFC857;

    /**
     * Light of a dimension, relative to the Overworld's (GTNH's table where AF9 has the same body); a dimension that
     * is not listed gets 1.
     */
    private static final Map<String, Double> LIGHT = Map.ofEntries(
            Map.entry("minecraft:overworld", 1.0), Map.entry("minecraft:the_nether", 0.0),
            Map.entry("minecraft:the_end", 0.5), Map.entry("allthemodium:mining", 0.0),
            Map.entry("ad_astra:moon", 1.0), Map.entry("ad_astra:mars", 0.81), Map.entry("ad_astra:venus", 1.76),
            Map.entry("ad_astra:mercury", 1.61), Map.entry("ad_astra:glacio", 0.32),
            Map.entry("ad_astra:earth_orbit", 1.1), Map.entry("ad_astra:moon_orbit", 1.1),
            Map.entry("ad_astra:mars_orbit", 0.89), Map.entry("ad_astra:venus_orbit", 1.94),
            Map.entry("ad_astra:mercury_orbit", 1.7), Map.entry("ad_astra:glacio_orbit", 0.36),
            Map.entry("af9:asteroid_field", 0.61));

    /**
     * Starts a cycle only with sails in the swarm, and gives it the power of the sails: the recipe's EU/t is
     * {@link #BASE_EUT}, scaled to what they catch.
     */
    public static final RecipeModifier SWARM = (machine, recipe) -> {
        if (!(machine instanceof DysonSwarmMachine swarm)) {
            return RecipeModifier.nullWrongType(DysonSwarmMachine.class, machine);
        }
        long output = swarm.outputEUt();
        if (output <= 0) return ModifierFunction.NULL;
        return ModifierFunction.builder().eutMultiplier((double) output / BASE_EUT).build();
    };

    /** Sails in the swarm by tier (see {@link DysonSails}). */
    @Persisted
    private int lowSails, midSails, highSails;
    /** EU/t of the running cycle, 0 when none. */
    @Persisted
    private long cycleEUt;

    private TickableSubscription absorbSubs;

    public DysonSwarmMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.dyson_swarm.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLOR;
    }

    //////////////////////////////////////
    // ************ Sails ************//
    //////////////////////////////////////

    public int sails(int tier) {
        return switch (tier) {
            case 0 -> lowSails;
            case 1 -> midSails;
            default -> highSails;
        };
    }

    private void setSails(int tier, int count) {
        switch (tier) {
            case 0 -> lowSails = count;
            case 1 -> midSails = count;
            default -> highSails = count;
        }
    }

    public int totalSails() {
        return lowSails + midSails + highSails;
    }

    /** Light of the dimension the swarm stands in, relative to the Overworld's. */
    public double lightFactor() {
        var level = getLevel();
        if (level == null) return 1;
        return LIGHT.getOrDefault(level.dimension().location().toString(), 1.0);
    }

    /** EU/t of the swarm as it is now: the yields of its sails, in the light of its dimension. */
    public long outputEUt() {
        double sum = 0;
        for (int tier = 0; tier < DysonSails.IDS.length; tier++) {
            sum += (double) sails(tier) * EU_PER_SAIL * DysonSails.PERCENT[tier] / 100.0;
        }
        return (long) (sum * lightFactor());
    }

    /** Ticks: takes the sails out of the input buses into the swarm. */
    private void absorb() {
        if (getOffsetTimer() % ABSORB_INTERVAL != 0 || !isFormed()) return;
        boolean changed = false;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (!(handler instanceof IItemHandlerModifiable inventory)) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                int tier = sailTier(stack);
                int room = MAX_SAILS - totalSails();
                if (tier < 0 || room <= 0) continue;
                ItemStack taken = inventory.extractItem(slot, Math.min(stack.getCount(), room), false);
                if (taken.isEmpty()) continue;
                setSails(tier, sails(tier) + taken.getCount());
                changed = true;
            }
        }
        if (changed) markDirty();
    }

    /** The tier (0-2) of a sail, -1 for anything else. */
    public static int sailTier(ItemStack stack) {
        Item item = stack.getItem();
        for (int tier = 0; tier < DysonSails.IDS.length; tier++) {
            if (item == ForgeRegistries.ITEMS.getValue(new ResourceLocation("af9", DysonSails.IDS[tier]))) return tier;
        }
        return -1;
    }

    //////////////////////////////////////
    // *********** Cycles ************//
    //////////////////////////////////////

    /** The share of the sails lost in a cycle (GTNH's formula): more sails collide more, computation steers them. */
    public static double destroyedShare(int sails, int cwut) {
        if (sails <= 0) return 0;
        double cps = Math.min(cwut, LOSS_MAX_CWUT);
        double lost = sails * (2 * LOSS_CHANCE) / (Math.exp(-LOSS_A * (sails - 1)) + Math.exp(LOSS_B * cps));
        return Math.min(1, lost / sails);
    }

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        cycleEUt = outputEUt();
        markDirty();
        return true;
    }

    /** A cycle is over: collisions took some of the sails. */
    @Override
    public void afterWorking() {
        super.afterWorking();
        double share = destroyedShare(totalSails(), computation());
        for (int tier = 0; tier < DysonSails.IDS.length; tier++) {
            // truncated, like GTNH's: any loss takes at least one sail from a tier that has some
            setSails(tier, (int) (sails(tier) - sails(tier) * share));
        }
        cycleEUt = 0;
        markDirty();
    }

    /** CWU/t the computation hatches can give. */
    public int computation() {
        int total = 0;
        for (IRecipeHandler<?> handler : getCapabilitiesFlat(IO.IN, CWURecipeCapability.CAP)) {
            if (handler instanceof IOpticalComputationProvider provider) {
                total += Math.max(0, provider.getMaxCWUt(new ArrayList<>()));
            }
        }
        return total;
    }

    //////////////////////////////////////
    // ********** Life cycle *********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        absorbSubs = subscribeServerTick(absorbSubs, this::absorb);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeAbsorb();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeAbsorb();
    }

    private void unsubscribeAbsorb() {
        if (absorbSubs != null) {
            absorbSubs.unsubscribe();
            absorbSubs = null;
        }
    }

    /** A swarm in the output hatches' reach is a generator: it needs nothing from them. */
    @Override
    public long getNeededEUt() {
        return 0;
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.dyson_swarm.console.sails", totalSails(), MAX_SAILS));
        lines.add(Component.translatable("af9.dyson_swarm.console.tiers", lowSails, midSails, highSails));
        lines.add(Component.translatable("af9.dyson_swarm.console.light",
                String.format(Locale.ROOT, "%.0f", lightFactor() * 100)));
        lines.add(Component.translatable("af9.dyson_swarm.console.output",
                String.format(Locale.ROOT, "%,d", getRecipeLogic().isWorking() ? cycleEUt : outputEUt())));
        lines.add(Component.translatable("af9.dyson_swarm.console.loss",
                String.format(Locale.ROOT, "%.2f", destroyedShare(totalSails(), computation()) * 100)));
        return lines;
    }
}
