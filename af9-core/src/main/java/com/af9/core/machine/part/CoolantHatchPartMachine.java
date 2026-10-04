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
 * Coolant Hatch: a fluid input hatch that only takes the supercooled fluids (gtceu:supercooled_*, made in the
 * Supercooling Cryostat). The machines that need cooling (Particle Accelerator, Orbital Lithography Station)
 * take their fluids only through these ({@link #COOLANT_INPUT}); the Space Elevator has them for
 * its coolant; in other multiblocks it works as a filtered input hatch.
 */
public class CoolantHatchPartMachine extends FluidHatchPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CoolantHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definitions (KubeJS), required by the cooled multiblocks' patterns. */
    public static final PartAbility COOLANT_INPUT = new PartAbility("af9_coolant_input");
    /** Tank size at LuV; doubles per tier like GT's hatches (see {@link FluidHatchPartMachine#getTankCapacity}). */
    public static final int INITIAL_CAPACITY = 1000;

    public CoolantHatchPartMachine(IMachineBlockEntity holder, int tier) {
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

    /** Supercooled fluids only. */
    private boolean accepts(FluidStack stack) {
        return isCoolant(stack);
    }

    /** gtceu:supercooled_&lt;anything&gt;. */
    public static boolean isCoolant(FluidStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && id.getNamespace().equals("gtceu") && id.getPath().startsWith("supercooled_");
    }
}
