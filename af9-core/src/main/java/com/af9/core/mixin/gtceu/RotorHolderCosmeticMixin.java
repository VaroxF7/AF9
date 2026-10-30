package com.af9.core.mixin.gtceu;

import com.af9.core.machine.OrbitalLithographyMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The rotors in the Orbital Array Mk2's collision chamber are for show: while the Array works they spin up at once and
 * coast down a few seconds after it stops, and they are never worn (GT's holder wears its rotor once a second while its
 * controller works) and give nothing to the prints.
 */
@Mixin(value = RotorHolderPartMachine.class, remap = false)
public abstract class RotorHolderCosmeticMixin {

    /** The speed a cosmetic rotor spins at: GT takes 3 off it a tick when the controller stands still, so 10 s of coasting. */
    private static final int SHOW_SPEED = 600;

    @Shadow
    protected abstract void updateRotorSubscription();

    @Inject(method = "onWorking", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$cosmeticRotor(IWorkableMultiController controller, CallbackInfoReturnable<Boolean> cir) {
        if (!(controller instanceof OrbitalLithographyMachine)) return;
        RotorHolderPartMachine holder = (RotorHolderPartMachine) (Object) this;
        if (holder.getRotorSpeed() != SHOW_SPEED) {
            holder.setRotorSpeed(SHOW_SPEED);
            updateRotorSubscription();
        }
        cir.setReturnValue(true);
    }
}
