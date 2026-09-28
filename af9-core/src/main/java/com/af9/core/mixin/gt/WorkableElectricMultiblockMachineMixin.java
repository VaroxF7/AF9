package com.af9.core.mixin.gt;

import com.af9.core.machine.console.BusPlacardWidget;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GT's multiblock screen (the Assembly Line's, the Research Station's, the Data Bank's, the computation arrays'...):
 * while the machine is part of a machine bus it shows only the bus card ({@link BusPlacardWidget}), as AF9's consoles
 * do. Machines that build their own screen are not touched.
 */
@Mixin(value = WorkableElectricMultiblockMachine.class, remap = false)
public abstract class WorkableElectricMultiblockMachineMixin {

    @Inject(method = "createUIWidget", at = @At("RETURN"), cancellable = true)
    private void af9$busPlacard(CallbackInfoReturnable<Widget> cir) {
        cir.setReturnValue(BusPlacardWidget.wrap(cir.getReturnValue(), (IMultiController) (Object) this));
    }
}
