package com.af9.core.startup.machines;

import com.af9.core.machine.DtpfModuleMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;

/**
 * The Dimensionally Transcendent Plasma Forge's modules: the controllers of the hall's six module slots (the hall's
 * pattern, {@link Dtpf}: a floating plate of 12 Inconel 738 Module Casings with a controller in its middle, behind the
 * middle injection casing at the top and bottom of each big ring; an empty slot is simply not part of the structure). A
 * module runs no recipe: the hall finds it, turns it to face outwards (the controller is rotatable to every side),
 * links it and gives its bonus to every run while the slot's casings are all in place
 * ({@code PlasmaForgeMachine.scanModules}, {@code DtpfModuleMachine.Kind}).
 * <pre>
 *   Parallel Processing Assembly      +10 parallels each (as the energy allows; no batch mode)            max 6
 *   Superheating Element              field heat x10 (1,000 K to 10,000 K), recipe EU/t x2                 max 1
 *   Osmic Retention Stabilisation Sequencer   field held at 30 percent at least, no run lost to a weak field, plasma burn halved   max 1
 *   Manipulated Celestial Forge       runs 1.5 times as fast, same EU/t                                    max 2
 * </pre>
 * Docs: docs/dtpf.md, docs/dtpf-modules.md
 */
public final class DtpfModules {

    private DtpfModules() {}

    public static void register() {
        module("parallel", "Parallel Processing Assembly");
        module("superheater", "Superheating Element");
        module("sequencer", "Osmic Retention Stabilisation Sequencer");
        module("forge", "Manipulated Celestial Forge");
    }

    // lint-loop: kind=parallel|superheater|sequencer|forge; name=Parallel Processing Assembly|Superheating Element|Osmic Retention Stabilisation Sequencer|Manipulated Celestial Forge
    private static void module(String kind, String name) {
        AF9Machines.multiblock("dtpf_module_" + kind, holder -> new DtpfModuleMachine(holder, DtpfModuleMachine.kindOf(kind)),
                b -> b
                        .langValue(name)
                        .rotationState(RotationState.ALL)
                        .appearanceBlock(AF9Machines.blockSupplier("af9:dtpf_module_casing"))
                        .tooltips(AF9Machines.lines("af9.dtpf_module_" + kind + ".tooltip.", 8))
                        // the controller alone: its casings are the hall's to check (they come with the slot)
                        .pattern(definition -> FactoryBlockPattern.start()
                                .aisle("S")
                                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                                .build())
                        .workableCasingModel(AF9Machines.rl("af9:block/dtpf_module_casing"),
                                AF9Machines.rl("gtceu:block/multiblock/dtpf_module_" + kind)));
    }
}
