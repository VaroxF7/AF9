package com.af9.core.machine.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Resonating Fluix Hatch: the stabiliser input of the Certus Resonating Manufacturer
 * ({@link com.af9.core.machine.MEResonanceFabricatorMachine}). It takes Liquid Resonating Fluix
 * (gtceu:liquid_resonating_fluix) and nothing else; while the machine has the fluid, it batches its
 * crafts, and every craft cycle drinks a few millibuckets of it.
 */
public class ResonanceHatchPartMachine extends FluidHatchPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ResonanceHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definition (KubeJS), required by the Manufacturer's pattern. */
    public static final PartAbility RESONANCE_INPUT = new PartAbility("af9_resonance_input");
    /** The fluid the hatch takes. */
    public static final String STABILISER = "gtceu:liquid_resonating_fluix";
    /** Tank size at LuV; doubles per tier like GT's hatches (see {@link FluidHatchPartMachine#getTankCapacity}). */
    public static final int INITIAL_CAPACITY = 1000;

    public ResonanceHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.IN, INITIAL_CAPACITY, 1);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        return super.createTank(initialCapacity, slots, args).setFilter(this::accepts);
    }

    /** Liquid Resonating Fluix only. */
    private boolean accepts(FluidStack stack) {
        return isStabiliser(stack);
    }

    /** gtceu:liquid_resonating_fluix. */
    public static boolean isStabiliser(FluidStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && id.toString().equals(STABILISER);
    }

    /** The millibuckets of stabiliser in the tank. */
    public int amount() {
        FluidStack stack = tank.getFluidInTank(0);
        return isStabiliser(stack) ? stack.getAmount() : 0;
    }

    /** Takes millibuckets of stabiliser out of the tank, as much as it has. */
    public int drain(int millibuckets) {
        if (millibuckets <= 0) return 0;
        FluidStack drained = tank.drain(millibuckets, IFluidHandler.FluidAction.EXECUTE);
        return drained == null || drained.isEmpty() ? 0 : drained.getAmount();
    }
}
