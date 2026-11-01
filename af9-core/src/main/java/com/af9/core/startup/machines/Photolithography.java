package com.af9.core.startup.machines;

import com.af9.core.fiber.FiberLinkPortPartMachine;
import com.af9.core.machine.AF9MachineModels;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.PhotolithographyLineMachine;
import com.af9.core.machine.PhotolithographyScannerMachine;
import com.af9.core.machine.part.AirConditioningHatchPartMachine;
import com.af9.core.machine.part.CoolantHatchPartMachine;
import com.af9.core.startup.AF9Machines;
import com.af9.core.startup.layout.OrbitalMk2Layout;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * AF9 - Photolithography Line (Mk1), Photolithography Scanner (Mk2) and Orbital Lithography Station
 * Realistic lithography: a coater/developer track feeding a stepper. One exposure mode per wafer substrate (see
 * wafers.js): the Mk1 line prints 350, 200 and 100 nm, the Mk2 scanner 80 and 65 nm, the orbital station 50, 20, 7
 * and 1 nm (in orbit). A print is always GT's own chip wafer, more of them per blank on a higher substrate.
 * Each mode uses the light source its real node used and the resist made for that light:
 *   350 nm            mercury-lamp i-line 365 nm             DNQ-novolac resist
 *   200 nm            KrF excimer laser 248 nm               chemically amplified PHOST resist
 *   100, 80 nm        ArF excimer laser 193 nm (dry)         chemically amplified methacrylate resist
 *   65, 50 nm         ArF immersion (water under the lens)   same, + high-k gate from 50 nm
 *   20, 7 nm          EUV 13.5 nm (tin plasma), 7 nm high-NA metal-oxide (tin-oxo) EUV resist
 *   1 nm              X-ray free-electron laser, in orbit    dry resist cartridges (no spin coating without gravity)
 * The line (versions 1-3) and the scanner (versions 1-2) grow like the Assembly Line (longer projection lens, better
 * light source); a version runs the modes up to its own and runs lower ones faster and with fewer broken wafers.
 * Both machines keep an exposure vacuum (cleanliness 0-100): the pumps raise it while there is energy and no
 * maintenance problem, every finished wafer drops it by 10-15, and every print can break: the finer the node and the
 * dirtier the vacuum, the likelier. See LithoMachine in af9-core.
 * Recipes: server_scripts/mods/gtceu/photolithography.js. Controller behaviour: AF9 Core (af9-core/).
 */
public final class Photolithography {

    private Photolithography() {}

