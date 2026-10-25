package com.af9.core.machine.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Plasma Hatch: a fluid input hatch that only takes osmium plasma. The Plasma Forge burns it for
 * readiness through these ({@link #PLASMA_INPUT}); the forging recipes never draw on them. The
 * Particle Accelerator's plasma cells fill them.
 */
public class PlasmaHatchPartMachine extends FluidHatchPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PlasmaHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definitions (KubeJS), required by the forge's pattern. */
    public static final PartAbility PLASMA_INPUT = new PartAbility("af9_plasma_input");
    /** Tank size at HV; doubles per tier like GT's hatches (see {@link FluidHatchPartMachine#getTankCapacity}). */
    public static final int INITIAL_CAPACITY = 1000;

    public PlasmaHatchPartMachine(IMachineBlockEntity holder, int tier) {
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

    /** Osmium plasma only. */
    private boolean accepts(FluidStack stack) {
        return isPlasma(stack);
    }

    /** gtceu:osmium_plasma. */
    public static boolean isPlasma(FluidStack stack) {
        if (stack.isEmpty()) return false;
        return new ResourceLocation("gtceu", "osmium_plasma").equals(ForgeRegistries.FLUIDS.getKey(stack.getFluid()));
    }
}
