package com.af9.core.startup.machines;

import com.af9.core.machine.ChemicalPlantMachine;
import com.af9.core.pattern.ChemicalPlantPipes;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Chemical Plant: a 6 x 4 x 6 plant of real GT fluid pipes (Bronze Huge, Stainless Steel Quadruple, Niobium-Titanium
 * Huge), taken 1:1 from Janne's structure file (testing.nbt), block by block with the file's exact pipe connections: the
 * pattern asks for the exact pipe and the exact connection mask at every pipe spot (af9-core ChemicalPlantPipes), and the
 * controller connects the pipes of an auto-built plant itself while it is unformed. The file's casings (Module Casing, a
 * stand-in until the plant gets its own) take the hatches; the controller replaces the casing in the front wall (the file
 * had none). Test version: it runs the Chemical Reactor's and the Large Chemical Reactor's recipes.
 *
 * Rows bottom -&gt; top, aisles back -&gt; front (the controller's last), columns as in the file's x, left to right as seen
 * from the front. The file stands in the world with the controller facing SOUTH. A space is anything.
 */
public final class ChemicalPlant {

    private ChemicalPlant() {}

    public static void register() {
        AF9Machines.multiblock("chemical_plant", holder -> new ChemicalPlantMachine(holder), b -> b
                .langValue("Chemical Plant")
                .rotationState(RotationState.NON_Y_AXIS)
                .allowFlip(false)
                .recipeTypes(AF9Machines.type("chemical_reactor"), AF9Machines.type("large_chemical_reactor"))
                .recipeModifiers(GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("af9:dtpf_module_casing"))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle(" CC   ", " CC   ", " a    ", " a    ", " bcde ", "  f   ")
                    .aisle(" CCC  ", " CCC  ", "  g   ", "      ", "    h ", "  i   ")
                    .aisle("  CCC ", "  CCC ", "  j a ", "  k a ", "  k l ", "  m   ")
                    .aisle("   CC ", "   SC ", "      ", "      ", "      ", "      ")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('C', AF9Machines.blocks("af9:dtpf_module_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('a', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 3))
                    .where('b', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 33))
                    .where('c', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 50))
                    .where('d', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 48))
                    .where('e', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 24))
                    .where('f', ChemicalPlantPipes.pipe("gtceu:stainless_steel_quadruple_fluid_pipe", 9))
                    .where('g', ChemicalPlantPipes.pipe("gtceu:niobium_titanium_huge_fluid_pipe", 9))
                    .where('h', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 12))
                    .where('i', ChemicalPlantPipes.pipe("gtceu:stainless_steel_quadruple_fluid_pipe", 12))
                    .where('j', ChemicalPlantPipes.pipe("gtceu:stainless_steel_quadruple_fluid_pipe", 7))
                    .where('k', ChemicalPlantPipes.pipe("gtceu:stainless_steel_quadruple_fluid_pipe", 3))
                    .where('l', ChemicalPlantPipes.pipe("gtceu:bronze_huge_fluid_pipe", 5))
                    .where('m', ChemicalPlantPipes.pipe("gtceu:stainless_steel_quadruple_fluid_pipe", 5))
                    .build())
                .workableCasingModel(AF9Machines.rl("af9:block/dtpf_module_casing"), AF9Machines.rl("gtceu:block/multiblock/large_chemical_reactor")));
    }
}
