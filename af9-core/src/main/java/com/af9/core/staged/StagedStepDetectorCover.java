package com.af9.core.staged;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.common.cover.detector.DetectorCover;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Cover that emits the running staged craft's step as redstone, like Star Technology's Layered Step Detector: the
 * number of the step to feed next (the steps begun, the running one counted; up to 15), and 0 on the last step and
 * while idle. Inverted with a screwdriver like every detector cover. Only attaches to machines running
 * {@link StagedRecipeLogic}.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StagedStepDetectorCover extends DetectorCover {

    public StagedStepDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        if (coverHolder.getLevel() == null) return false;
        return super.canAttach() && GTCapabilityHelper.getRecipeLogic(coverHolder.getLevel(), coverHolder.getPos(),
                attachedSide) instanceof StagedRecipeLogic;
    }

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 20 != 0) return;

        int output = 0;
        if (coverHolder.getLevel() != null && GTCapabilityHelper.getRecipeLogic(coverHolder.getLevel(),
                coverHolder.getPos(), attachedSide) instanceof StagedRecipeLogic staged) {
            output = staged.getCoverRedstoneOutput();
        }
        setRedstoneSignalOutput(isInverted() ? 15 - output : output);
    }
}
