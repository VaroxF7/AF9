package com.af9.core.startup.machines;

import com.af9.core.elevator.SpaceElevatorMachine;
import com.af9.core.elevator.SpaceMissionMachine;
import com.af9.core.elevator.SpaceModuleMachine;
import com.af9.core.elevator.SpacePumpingModuleMachine;
import com.af9.core.machine.AF9MachineModels;
import com.af9.core.machine.part.CoolantHatchPartMachine;
import com.af9.core.startup.AF9Machines;
import com.af9.core.startup.layout.SpaceElevatorLayout;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

/**
 * AF9 - The Space Elevator, GTNH's: a tower on a cable that reaches into space, the pack's renewable ore source from ZPM on.
 * Behaviour: af9-core (com.af9.core.elevator.SpaceElevatorMachine: the asteroids, the motors, the modules, the cable; the
 * cable and the climber that rides it: com.af9.core.client.render.SpaceElevatorRender). Recipes:
 * server_scripts/mods/gtceu/space_elevator.js. Spec: docs/space-elevator.md
 *
 * A Mining Drone (not used up) in the drone slot of its screen (or an input bus), 50 to 100 buckets of hydrogen and of a
 * supercooled coolant in the fluid hatches and 4 to 32 amps of ZPM energy for minutes send an expedition to a random
 * asteroid: the output buses hold its ore, tens of stacks of raw ore. The asteroids are made from GT's ore veins; the better
 * the drone, the more of them it reaches.
 * As in GTNH the work is the modules': Mining Modules in the module slots fly one expedition each at once, and the motors'
 * tier says how many slots are powered.
 * Instead of an asteroid an expedition can go to a planet and bring home a fluid (a liquid mission, picked on the screen):
 * GTNH's Space Pumping table (af9-core, PlanetCatalog), the same drone, fluids and energy, into fluid output hatches.
 *
 * The structure is GTNH's, block for block (SE_MAIN): 35 x 35 and 43 high, a floor of concrete, a central column of motors
 * round an empty shaft with the cable on top of it, and a tapering frame of base casing and stress-proof casing around it.
 * Its extended size (SE_EXTENSION, switched on the controller's screen) adds a ring of 47 x 47 with twelve more module slots.
 *
 * The slices of the two sizes are made by {@link SpaceElevatorLayout} (tables, unfolding and the controller's place).
 */
public final class SpaceElevator {

    private SpaceElevator() {}

    private static final String[] ROMAN = { "I", "II", "III" };
    private static final String[] MODULE_TOOLTIPS = { "af9.space_mining_module_mk1.tooltip.",
        "af9.space_mining_module_mk2.tooltip.", "af9.space_mining_module_mk3.tooltip." };

