package com.af9.core.mixin.gtceu;

import com.af9.core.pattern.ChemicalPlantPipes;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GT opens or closes a pipe's side on every neighbour update by asking {@code canConnect} (a side with air or a casing
 * behind it is closed). A pipe of the Chemical Plant ({@link ChemicalPlantPipes#LOCK_TAG}) has the connections of the
 * structure file, some toward casings and air, so for it the answer is the side's present state: nothing opens or closes.
 */
@Mixin(value = PipeBlock.class, remap = false)
public abstract class PipeBlockMixin {

    @Inject(method = "canConnect", at = @At("HEAD"), cancellable = true, remap = false)
    private void af9$keepPlantPipe(IPipeNode<?, ?> selfTile, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (selfTile instanceof BlockEntity be && be.getPersistentData().getBoolean(ChemicalPlantPipes.LOCK_TAG)) {
            cir.setReturnValue(selfTile.isConnected(facing));
        }
    }
}
