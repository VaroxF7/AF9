package com.af9.core.recipes.gen;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/** Registers every generated recipe class (written by tools/port/record.js). */
public final class GenAll {

    private GenAll() {}

    public static void register(Consumer<FinishedRecipe> out) {
        GenAdvancedCircuitManufacturer.register(out);
        GenAnnealedBronze.register(out);
        GenAromatics.register(out);
        GenBiofuels.register(out);
        GenCeranium.register(out);
        GenCwuServer.register(out);
        GenDiesel.register(out);
        GenElectronicsMetallurgy.register(out);
        GenFabChemistry.register(out);
        GenFissionFuels.register(out);
        GenFusionPower.register(out);
        GenInterconnects.register(out);
        GenLapisSulfonate.register(out);
        GenLargeChemicalReactor.register(out);
        GenLithiumBrine.register(out);
        GenLithoProcess.register(out);
        GenNetherChemistry.register(out);
        GenOil.register(out);
        GenOrbitalCasings.register(out);
        GenPlatinumGroup.register(out);
        GenQuadHatches.register(out);
        GenTitaniumSynthetic.register(out);
        GenWirelessEnergy.register(out);
    }
}
