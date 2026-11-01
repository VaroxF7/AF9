package com.af9.core.startup.machines;

import com.af9.core.machine.MEResonanceFabricatorMachine;
import com.af9.core.machine.part.ResonanceHatchPartMachine;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;

import net.minecraft.network.chat.Component;

/**
 * AF9 - Certus Resonating Manufacturer: TerraFirmaGreg's ME Assembler (Core-Modern, MEAssemblerMachine and
 * TFGMultiMachines, LGPL-3.0; credits in CREDITS-textures.md, licence in licenses/) copied into GTCEu with a structure
 * of AF9's own: a cage of AE2 casings and steel pipe casings around a core of vibrant quartz glass, the Resonating
 * Fluix Hatch and the Data Access Hatch on the deck. The Liquid Resonating Fluix in the hatch is the stabiliser: while
 * there is some, the machine batches its crafts; every craft cycle drinks a few millibuckets of it. No overclocks.
 * Recipe type: gtceu:me_assembler (id as in TFG, so its recipes can be taken over unchanged).
 * Controller logic, the stabiliser hatch and the recipe page: AF9 Core (MEResonanceFabricatorMachine,
 * ResonanceHatchPartMachine). Blocks: af9:ae2_machine_casing (AF9Blocks), gtceu:steel_pipe_casing, ae2:quartz_vibrant_glass.
 *
 * Differences from TFG: parts have a maximum only (one energy hatch at most, one maintenance hatch at most), the machine
 * has no 'moon only' rule (its recipes ask for a cleanroom instead), and the Budding Certus is gone: the fluix
 * stabiliser takes its place.
 */
public final class MeResonanceFabricator {

    private MeResonanceFabricator() {}

    public static void register() {
        // Resonating Fluix Hatch: the stabiliser input of the Certus Resonating Manufacturer, and the only place
        // Liquid Resonating Fluix goes into it. HV-UHV. Tank: 1000 mB x 2^tier (8,000 mB at HV).

        AF9Machines.tiered("resonance_hatch", new int[] { GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV,
            GTValues.UHV },
                (holder, tier) -> new ResonanceHatchPartMachine(holder, tier),
                (tier, builder) -> builder
                        .langValue("" + GTValues.VN[tier] + " Resonating Fluix Hatch")
                        .rotationState(RotationState.ALL)
                        .colorOverlayTieredHullModel("overlay_pipe_in_emissive", (String) null, "overlay_fluid_hatch")
                        .abilities(ResonanceHatchPartMachine.RESONANCE_INPUT)
                        .tooltips(Component.translatable("af9.resonance_hatch.tooltip.0"),
                            Component.translatable("af9.resonance_hatch.tooltip.1"),
                            Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity", "" + (1000 << tier))));

        // 5 wide x 4 high x 5 deep, aisles back -> front (the controller's last), rows bottom -> top. The bottom layer
        // is the deck: AE2 casings (C) with the hatches, the Data Access Hatch (H) and the steel pipe casing (P) in the
        // middle; above it a cage of AE2 casings around a core of vibrant quartz glass (G), the walls' inside is any
        // block. Parts have a maximum only (setMaxGlobalLimited(max, preview count)).

        AF9Machines.multiblock("me_assembler", holder -> new MEResonanceFabricatorMachine(holder), b -> b
                .langValue("Certus Resonating Manufacturer")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("me_assembler"))
                .alwaysTryModifyRecipe(true)
                .recipeModifiers(MEResonanceFabricatorMachine.RESONANCE)
                .appearanceBlock(AF9Machines.blockSupplier("af9:ae2_machine_casing"))
                .tooltips(AF9Machines.lines("af9.me_fabricator.tooltip.", 5))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle(" CHC ", " C C ", " C C ", " C C ")
                    .aisle("CCCCC", "CCGCC", "CCGCC", " C C ")
                    .aisle("CCPCC", "CC CC", "CCGCC", " C C ")
                    .aisle("CCCCC", "CCGCC", "CCGCC", " C C ")
                    .aisle(" CXC ", " C C ", " C C ", " C C ")
                    .where('X', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('P', AF9Machines.blocks("gtceu:steel_pipe_casing"))
                    .where('G', AF9Machines.blocks("ae2:quartz_vibrant_glass"))
                    .where('C', AF9Machines.blocks("af9:ae2_machine_casing")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9, 3))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(3, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(ResonanceHatchPartMachine.RESONANCE_INPUT).setMaxGlobalLimited(1, 1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1, 1)))
                    .where('H', Predicates.dataHatchPredicate(AF9Machines.blocks("af9:ae2_machine_casing")))
                    .where(' ', Predicates.any())
                    .build())
                .shapeInfos(definition -> MEResonanceFabricatorMachine.previews(definition))
                // AE2 casings with the manufacturer's own controller face
                .workableCasingModel(AF9Machines.rl("af9:block/ae2_machine_casing"), AF9Machines.rl("gtceu:block/multiblock/me_resonator")));
    }
}
