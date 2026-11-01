package com.af9.core.startup.machines;

import com.af9.core.machine.OrbitalModuleMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

/**
 * The Orbital Lithography Array Mk2's modules: small multiblocks of their own that stand in the six module slots in the
 * rim of the printing chamber's deck (orbital_mk2_layout, 'Y'; {@code OrbitalLithographyMachine.modules()}), as the Space
 * Elevator's Mining Modules stand in the tower's. They run no recipe: the Array finds them when it forms and gives every
 * print the bonus of each module whose own structure stands ({@code OrbitalModuleMachine.Kind}). Modules of one kind
 * stack, up to a maximum.
 * <pre>
 *   Parallel Processing Computer      +10 parallels each (as the energy allows)         max 6
 *   Energy Efficiency Module          -5% energy of the recipes (not the auxiliary)     max 6
 *   Endion Evaporation Annihilation Stack   recipes 10% faster                          max 2
 *   Stabilisation Sequencer           half the auxiliary power, running and idle         max 1
 * </pre>
 */
public final class OrbitalModules {

    private OrbitalModules() {}

    public static void register() {
        module("parallel", "Parallel Processing Computer");
        module("efficiency", "Energy Efficiency Module");
        module("evaporation", "Endion Evaporation Annihilation Stack");
        module("sequencer", "Stabilisation Sequencer");
    }

    // lint-loop: kind=parallel|efficiency|evaporation|sequencer; name=Parallel Processing Computer|Energy Efficiency Module|Endion Evaporation Annihilation Stack|Stabilisation Sequencer
    private static void module(String kind, String name) {
        AF9Machines.multiblock("orbital_module_" + kind, holder -> new OrbitalModuleMachine(holder, OrbitalModuleMachine.kindOf(kind)),
                b -> b
                        .langValue(name)
                        .rotationState(RotationState.NON_Y_AXIS)
                        .appearanceBlock(AF9Machines.blockSupplier("gtceu:inert_machine_casing"))
                        .tooltips(AF9Machines.lines("af9.orbital_module_" + kind + ".tooltip.", 8))
                        // a module is a column: its controller with a PTFE casing above and below it, the slot's (as the elevator's)
                        .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP,
                                RelativeDirection.BACK)
                                .aisle("M", "S", "M")
                                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                                .where('M', AF9Machines.blocks("gtceu:inert_machine_casing")
                                        .or(AF9Machines.blocks("gtceu:nonconducting_casing"))
                                        .or(AF9Machines.blocks("af9:orbital_deck_casing")))
                                .build())
                        .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_inert_ptfe"),
                                AF9Machines.rl("gtceu:block/multiblock/orbital_module_" + kind)));
    }
}