    /** The pattern of such slices: aisles front -> back (a slice each), rows top -> bottom, as the table is written. */
    private static BlockPattern sePattern(MultiblockMachineDefinition definition, String[][] slices) {
        var casing = "af9:space_elevator_base_casing";
        var pattern = AF9Machines.aisles(FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.DOWN,
            RelativeDirection.BACK), slices);
        // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)). One set of them for
        // the whole tower, so the maximums count across it. GTNH's places: the energy in the bottom centre casings, the buses
        // and the hatches of the fluids there or in the module slots. The supercooled coolant has its Coolant Hatches, as
        // in AF9's other cooled machines: up to 4. They hold an eighth of a fluid hatch (ZPM 128 B, UV 256 B, UHV 512 B); GT
        // takes a recipe's fluids from every fluid input, so the coolant of a run bigger than they hold sits in the fluid
        // input hatches, with the hydrogen. The fluid of a liquid mission goes to fluid output hatches
        var energy = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2);
        var laser = Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2, 0);
        // the computation the modules need (the elevator's screen says how much): data hatches for the optical cables
        var computation = Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 0);
        var itemsIn = Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1);
        var coolant = Predicates.abilities(CoolantHatchPartMachine.COOLANT_INPUT).setMaxGlobalLimited(4, 1);
        var fluidsOut = Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(6, 1);
        var fluidsIn = Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1);
        var itemsOut = Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(12, 2);
        // a Coolant Hatch is a fluid input hatch too: here it is a part of its own, counted once
        SpaceElevatorMachine.plainFluidHatches(fluidsIn);
        // what the preview shows and the terminal builds with: ZPM parts, not GT's first ones (ULV, which hold nothing an
        // expedition needs)
        SpaceElevatorMachine.zpmFirst(energy);
        SpaceElevatorMachine.zpmFirst(laser);
        SpaceElevatorMachine.zpmFirst(computation);
        SpaceElevatorMachine.zpmFirst(itemsIn);
        SpaceElevatorMachine.zpmFirst(coolant);
        SpaceElevatorMachine.zpmFirst(fluidsOut);
        SpaceElevatorMachine.zpmFirst(fluidsIn);
        SpaceElevatorMachine.zpmFirst(itemsOut);
        // The order matters to the terminal: it builds a place with the first kind that is not full, and counts the place for
        // every kind that is not full. So each kind gets its maximum less that of the kind before it, and the maximums have to
        // rise along the list (a kind whose maximum is no higher than that of the kind before it is never built): 4 energy
        // hatches, and 2 input buses, 2 Coolant Hatches, 2 fluid output hatches, 2 fluid input hatches and 4 output buses
        var power = energy.or(laser).or(computation);
        var buses = itemsIn.or(coolant).or(fluidsOut).or(fluidsIn).or(itemsOut);
        return pattern
            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', AF9Machines.blocks("af9:ultra_high_strength_concrete_floor"))
            .where('D', AF9Machines.blocks(casing))
            .where('E', AF9Machines.blocks("gtceu:stress_proof_casing"))
            .where('F', AF9Machines.blocks("af9:space_elevator_internal_structure"))
            .where('H', AF9Machines.blocks("gtceu:neutronium_frame"))
            .where('C', SpaceElevatorMachine.motors())                                // any tier, all of one tier
            .where('B', SpaceElevatorMachine.cable())                                 // with open sky above it
            .where('X', AF9Machines.blocks(casing).or(power).or(buses))
            .where('M', AF9Machines.blocks(casing).or(buses))
            .where('I', SpaceElevatorMachine.modules().or(AF9Machines.blocks(casing)))  // any tier
            .where('-', Predicates.air())
            .where(' ', Predicates.any())
            .build();
    }

    public static void register() {
        AF9Machines.multiblock("space_elevator", holder -> new SpaceElevatorMachine(holder), b -> b
                .langValue("Space Elevator")
                .rotationState(RotationState.NON_Y_AXIS)
                // upright only, as GTNH's
                .allowExtendedFacing(false)
                .allowFlip(false)
                // the elevator itself runs no recipe: its Mining Modules fly the missions (GTNH's design). The missions are listed
                // here all the same, so the recipe viewers show them on the tower (SpaceElevatorMachine.attachSideTabs: no mode tab)
                .recipeTypes(AF9Machines.type("space_module"), AF9Machines.type("space_mining"),
                    AF9Machines.type("space_pumping"))
                .appearanceBlock(AF9Machines.blockSupplier("af9:space_elevator_base_casing"))
                .tooltips(AF9Machines.lines("af9.space_elevator.tooltip.", 11))
                .pattern(definition -> {
                    // GT asks for this once: both sizes are built, the extended one is the machine's own to switch to
                    SpaceElevatorMachine.setExtendedPattern(sePattern(definition, SpaceElevatorLayout.slices(true)));
                    return sePattern(definition, SpaceElevatorLayout.slices(false));
                })
                // the structure preview's two pages: the basic and the extended tower
                .shapeInfos(definition -> SpaceElevatorMachine.previews(definition))
                .workableCasingModel(AF9Machines.rl("af9:block/space_elevator_base_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/fusion_reactor"))
                // the same model plus the cable and the climber on it; where the cable block is from the controller lives in
                // af9-core
                .model(AF9MachineModels.workableCasingWithSpaceElevator(AF9Machines.rl("af9:block/space_elevator_base_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/fusion_reactor"), SpaceElevatorMachine.CABLE_UP,
                    SpaceElevatorMachine.CABLE_BACK))
                .hasBER(true));

        // ---- The Mining Modules: GTNH's elevator modules, each a small multiblock of its own that stands in a module slot ('I' above)
        // and flies the missions (af9-core SpaceModuleMachine: the drone, the fuel and the coolant in its hatches, the cargo out of
        // them, the energy from the buffer the elevator fills). Its structure is the slot's casings round it, where the slot's
        // hatches go: the same parts as the tower's; the side towards the shaft is the tower's business. Parts have a maximum only,
        // never a required count (setMaxGlobalLimited(max, preview count)); the two sides are alike, so the module forms whichever
        // way it faces.
        for (int tier : new int[] { 1, 2, 3 }) {
            String roman = ROMAN[tier - 1];
            // lint-loop: tier=1|2|3; roman=I|II|III
            AF9Machines.multiblock("space_mining_module_mk" + tier, holder -> new SpaceModuleMachine(holder, tier), b -> b
                    .langValue("Space Mining Module MK-" + roman)
                    .rotationState(RotationState.NON_Y_AXIS)
                    .recipeTypes(AF9Machines.type("space_mining"))
                    // the missions only (the modifier is the engine: drone, asteroid, expeditions at once)
                    .recipeModifiers(SpaceMissionMachine.MISSION)
                    .appearanceBlock(AF9Machines.blockSupplier("af9:space_elevator_base_casing"))
                    .tooltips(AF9Machines.lines(MODULE_TOOLTIPS[tier - 1], 4))
                    .pattern(definition -> {
                        var slotPart = AF9Machines.blocks("af9:space_elevator_base_casing")
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4, 1))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                            .or(Predicates.abilities(CoolantHatchPartMachine.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                            // what else the tower has in the slot's casings (energy hatches and the like) is not the module's
                            .or(Predicates.any());
                        return FactoryBlockPattern.start()
                            .aisle("MMM", "MSM", "MMM")
                            .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                            .where('M', slotPart)
                            .build();
                    })
                    .workableCasingModel(AF9Machines.rl("af9:block/space_elevator_base_casing"),
                        AF9Machines.rl("gtceu:block/multiblock/space_mining_module_mk" + tier)));
        }

        // ---- The Pumping Module: a module of its own in a module slot, as the Mining Modules, for the fluids of the planets. Its
        // recipe is set in the module (the drone of the planet's reach and the programmed circuit that picks the fluid), the run
        // is the tower's (energy, hydrogen and coolant from its hatches, switched on with it, the fluid to its output hatches)
        AF9Machines.multiblock("space_pumping_module", holder -> new SpacePumpingModuleMachine(holder), b -> b
                .langValue("Space Pumping Module")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("space_pumping"))
                .recipeModifiers(SpaceMissionMachine.MISSION)
                .appearanceBlock(AF9Machines.blockSupplier("af9:space_elevator_base_casing"))
                .tooltips(AF9Machines.lines("af9.space_pumping_module.tooltip.", 4))
                .pattern(definition -> {
                    var slotPart = AF9Machines.blocks("af9:space_elevator_base_casing")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4, 1))
                        .or(Predicates.abilities(CoolantHatchPartMachine.COOLANT_INPUT).setMaxGlobalLimited(2, 1))
                        .or(Predicates.any());
                    return FactoryBlockPattern.start()
                        .aisle("MMM", "MSM", "MMM")
                        .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                        .where('M', slotPart)
                        .build();
                })
                .workableCasingModel(AF9Machines.rl("af9:block/space_elevator_base_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/space_pumping_module")));
    }
}
