package com.af9.core.devtest;

import com.af9.core.fab.FabFamily;
import com.af9.core.machine.fab.FabTieredMachine;

import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

/**
 * Dev runs only: what a KubeJS script needs of GT's {@link SimpleTieredMachine} and its subclasses on a dedicated
 * server. GT marks those classes with a client-only annotation, so a script's {@code Java.loadClass} of them fails
 * there (KubeJS looks at a class's annotations; Java code using the classes is fine). The dev run's copy of a script
 * calls these instead.
 */
public final class FabBridge {

    private FabBridge() {}

    public static MetaMachine create(IMachineBlockEntity holder, int tier, Int2IntFunction tankScaling,
                                     FabFamily family) {
        return new FabTieredMachine(holder, tier, tankScaling, family);
    }

    public static int temperatureOf(int tier) {
        return FabTieredMachine.temperatureOf(tier);
    }

    public static EditableMachineUI editableUI(ResourceLocation id, GTRecipeType type) {
        return SimpleTieredMachine.EDITABLE_UI_CREATOR.apply(id, type);
    }
}
