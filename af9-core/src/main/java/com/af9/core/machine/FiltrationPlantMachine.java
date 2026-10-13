package com.af9.core.machine;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;

/**
 * The Water Filtration Plant (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/water_filtration.js}): an open stainless basin behind glass that
 * takes dirty water and filters it to distilled water, skimming the floating debris off the top.
 * <p>
 * GT's default multiblock behaviour; the visible basin (water, debris, skimmer arm) is
 * {@link com.af9.core.client.render.FiltrationBasinRender}, through {@link IFiltrationPlantMachine}.
 */
public class FiltrationPlantMachine extends WorkableElectricMultiblockMachine implements IFiltrationPlantMachine {

    /** The filtration recipe type (gtceu namespace). */
    public static final String RECIPE_TYPE = "water_filtration";

    public FiltrationPlantMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public boolean isBasinFormed() {
        return isFormed();
    }

    @Override
    public boolean isBasinActive() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    @Override
    public float getBasinProgress(float partialTick) {
        if (!getRecipeLogic().isWorking()) return -1F;
        int duration = getRecipeLogic().getDuration();
        if (duration <= 0) return 0F;
        float p = (getRecipeLogic().getProgress() + partialTick) / duration;
        return Math.max(0F, Math.min(1F, p));
    }

    /** Common setup: the controller is the filtration type's icon on its EMI / JEI pages where GT left none. */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void registerRecipeInfo() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu",
                "water_filtration_plant"));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                    RECIPE_TYPE);
            return;
        }
        if (type.getIconSupplier() == null && definition != null) type.setIconSupplier(definition::asStack);
    }
}
