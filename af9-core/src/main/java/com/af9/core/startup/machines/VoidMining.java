package com.af9.core.startup.machines;

import com.af9.core.machine.AmperageOverclock;
import com.af9.core.machine.VoidMinerMachineMK2;
import com.af9.core.machine.VoidMinerMachineMK3;
import com.af9.core.startup.AF9Machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Void Miner modes: the miner's five areas as recipe types (the machine itself stays GT's: VoidMinerMachine
 * takes it over in af9-core). Recipes: server_scripts/mods/gtceu/miner.js. Modes 1-5: Overworld, Nether, End,
 * Asteroids, Moon; a programmed circuit picks the ore in the area. Drilling fluid in, tenfold raw ore out.
 *
 * MK2 and MK3 are AF9's own multiblocks, with the structures of GTNH's Void Miners (see the patterns below): every mode gets its own
 * recipe type again, suffixed _mk2 / _mk3 with the base type's settings, and the controllers are gtceu:void_miner_mk2
 * and gtceu:void_miner_mk3. af9-core's VoidMinerMachine.install() looks those up in the machine registry and attaches
 * the classes plus the af9.void_miner_mkX.tooltip.0..3 lines itself, so no tooltips() here. Front textures:
 * gtceu:block/multiblock/void_miner_mk2 and .../void_miner_mk3 (kubejs assets, overlay_front*: GTNH's ore drill face).
 */
public final class VoidMining {

    private VoidMining() {}

    // MK2 and MK3 have the structures of GTNH's Void Miners (GT5-Unofficial, bwcrossmod.galacticgreg.MTEVoidMiners: the
    // ZPM miner's 9 x 13 x 8 tower and the UV miner's 9 x 16 x 9 one), in AF9's blocks: the mining casings and the
    // bolted casings of the originals (af9-core AF9Blocks), GT frames for GTNH's (naquadah alloy; tritanium for the
    // adamantium). Transcribed by the tool that checked the block counts against the originals' tooltips. Aisles back ->
    // front (the controller's last), rows bottom -> top; a space is anything. The pillar up the middle is the mining pipe,
    // the four legs carry the hatches. Parts have a maximum only, never a required count (setMaxGlobalLimited(max,
    // preview count)); every variant takes drilling fluid, so IMPORT_FLUIDS stays open.
    // chars: S controller, A item pipe casing, B mining casing, C frame, D / E the two bolted casings, F a hatch spot
    private static final String[][] MK2_SHAPE = {
            {"   F F   ", "   F F   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"         ", "   B B   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"  CCCCC  ", "  CABAC  ", "  CABAC  ", "  CCCCC  ", "  C   C  ", "  C   C  ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"F C   C F", "FBA   ABF", "  A   A  ", "  CDADC  ", "   DCD   ", "    C    ", "    C    ", "    C    ", "    C    ", "         ", "         ", "         ", "         "},
            {"  C E C  ", "  B E B  ", "  B E B  ", "  CAEAC  ", "   CEC   ", "   CEC   ", "   CEC   ", "   CEC   ", "   CEC   ", "    C    ", "    C    ", "    C    ", "    C    "},
            {"F C   C F", "FBA   ABF", "  A   A  ", "  CDADC  ", "   DCD   ", "    C    ", "    C    ", "    C    ", "    C    ", "         ", "         ", "         ", "         "},
            {"  CCCCC  ", "  CABAC  ", "  CASAC  ", "  CCCCC  ", "  C   C  ", "  C   C  ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"   F F   ", "   F F   ", "   D D   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
        };

    private static final String[][] MK3_SHAPE = {
            {"   F F   ", "   F F   ", "   E E   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"  C   C  ", "         ", "   B B   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {" CCCCCCC ", "  CBBBC  ", "  CBBBC  ", "  CEEEC  ", "  C   C  ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"F C   C F", "F B   B F", "EBB   BBE", "  EAAAE  ", "   DDD   ", "   DCD   ", "   DCD   ", "   DCD   ", "   DCD   ", "    C    ", "    C    ", "    C    ", "         ", "         ", "         ", "         "},
            {"  C D C  ", "  B D B  ", "  B D B  ", "  EADAE  ", "   DDD   ", "   CDC   ", "   CDC   ", "   CDC   ", "   CDC   ", "   CDC   ", "   CDC   ", "   CDC   ", "    C    ", "    C    ", "    C    ", "    C    "},
            {"F C   C F", "F B   B F", "EBB   BBE", "  EAAAE  ", "   DDD   ", "   DCD   ", "   DCD   ", "   DCD   ", "   DCD   ", "    C    ", "    C    ", "    C    ", "         ", "         ", "         ", "         "},
            {" CCCCCCC ", "  CBBBC  ", "  CBSBC  ", "  CEEEC  ", "  C   C  ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"  C   C  ", "         ", "   B B   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
            {"   F F   ", "   F F   ", "   E E   ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         ", "         "},
        };

    public static void register() {
        // MK1: ATM9's own void miner (its startup script miner.js), registered here now: a 3 x 3 x 7 tower of
        // aluminium-tranquillite casing round a frame pillar; the casing is the hatch spot, with exactly one maintenance hatch
        AF9Machines.multiblock("void_miner", WorkableElectricMultiblockMachine::new, b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(AF9Machines.type("void_miner"))
                .recipeModifier(GTRecipeModifiers.OC_NON_PERFECT)
                .appearanceBlock(AF9Machines.blockSupplier("af9:aluminium_tranquillite_casing"))
                .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("XXX", "#F#", "#F#", "#F#", "###", "###", "###")
                    .aisle("XXX", "FCF", "FCF", "FCF", "#F#", "#F#", "#F#")
                    .aisle("XSX", "#F#", "#F#", "#F#", "###", "###", "###")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('X', AF9Machines.blocks("af9:aluminium_tranquillite_casing")
                        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                    .where('C', AF9Machines.blocks("af9:aluminium_tranquillite_casing"))
                    .where('F', AF9Machines.blocks("gtceu:aluminium_tranquillite_frame"))
                    .where('#', Predicates.any())
                    .build())
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_stable_titanium"),
                    AF9Machines.rl("gtceu:block/multiblock/large_miner")));

        // MK2: twice the ore per run as the base miner, twice the power and the speed
        AF9Machines.multiblock("void_miner_mk2", holder -> new VoidMinerMachineMK2(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("void_mining_overworld_mk2"),
                    AF9Machines.type("void_mining_nether_mk2"),
                    AF9Machines.type("void_mining_end_mk2"),
                    AF9Machines.type("void_mining_asteroids_mk2"),
                    AF9Machines.type("void_mining_moon_mk2"))
                .recipeModifier(AmperageOverclock.OC)
                .appearanceBlock(AF9Machines.blockSupplier("af9:mining_black_plutonium_casing"))
                .pattern(definition -> AF9Machines.aisles(FactoryBlockPattern.start(), MK2_SHAPE)
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where(' ', Predicates.any())
                    .where('A', AF9Machines.blocks("af9:black_plutonium_item_pipe_casing"))
                    .where('B', AF9Machines.blocks("af9:mining_black_plutonium_casing"))
                    .where('C', AF9Machines.blocks("gtceu:naquadah_alloy_frame"))
                    .where('D', AF9Machines.blocks("af9:bolted_naquadah_alloy_casing"))
                    .where('E', AF9Machines.blocks("af9:rebolted_naquadah_alloy_casing"))
                    .where('F', AF9Machines.blocks("af9:mining_black_plutonium_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .build())
                .workableCasingModel(AF9Machines.rl("af9:block/mining_black_plutonium_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/void_miner_mk2")));

        // MK3: three times the ore per run as the base miner, three times the power and the speed
        AF9Machines.multiblock("void_miner_mk3", holder -> new VoidMinerMachineMK3(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("void_mining_overworld_mk3"),
                    AF9Machines.type("void_mining_nether_mk3"),
                    AF9Machines.type("void_mining_end_mk3"),
                    AF9Machines.type("void_mining_asteroids_mk3"),
                    AF9Machines.type("void_mining_moon_mk3"))
                .recipeModifier(AmperageOverclock.OC)
                .appearanceBlock(AF9Machines.blockSupplier("af9:mining_neutronium_casing"))
                .pattern(definition -> AF9Machines.aisles(FactoryBlockPattern.start(), MK3_SHAPE)
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where(' ', Predicates.any())
                    .where('A', AF9Machines.blocks("af9:black_plutonium_item_pipe_casing"))
                    .where('B', AF9Machines.blocks("af9:mining_neutronium_casing"))
                    .where('C', AF9Machines.blocks("gtceu:tritanium_frame"))
                    .where('D', AF9Machines.blocks("af9:rebolted_iridium_casing"))
                    .where('E', AF9Machines.blocks("af9:bolted_iridium_casing"))
                    .where('F', AF9Machines.blocks("af9:mining_neutronium_casing")
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(4, 2))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .build())
                .workableCasingModel(AF9Machines.rl("af9:block/mining_neutronium_casing"),
                    AF9Machines.rl("gtceu:block/multiblock/void_miner_mk3")));
    }
}