    private static final String[][] ORBITAL_BASIC = {
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","        DDDDDDDDD        ","                         ","                         ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","         CCCCCCC         ","      DDDCCCCCCCDDD      ","         CCCCCCC         ","            C            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","       CC   C   CC       ","    DDDCC  FHF  CCDDD    ","       CC   C   CC       ","            C            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","     CC     C     CC     ","   DDCC    FHF    CCDD   ","     CC     C     CC     ","                         ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","    C       C       C    ","  DDC      FHF      CDD  ","    C               C    ","            L            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","   C        C        C   ","  DC       FHF       CD  ","   C                 C   ","            L            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","   C        C        C   "," DDC       FHF       CDD ","   C                 C   ","           LLL           ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","  C         C         C  "," DC        FHF        CD ","  C                   C  ","           LLL           ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","  C         C         C  ","DDC        FHF        CDD","  C        F F        C  ","          LLLLL          ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            "," C          C          C ","DC        HHHHH        CD"," C         F F         C ","         LLLLLLL         ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","            A            ","            A            ","            A            ","            C            "," C         CCC         C ","DC       HHHHHHH       CD"," C         FFF         C ","        LLLLLLLLL        ","           OOO           "},
        {"                         ", "            A            ", "            A            ", "            A            ","            A            ", "           AAA           ","           AAA           ","           AA            ","            A            ","            A            ","           CBC           ","           CBC           ","           CBC           "," C        CCBCC        C ","DCFFFFFFFHHHHHHHFFFFFFFCD"," C      FFFF FFFF      C ","      LLLLLLLLLLLLL      ","          OOOOO          "},
        {"            A            ", "           AAA           ", "           AAA           ", "           AAA           ","           ABA           ", "           ABA           ","           ABA           ","           ABA           ","           ABA           ","          AABAA          ","          ABBBA          ","          ABBBA          "," CCC     CCBBBCC     CCC "," CCCCCCCCCCBBBCCCCCCCCCC ","DCHHHHHHHHHHHHHHHHHHHHHCD"," CCC       F F       CCC "," CC LLLLLLLLLLLLLLLLL CC ","       OOOOOKOOOOO       "},
        {"                         ", "            A            ", "            A            ", "            A            ","            A            ", "           AAA           ","           AAA           ","            AA           ","            A            ","            A            ","           CBC           ","           CBC           ","           CBC           "," C        CCBCC        C ","DCFFFFFFFHHHHHHHFFFFFFFCD"," C      FFFF FFFF      C ","      LLLLLLLLLLLLL      ","          OOOOO          "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","            A            ","            A            ","            A            ","            C            "," C         CCC         C ","DC       HHHHHHH       CD"," C         FFF         C ","        LLLLLLLLL        ","           OOO           "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            "," C          C          C ","DC        HHHHH        CD"," C         F F         C ","         LLLLLLL         ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","  C         C         C  ","DDC        FHF        CDD","  C        F F        C  ","          LLLLL          ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","  C         C         C  "," DC        FHF        CD ","  C                   C  ","           LLL           ","            O            "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","   C        C        C   "," DDC       FHF       CDD ","   C                 C   ","           LLL           ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","   C        C        C   ","  DC       FHF       CD  ","   C                 C   ","            L            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","    C       C       C    ","  DDC      FHF      CDD  ","    C               C    ","            L            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","     CC     C     CC     ","   DDCC    FHF    CCDD   ","     CC     C     CC     ","                         ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","       CC   C   CC       ","    DDDCC  FHF  CCDDD    ","       CC   C   CC       ","            C            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","            C            ","         CCCCCCC         ","      DDDCCCCCCCDDD      ","         CCCCCCC         ","            C            ","                         "},
        {"                         ", "                         ", "                         ", "                         ","                         ", "                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","                         ","        DDDDDDDDD        ","                         ","                         ","                         "}
    };

    /**
     * The station's pattern: the basic one from {@link #ORBITAL_BASIC}, the Array Mk2 from the generated slices (Janne's own
     * build, orbital.nbt, 1:1: a slim spire of superconducting coils and magnets standing through three rings (a big one in the
     * middle, a smaller one above and below, each with four spokes), a halo round the crown with four arms that fire their
     * lasers at the computer core under the controller, a collision chamber with a floating wafer and a deck round it, and the
     * plasma lance (drawn, not built) leaving the open foot; see {@link OrbitalMk2Layout}). 73 aisles, 99 rows, 73 characters;
     * the controller is the last row of the middle aisle's middle column; rows bottom -> top, levels below the controller count
     * d = 98 - row. The rings' numbers are af9-core's (OrbitalLithographyMachine.BANDS_MK2): keep them in step.
     */
    private static BlockPattern orbitalPattern(MultiblockMachineDefinition definition, String[][] slices, boolean mk2) {
        // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview)): the track
        // chemicals through fluid input hatches, the supercooled coolant through coolant hatches, computation
        // (7 and 1 nm) through a computation hatch and the 1 nm research through a data hatch. Every hatch goes
        // on the PTFE casings of the top deck and nowhere else.
        var energy = Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(3, 1);
        var laser = Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1);
        var coolant = Predicates.abilities(CoolantHatchPartMachine.COOLANT_INPUT).setMaxGlobalLimited(2, 1);
        var computation = Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 1);
        var data = Predicates.abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(1, 1);
        var optical = Predicates.abilities(PartAbility.OPTICAL_DATA_RECEPTION).setMaxGlobalLimited(1, 0);
        var fluids = Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(11, 1);
        var itemsIn = Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1);
        var itemsOut = Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1);
        var maintenance = Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1);
        var hatches = energy.or(laser).or(coolant).or(computation).or(data).or(optical).or(fluids).or(itemsIn).or(itemsOut)
            .or(maintenance);
        // the top deck: PTFE casings and every hatch; hatches go here and nowhere else
        // (the Mk2's hatches go on the nonconducting casing of the printing chamber's deck, 'O')
        var deck = AF9Machines.blocks(mk2 ? "gtceu:nonconducting_casing" : "gtceu:inert_machine_casing").or(hatches);
        var pattern = AF9Machines.aisles(FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.FRONT,
            RelativeDirection.UP), slices)
            .where('K', Predicates.controller(Predicates.blocks(definition.get())))
            .where('A', AF9Machines.blocks("gtceu:hsse_frame"))
            .where('S', AF9Machines.blocks("gtceu:superconducting_coil"))
            // the basic station's plasma-injection coils stay HSS-G (the Mk2 has none: its structure is Janne's build)
            .where('B', AF9Machines.blocks("gtceu:hssg_coil_block"))
            // the Mk2's structure has casings of its own in place of GT's (OrbitalCasings): the Combined Structure Casing
            // for the Stress-Proof, the Truss ('D', inside of the rings' bands) and the Girder ('E', outside) for the Sturdy HSS-E
            .where('D', AF9Machines.blocks(mk2 ? "af9:orbital_truss_casing" : "gtceu:sturdy_machine_casing"))
            .where('E', AF9Machines.blocks(mk2 ? "af9:orbital_girder_casing" : "gtceu:sturdy_machine_casing"))
            .where('F', AF9Machines.blocks(mk2 ? "gtceu:tungsten_carbide_frame" : "gtceu:hsss_frame"))
            // the Mk2 tower's magnet pillars and light seams (textures from Cosmic Frontiers)
            .where('M', AF9Machines.blocks("af9:orbital_magnet_casing"))
            .where('N', AF9Machines.blocks("af9:orbital_light_casing"))
            .where('H', AF9Machines.blocks(mk2 ? "gtceu:atomic_casing" : "gtceu:shock_proof_cutting_casing"))
            .where('L', AF9Machines.blocks(mk2 ? "af9:orbital_combined_structure_casing" : "gtceu:stress_proof_casing"))
            .where('Q', AF9Machines.blocks("gtceu:inert_machine_casing"))
            // Y: the basic station's module slot is nothing (air); in the Mk2 a module slot of the Array Mk2: a module (any
            // type) or its casing; the station finds them when it forms (the module's own structure is a casing above and
            // below it, nonconducting casing here)
            .where('Y', mk2 ? OrbitalLithographyMachine.modules().or(AF9Machines.blocks("gtceu:nonconducting_casing")) : Predicates.air())
            .where('C', AF9Machines.blocks("gtceu:nonconducting_casing"))
            .where('O', deck)
            .where(' ', Predicates.any());
        if (mk2) {
            pattern = pattern.where('G', AF9Machines.blocks("gtceu:fusion_glass"))
                .where('V', AF9Machines.blocks("gtceu:heat_vent"))
                .where('T', AF9Machines.blocks("gtceu:tungstensteel_turbine_casing"))
                .where('P', AF9Machines.blocks("gtceu:computer_casing"))
                .where('g', AF9Machines.blocks("create:metal_girder"))
                .where('n', AF9Machines.blocks("gtceu:magnetic_neodymium_block"))
                .where('s', AF9Machines.blocks("gtceu:magnetic_samarium_block"))
                // the collision chamber's four rotor holders: UV or better, the rotors in them are for show (af9-core)
                .where('R', OrbitalLithographyMachine.rotorHolders())
                // the wafer's cell: air, the floating wafer is drawn in it
                .where('#', Predicates.air());
        }
        return pattern.build();
    }

    public static void register() {
        AF9Machines.multiblock("photolithography_line", holder -> new PhotolithographyLineMachine(holder), b -> b
                .rotationState(RotationState.NON_Y_AXIS)
                // machine modes, in order; switched with GT's mode tab, the console's tiles show the active one
                .recipeTypes(AF9Machines.type("lithography_350nm"), AF9Machines.type("lithography_200nm"),
                    AF9Machines.type("lithography_100nm"))
                // LITHO_GATE: only prints a mode the line's version allows, when the two energy hatches can supply its EU/t
                // STRIP_BROKEN: the chanced broken wafer is only for the recipe viewers, the break roll decides
                // Mk1: perfect overclocks and batch mode (GT's: once overclocked below 5 s, several prints in one run, each
                // rolling its own break), nothing else (no version speed-up, no parallel hatch). A print draws 1A of its mode's tier.
                .recipeModifiers(LithoMachine.LITHO_GATE, LithoMachine.STRIP_BROKEN,
                    GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:plascrete"))
                .tooltips(AF9Machines.lines("af9.photolithography_line.tooltip.", 17))
                // 3 wide x 3 high x 10-12 long, built from plascrete like a clean room. Aisles run from the front (controller)
                // to the back (light source): the controller has to come before the repeatable lens aisle, or GT's auto-build
                // (terminal) places the structure off the controller. Each aisle lists its rows bottom -> middle -> top.
                // Versions 1-3 like the Assembly Line's length: 3-5 projection-lens slices, and the light source must allow the
                // version (mercury lamp V1, KrF excimer laser V2, ArF excimer laser V3). One preview page each.
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK)
                    // --- Coater / developer track ---
                    .aisle("III", "IMI", "CFC") // cassette station (wafers in and out) + controller
                    .aisle("CSC", "WXW", "FPF") // HMDS prime and spin coater
                    .aisle("CKC", "CHC", "FFF") // bake plates (soft bake, post-exposure bake, hard bake) and chill plate
                    .aisle("CSC", "WXW", "FPF") // developer: TMAH puddle and rinse
                    // --- Stepper (exposure tool) ---
                    .aisle("CRC", "WRW", "CCC") // wafer XY stage
                    .aisle("CCC", "WTW", "CCC").setRepeatable(3, 5) // projection lens: one slice per version + 2
                    .aisle("CCC", "CRC", "CCC") // reticle stage
                    .aisle("CCC", "CLC", "CCC") // light source / illuminator
                    .where('M', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
                    .where('I', AF9Machines.blocks("gtceu:plascrete")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1)))
                    .where('C', AF9Machines.blocks("gtceu:plascrete")
                        // a print draws 1A of its mode's tier; the hatches' voltage decides the modes (a second hatch is allowed, not needed)
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                        // a computation hatch: the optical proximity correction's computation (an extra)
                        .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 0))
                        // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                        .or(Predicates.abilities(AirConditioningHatchPartMachine.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('F', AF9Machines.blocks("af9:plascrete_filter_casing"))     // fan filter units
                    .where('R', AF9Machines.blocks("gtceu:stainless_steel_gearbox"))      // robots and stages
                    .where('S', AF9Machines.blocks("gtceu:steel_gearbox"))                // spin motors
                    .where('X', AF9Machines.blocks("gtceu:solid_machine_casing"))         // process cups (MV: no PTFE yet)
                    .where('P', AF9Machines.blocks("af9:plascrete_pipe_casing"))       // chemical dispense lines
                    .where('H', AF9Machines.blocks("gtceu:cupronickel_coil_block"))       // hot plates
                    .where('K', AF9Machines.blocks("gtceu:frostproof_machine_casing"))    // aluminium chill plates
                    .where('T', AF9Machines.blocks("gtceu:tempered_glass"))               // lens elements
                    .where('W', AF9Machines.blocks("gtceu:cleanroom_glass"))
                    .where('L', AF9Machines.blocks("gtceu:purple_lamp")                   // mercury i-line lamp (V1)
                        .or(AF9Machines.blocks("af9:krf_excimer_laser"))                 // V2
                        .or(AF9Machines.blocks("af9:arf_excimer_laser")))                // V3
                    .build())
                .shapeInfos(definition -> PhotolithographyLineMachine.versionShapes(definition))
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/cleanroom/plascrete"),
                    AF9Machines.rl("gtceu:block/multiblock/gcym/large_engraving_laser")));

        // Photolithography Scanner (Mk2): a step-and-scan tool for 80 and 65 nm, a cleanroom tube 3 x 3 of plascrete, 10
        // long at version 1 and 12 at version 2. Aisles front (controller) -> back, rows bottom -> top: the front with the
        // controller, a cap (a Plascrete Filter Casing over the tube), a window section (cleanroom glass on both sides,
        // stainless steel gearboxes over and under the tube: the wafer stages), the track (Plascrete Pipe Casings under the
        // tube, filter casings over it), the back window run (2 aisles a version: the lens slices), a cap and the back. The
        // tube stays empty. Its ArF excimer laser sits in a slot of its screen, not in the structure. Versions 1-2: 2-3
        // window sections of 4 cleanroom glass. Hatches go on any plascrete. AF9 Core: PhotolithographyScannerMachine.

        AF9Machines.multiblock("photolithography_scanner", holder -> new PhotolithographyScannerMachine(holder), b -> b
                .langValue("Photolithography Scanner Mk2")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(AF9Machines.type("lithography_80nm"), AF9Machines.type("lithography_65nm"))
                // the line's modifiers, batch mode included
                .recipeModifiers(LithoMachine.LITHO_GATE, LithoMachine.STRIP_BROKEN,
                    PhotolithographyLineMachine.LITHO_VERSION, GTRecipeModifiers.OC_PERFECT, GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:plascrete"))
                .tooltips(AF9Machines.lines("af9.photolithography_scanner.tooltip.", 10))
                .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK)
                    .aisle("CCC", "CMC", "CCC") // front with the controller
                    .aisle("CCC", "C#C", "CFC") // cap
                    .aisle("CGC", "W#W", "CGC") // front window section: the wafer stages
                    .aisle("CGC", "W#W", "CGC")
                    .aisle("CPC", "C#C", "CFC") // track: resist and developer lines under the tube
                    .aisle("CPC", "C#C", "CFC")
                    .aisle("CGC", "W#W", "CGC").setRepeatable(2, 4) // back window run: 2 aisles a version (the lens slices)
                    .aisle("CCC", "C#C", "CFC") // cap
                    .aisle("CCC", "CCC", "CCC") // back
                    .where('M', Predicates.controller(Predicates.blocks(definition.get())))
                    // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count)); any
                    // plascrete of the shell may be one
                    .where('C', AF9Machines.blocks("gtceu:plascrete")
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 2))
                        // a computation hatch: the optical proximity correction's computation (an extra)
                        .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(1, 0))
                        // air cooling: the hatches carry the print's heat (cooling units, see air_conditioning.js)
                        .or(Predicates.abilities(AirConditioningHatchPartMachine.AIR_CONDITIONING).setMaxGlobalLimited(2, 1))
                        // the fibre to its Logistics Hub (af9-core fiber): the hub's buses and hatches feed the scanner through it
                        .or(Predicates.abilities(FiberLinkPortPartMachine.FIBER_LINK).setMaxGlobalLimited(2, 0))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(9, 1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1)))
                    .where('F', AF9Machines.blocks("af9:plascrete_filter_casing"))     // fan filter units
                    .where('G', AF9Machines.blocks("gtceu:stainless_steel_gearbox"))      // wafer stages
                    .where('P', AF9Machines.blocks("af9:plascrete_pipe_casing"))       // track lines
                    .where('W', AF9Machines.blocks("gtceu:cleanroom_glass"))              // windows: the lens slices
                    .where('#', Predicates.air())                                        // the tube
                    .build())
                .shapeInfos(definition -> PhotolithographyLineMachine.versionShapes(definition,
                    PhotolithographyLineMachine.MK2))
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/cleanroom/plascrete"),
                    AF9Machines.rl("gtceu:block/multiblock/gcym/large_engraving_laser")));

        // Orbital Lithography Station: a 25 x 25 platform, 18 high. The top deck of inert PTFE casing carries the
        // controller and the hatches; under it lie the stress-proof deck plate, the shock-proof exposure deck (cross
        // beams) in a ring of sturdy casing, non-conducting spokes and rims, HSS-S trusses, and the X-ray undulator mast:
        // an HSS-G coil column in HSS-E frames reaching 12 blocks down. Prints 50, 20, 7 and 1 nm, only in orbit
        // (af9-core OrbitalLithographyMachine). Pattern from the sol_array design, unchanged; rows bottom -> top.
        // While it prints, a light ring like GT's fusion ring glows just inside the rim, at the level of the exposure deck
        // (3 below the controller, radius 9.6: clear of the rim, it only crosses the four beams), in the node's colour.
        // The controller faces up out of the top deck (look down when you place it); the station turns with it in any
        // direction. The pattern is laid out for that: rows along the controller's front (up), aisles along its up; with
        // the controller facing up the blocks sit exactly where the old horizontal controller had them. Coolant: every
        // print draws a supercooled fluid through the coolant hatches (af9-core OrbitalLithographyMachine.COOLANT);
        // 7 and 1 nm draw computation (computation hatch) and need their research (data hatch). While switched on and
        // powered its magnetic field gives the space around it normal gravity (Ad Astra; af9-core OrbitalField).
        // Array Mk2 (extended, a screwdriver on the controller switches sizes like the Space Elevator's screen
        // switch): Janne's 73 x 99 x 73 spire with three spinning rings and a thruster lance below (above, af9-core BANDS_MK2). Only the Mk2 runs plasma atomic soldering (gtceu:plasma_soldering):
        // UHV+ circuits deposited ion-by-ion with plasma solder, in orbit.

        AF9Machines.multiblock("orbital_lithography_station", holder -> new OrbitalLithographyMachine(holder), b -> b
                .rotationState(RotationState.ALL)
                .allowExtendedFacing(true)
                .recipeTypes(AF9Machines.type("lithography_50nm"), AF9Machines.type("lithography_20nm"),
                    AF9Machines.type("lithography_7nm"), AF9Machines.type("orbital_lithography"))
                // LITHO_GATE: only in orbit, with the recipe's full EU/t, a sealed vacuum and (researched prints) a data hatch;
                // STRIP_BROKEN: the break roll decides; COOLANT: adds the coolant (faster with a better one);
                // MK2_GATE: the Mk2's own types only in orbit on the Array Mk2 (extended) once its beams are
                // aligned (the 1 nm prints check the same in the station's own gate); FOCUS: the Mk2's focus bonus (shorter
                // runs; its break-chance share is in the station's machineBreakFactor); COILS: the Mk2's coil tier (shorter runs, less EU/t; break chance and focus in the station);
                // MODULES: the Array Mk2's modules (af9-core OrbitalModuleMachine: parallels as the energy allows, less EU/t, faster runs; the
                // coolant and everything else scale with the parallels, there is no parallel hatch); perfect overclocks (the machine caps them at UXV, af9-core
                // getOverclockTier); then batch mode (the console's switch; af9-core BATCH: a print overclocked below a minute
                // runs as a batch about a minute long, up to 64 prints, the coolant too).
                .recipeModifiers(LithoMachine.LITHO_GATE, LithoMachine.STRIP_BROKEN, OrbitalLithographyMachine.COOLANT,
                    OrbitalLithographyMachine.MK2_GATE, OrbitalLithographyMachine.FOCUS, OrbitalLithographyMachine.COILS,
                    OrbitalLithographyMachine.MODULES, GTRecipeModifiers.OC_PERFECT, OrbitalLithographyMachine.BATCH)
                .appearanceBlock(AF9Machines.blockSupplier("gtceu:inert_machine_casing"))
                .tooltips(AF9Machines.lines("af9.orbital_lithography_station.tooltip.", 37))
                .pattern(definition -> {
                    // GT asks for this once: both sizes are built, the extended one is the machine's own to switch to
                    OrbitalLithographyMachine.setExtendedPattern(orbitalPattern(definition, OrbitalMk2Layout.slices(), true));
                    return orbitalPattern(definition, ORBITAL_BASIC, false);
                })
                .workableCasingModel(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_inert_ptfe"),
                    AF9Machines.rl("gtceu:block/multiblock/fusion_reactor"))
                // the same model plus the light ring: centre 3 behind the controller (below, as it faces up), radius 9.6
                // (the Array Mk2's three rings: af9-core reads them through ringSpecs), tube 0.25, lying across the
                // controller's front axis. The numbers live in af9-core (RING_*), which also burns whatever touches the lit
                // ring. Plasma soldering glows hotter (ringGlow 2, af9-core).
                .model(AF9MachineModels.workableCasingWithLightRing(AF9Machines.rl("gtceu:block/casings/solid/machine_casing_inert_ptfe"),
                    AF9Machines.rl("gtceu:block/multiblock/fusion_reactor"), OrbitalLithographyMachine.RING_UP,
                    OrbitalLithographyMachine.RING_BACK, OrbitalLithographyMachine.RING_RADIUS,
                    OrbitalLithographyMachine.RING_THICKNESS, "front"))
                .hasBER(true)
                // GT would draw the preview for a controller facing north (the platform on its edge): show it facing up;
                // the basic station's page (the Array Mk2 is too big for a preview)
                .shapeInfos(definition -> OrbitalLithographyMachine.previews(definition)));
    }
}
