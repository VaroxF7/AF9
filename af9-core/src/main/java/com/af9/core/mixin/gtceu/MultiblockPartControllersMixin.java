package com.af9.core.mixin.gtceu;

import com.af9.core.distillation.DistillationModuleMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * A part takes its look from the first of its controllers whose model it can use ({@code MachineModel.replacePartBaseModel}). A
 * hatch in a casing of a Distillation Facility tower has two, the facility (Steel Turbine Casing) and the tower's module (clean
 * stainless casing), and the client builds the list from a hash set, so which came first was an accident of the block's position:
 * tower 1's hatches were stainless and towers 2 and 3 wore the facility's casing. A Distillation Tower Module is kept first.
 */
@Mixin(value = MultiblockPartMachine.class, remap = false)
public abstract class MultiblockPartControllersMixin {

    @Shadow
    @Final
    protected SortedSet<IMultiController> controllers;

    @Inject(method = "onControllersUpdated", at = @At("TAIL"), remap = false)
    private void af9$modulesFirstAfterUpdate(Set<BlockPos> newPositions, Set<BlockPos> old, CallbackInfo ci) {
        af9$modulesFirst();
    }

    @Inject(method = "addedToController", at = @At("TAIL"), remap = false)
    private void af9$modulesFirstAfterAdd(IMultiController controller, CallbackInfo ci) {
        af9$modulesFirst();
    }

    private void af9$modulesFirst() {
        if (controllers.size() < 2 || controllers.first() instanceof DistillationModuleMachine) return;
        boolean hasModule = false;
        for (IMultiController controller : controllers) {
            if (controller instanceof DistillationModuleMachine) {
                hasModule = true;
                break;
            }
        }
        if (!hasModule) return;
        List<IMultiController> ordered = new ArrayList<>(controllers);
        controllers.clear();
        for (IMultiController controller : ordered) {
            if (controller instanceof DistillationModuleMachine) controllers.add(controller);
        }
        for (IMultiController controller : ordered) {
            if (!(controller instanceof DistillationModuleMachine)) controllers.add(controller);
        }
    }
}
