package com.af9.core.staged;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.common.cover.detector.DetectorCover;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Cover that emits the running staged craft's step as redstone (step 1 is 1, up to 15; 0 while idle), inverted with
 * a screwdriver like every detector cover. Only attaches to machines running {@link StagedRecipeLogic}.
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
                coverHolder.getPos(), attachedSide) instanceof StagedRecipeLogic staged &&
                staged.hasStagedCraft()) {
            output = Math.min(15, Math.max(0, staged.getStageIndex()) + 1);
        }
        setRedstoneSignalOutput(isInverted() ? 15 - output : output);
    }
}
