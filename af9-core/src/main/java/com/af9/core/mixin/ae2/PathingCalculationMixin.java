package com.af9.core.mixin.ae2;

import com.af9.core.ae2.MEComputationService;

import appeng.api.networking.IGrid;
import appeng.me.GridNode;
import appeng.me.pathfinding.PathingCalculation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * AE2's channel assignment (a BFS out from the controllers, dense cables first): a network short of computation gets
 * no more channels than its {@link MEComputationService#getChannelCap cap}, so the devices farthest from the
 * controller are the ones without.
 */
@Mixin(value = PathingCalculation.class, remap = false)
public abstract class PathingCalculationMixin {

    @Shadow
    @Final
    private IGrid grid;

    @Shadow
    @Final
    private Set<GridNode> channelNodes;

    @Inject(method = "tryUseChannel", at = @At("HEAD"), cancellable = true)
    private void af9$computationCap(GridNode start, CallbackInfoReturnable<Boolean> cir) {
        int cap = MEComputationService.channelCap(grid);
        if (cap >= 0 && channelNodes.size() >= cap) cir.setReturnValue(false);
    }
}
