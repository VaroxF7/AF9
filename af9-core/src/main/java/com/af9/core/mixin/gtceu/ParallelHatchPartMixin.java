package com.af9.core.mixin.gtceu;

import com.af9.core.distillation.DistillationFacilityMachine;
import com.af9.core.distillation.DistillationModuleMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GT's Parallel Hatch belongs to one multiblock only ({@code canShared() == false}: a pattern that finds it with another
 * controller already in it fails with "part shared"). A parallel hatch in a casing of a Distillation Facility tower is in two
 * patterns at once, the facility's (it is one of the casings of the whole) and the Distillation Tower Module's (which runs on
 * it), so a hatch that has one of those two as its controller can be shared with the other.
 */
@Mixin(value = ParallelHatchPartMachine.class, remap = false)
public abstract class ParallelHatchPartMixin {

    @Inject(method = "canShared", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$shareWithTowers(CallbackInfoReturnable<Boolean> cir) {
        ParallelHatchPartMachine hatch = (ParallelHatchPartMachine) (Object) this;
        for (IMultiController controller : hatch.getControllers()) {
            if (controller instanceof DistillationFacilityMachine || controller instanceof DistillationModuleMachine) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
