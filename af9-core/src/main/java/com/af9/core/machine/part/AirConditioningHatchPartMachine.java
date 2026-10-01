package com.af9.core.machine.part;

import com.af9.core.machine.LithoMachine;
import com.af9.core.thermal.IHeatEmitter;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Air Conditioning Hatch (MV-IV): the cooling of the Photolithography Line and Scanner. The machines are built as clean
 * rooms, so they cool with air: a print puts {@link com.af9.core.litho.LithoMode#heatLoad()} cooling units of heat into
 * the chamber and the hatches in the structure have to remove it. One hatch is worth {@link #unitsFor} units (1 at MV,
 * doubling per tier); the machine fits two. It draws a quarter of an amp of its tier while a print runs
 * ({@link #drawFor}), and every cooling unit it removes leaves the room again as warm air from its front
 * ({@link IHeatEmitter}: the hook for the Temperature Update).
 */
public class AirConditioningHatchPartMachine extends TieredPartMachine implements IHeatEmitter {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AirConditioningHatchPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definitions (KubeJS), taken by the lithography machines' patterns. */
    public static final PartAbility AIR_CONDITIONING = new PartAbility("af9_air_conditioning");
    /** Heat units per tick the exhaust gives off per cooling unit in use: the compressor's own waste on top. */
    public static final double EXHAUST_FACTOR = 2.0;

    public AirConditioningHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Cooling units a hatch of this tier is worth: MV 1, HV 2, EV 4, IV 8. */
    public static int unitsFor(int tier) {
        return 1 << Math.max(0, tier - GTValues.MV);
    }

    /** EU/t a hatch of this tier draws while it cools: a quarter of one amp of its tier. */
    public static long drawFor(int tier) {
        return GTValues.VA[tier] / 4;
    }

    public int getCoolingUnits() {
        return unitsFor(getTier());
    }

    public long getDraw() {
        return drawFor(getTier());
    }

    //////////////////////////////////////
    // *********** Exhaust ************//
    //////////////////////////////////////

    /** Whether a machine this hatch serves is cooling right now. */
    public boolean isCooling() {
        for (IMultiController controller : getControllers()) {
            if (controller instanceof LithoMachine litho && litho.isCooling()) return true;
        }
        return false;
    }

    @Override
    public int getHeatOutput() {
        return isCooling() ? (int) Math.round(getCoolingUnits() * EXHAUST_FACTOR) : 0;
    }

    @Override
    public BlockPos getHeatPos() {
        return getPos();
    }

    @Override
    public Direction getHeatDirection() {
        return getFrontFacing();
    }

    @Override
    public Level getHeatLevel() {
        return getLevel();
    }
}
