package com.af9.core.machine.fab;

import com.af9.core.fab.FabFamily;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IFluidRenderMulti;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * SMC Large Chemical Reactor: a {@link FabMultiblockMachine} of the chemistry family whose reactor vessel is open
 * space. While it runs, the vessel shows its mode's fluid through the glass walls (the model's
 * {@link com.af9.core.client.render.ModeFluidRender}; which fluid per mode is set in
 * kubejs/startup_scripts/gtceu/fab_machines.js).
 * <p>
 * The vessel is the 3 x 3 x 2 core behind the controller (1-3 blocks back, the controller's row and the one above);
 * the fluid fills every air block of it, around the PTFE stirrer and the jacket coil.
 */
public class SmcReactorMachine extends FabMultiblockMachine implements IFluidRenderMulti {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SmcReactorMachine.class, FabMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Vessel air blocks, relative to the controller; set on formation (server), synced to the client. */
    @DescSynced
    @RequireRerender
    private Set<BlockPos> fluidBlockOffsets = new HashSet<>();

    public SmcReactorMachine(IMachineBlockEntity holder) {
        super(holder, FabFamily.CHEMISTRY);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        fluidBlockOffsets = saveOffsets();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        fluidBlockOffsets = new HashSet<>();
    }

    @Override
    public Set<BlockPos> getFluidBlockOffsets() {
        return fluidBlockOffsets;
    }

    @Override
    public void setFluidBlockOffsets(Set<BlockPos> offsets) {
        fluidBlockOffsets = offsets;
    }

    /** The air blocks of the vessel, in the controller's orientation. */
    @Override
    public Set<BlockPos> saveOffsets() {
        Direction back = getFrontFacing().getOpposite();
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction right = RelativeDirection.RIGHT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        BlockPos controller = getPos();
        Level level = getLevel();
        Set<BlockPos> offsets = new HashSet<>();
        for (int depth = 1; depth <= 3; depth++) {
            for (int height = 0; height <= 1; height++) {
                for (int side = -1; side <= 1; side++) {
                    BlockPos pos = controller.relative(back, depth).relative(up, height).relative(right, side);
                    if (level == null || level.getBlockState(pos).isAir()) offsets.add(pos.subtract(controller));
                }
            }
        }
        return offsets;
    }
}
