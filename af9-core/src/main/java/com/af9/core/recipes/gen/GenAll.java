package com.af9.core.recipes.gen;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/** Registers every generated recipe class (written by tools/port/record.js). */
public final class GenAll {

    private GenAll() {}

    public static void register(Consumer<FinishedRecipe> out) {
        Gen0Af9Notreepunching.register(out);
        GenAdvancedCircuitManufacturer.register(out);
        GenAe2Addons.register(out);
        GenAe2Components.register(out);
        GenAe2Cosmetics.register(out);
        GenAe2Devices.register(out);
        GenAe2Endgame.register(out);
        GenAe2Matter.register(out);
        GenAe2Processors.register(out);
        GenAe2Quartz.register(out);
        GenAe2World.register(out);
        GenAgriSim.register(out);
        GenAlloysAf9.register(out);
        GenAnnealedBronze.register(out);
        GenAromatics.register(out);
        GenAsteroidFission.register(out);
        GenAtmospheres.register(out);
        GenAtmosphericSimulation.register(out);
        GenBasepackTags.register(out);
        GenBioVat.register(out);
        GenBiofuels.register(out);
        GenBouleMelting.register(out);
        GenCeramicBoards.register(out);
        GenCeranium.register(out);
        GenChemicalReduction.register(out);
        GenChipUses.register(out);
        GenCircuitEtching.register(out);
        GenCleanroomHatch.register(out);
        GenCreateSteamAge.register(out);
        GenCryogenics.register(out);
        GenCwuServer.register(out);
        GenDiesel.register(out);
        GenDistillationFacility.register(out);
        GenDrillingFluid.register(out);
        GenDtpf.register(out);
        GenEarlyCircuits.register(out);
        GenElectricImplosion.register(out);
        GenElectronicsMetallurgy.register(out);
        GenEnergizingRods.register(out);
        GenErBridge.register(out);
        GenFabChemistry.register(out);
        GenFissionFuels.register(out);
        GenFusionPower.register(out);
        GenGreenhouseRecipes.register(out);
        GenHile.register(out);
        GenHostileNetworks.register(out);
        GenIndustrialCutter.register(out);
        GenInterconnects.register(out);
        GenLapisSulfonate.register(out);
        GenLargeChemicalReactor.register(out);
        GenLargeWiremill.register(out);
        GenLithiumBrine.register(out);
        GenLithoProcess.register(out);
        GenMeResonanceFabricator.register(out);
        GenMeResonanceFabricatorRecipes.register(out);
        GenMiner.register(out);
        GenMotors.register(out);
        GenMvEnergyHatch.register(out);
        GenNetherChemistry.register(out);
        GenNuclearRework.register(out);
        GenOil.register(out);
        GenOrbRecipes.register(out);
        GenOrbitalCasings.register(out);
        GenOrbitalModules.register(out);
        GenOrbitalUpgrade.register(out);
        GenParallelCap.register(out);
        GenPcbCoolant.register(out);
        GenPlanetMetals.register(out);
        GenPlanetarySatellite.register(out);
        GenPlasmaTransformer.register(out);
        GenPlatinumGroup.register(out);
        GenPrecisionFabricator.register(out);
        GenQuadHatches.register(out);
        GenRocketHull.register(out);
        GenRubber.register(out);
        GenSolders.register(out);
        GenSpaceElevator.register(out);
        GenStainlessFirebox.register(out);
        GenStainlessMolten.register(out);
        GenTitaniumSynthetic.register(out);
        GenUhvSuperconductor.register(out);
        GenVoltageCoils.register(out);
        GenWaterFiltration.register(out);
        GenWirelessEnergy.register(out);
        GenXpsBoard.register(out);
    }
}
