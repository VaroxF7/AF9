package com.af9.core.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fusion ramp: a fusion reactor runs faster the longer it runs without a pause, after the Plasma
 * Forge's warmth ({@code PlasmaForgeMachine}, same numbers: full ramp after 30 minutes, runs take
 * 25 % less time). Unlike the forge it keeps no state of its own: the ramp is estimated from GT's
 * own consecutive-craft counter times the run time, both persisted and synced by GT, so a ramp
 * survives reloads and resets whenever the line breaks (inputs out, structure broken, suspend).
 * EU/t never changes; cancelling modifiers (too little start EU, wrong tier) pass through
 * untouched, and every fusion tier ramps (Mk1 through the Mega, one class).
 */
@Mixin(FusionReactorMachine.class)
public abstract class FusionReactorMixin {

    /** Minutes of continuous running for the full ramp; the time discount at the full ramp, percent. */
    @Unique
    private static final int af9$RAMP_MINUTES = 30;
    @Unique
    private static final int af9$TIME_PERCENT = 25;
    @Unique
    private static final long af9$RAMP_TICKS = af9$RAMP_MINUTES * 60L * 20L;

    @Inject(method = "recipeModifier", at = @At("RETURN"), cancellable = true)
    private static void af9$fusionRamp(MetaMachine machine, GTRecipe recipe,
            CallbackInfoReturnable<ModifierFunction> cir) {
        if (!(machine instanceof FusionReactorMachine fusion)) return;
        ModifierFunction base = cir.getReturnValue();
        // a cancelling modifier carries its failure reason; only ramp plain successes
        if (base == null || base.getFailReason() != ModifierFunction.DEFAULT_FAILURE) return;
        double ramp = af9$getRamp(fusion);
        if (ramp <= 0) return;
        cir.setReturnValue(base.andThen(ModifierFunction.builder()
                .durationMultiplier(1 - af9$TIME_PERCENT / 100.0 * ramp)
                .build()));
    }

    @Inject(method = "addDisplayText", at = @At("TAIL"))
    private void af9$fusionRampLine(List<Component> textList, CallbackInfo ci) {
        FusionReactorMachine fusion = (FusionReactorMachine) (Object) this;
        if (!fusion.isFormed()) return;
        textList.add(Component.translatable("af9.fusion.ramp",
                Math.round(af9$TIME_PERCENT * af9$getRamp(fusion))));
    }

    /** How far the ramp has built, 0 to 1: consecutive crafts times the run time, over 30 minutes. */
    @Unique
    private static double af9$getRamp(FusionReactorMachine fusion) {
        var logic = fusion.getRecipeLogic();
        if (logic == null) return 0;
        long ticks = (long) logic.getConsecutiveRecipes() * Math.max(1, logic.getDuration());
        if (ticks <= 0) return 0;
        return Math.min(1.0, ticks / (double) af9$RAMP_TICKS);
    }
}
