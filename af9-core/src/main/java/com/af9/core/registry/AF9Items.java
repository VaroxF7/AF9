package com.af9.core.registry;

import com.af9.core.AF9Core;
import com.af9.core.machine.DysonSails;
import com.af9.core.staged.StagedCovers;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * AF9's plain items: the ones that are a name, a texture and a tooltip, and get their meaning from the recipes and from
 * the machines that read them (wafers, chips, reticles, boule charges, fuel rods, the Mining Drones). They were KubeJS
 * items ({@code kubejs:<id>}) until AF9 Core took the pack's registrations over; a world from before is remapped
 * ({@link AF9Remaps}).
 * <p>
 * To add one: a line here, its name ({@code item.af9.<id>}) and tooltip lines ({@code item.af9.<id>.tooltip.<n>}) in
 * the lang file, a model ({@code assets/af9/models/item/<id>.json}) and its texture; then the dev run, which
 * writes the linters' list of what is registered ({@code tools/lint/README.md}). The recipes are KubeJS's
 * ({@code kubejs/server_scripts}).
 */
public final class AF9Items {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);

    /**
     * The nine wafer substrates, one lithography mode each ({@link com.af9.core.litho.LithoMode}; the server side of
     * the table is AF9_WAFERS in {@code server_scripts/mods/gtceu/photolithography.js}: keep the three in sync):
     * silicon 350 nm, phosphorus 200, naquadah 100, trinium 80, naquadria 65, neutronium 50, transmuted neutronium 20,
     * strange matter 7, chromodynium 1 nm (the Orbital Lithography Station). Trinium comes before naquadria because GT
     * smelts trinium at LuV and naquadria only at ZPM.
     */
    private static final String[] SUBSTRATES = { "silicon", "phosphorus", "naquadah", "trinium", "naquadria",
            "neutronium", "transmuted_neutronium", "strange_matter", "chromodynium" };
    /** The substrates GT has no blank wafer of: AF9 adds it. */
    private static final String[] NEW_WAFERS = { "trinium", "naquadria", "transmuted_neutronium", "strange_matter",
            "chromodynium" };
    /** GT's chips: the dies the cutter makes of the printed wafers. Each has a contaminated chip. */
    private static final String[] GT_CHIPS = { "ilc_chip", "ram_chip", "cpu_chip", "ulpic_chip", "lpic_chip",
            "simple_soc", "nand_memory_chip", "nor_memory_chip", "mpic_chip", "soc", "advanced_soc",
            "highly_advanced_soc", "nano_cpu_chip", "qbit_cpu_chip", "hpic_chip", "uhpic_chip" };
    /**
     * AF9's own chips, next to GT's (docs/semiconductor-factory.md): each has a chip wafer, a chip and a contaminated
     * chip. RF Transceiver, APU and MCU are silicon chips (350 nm), the ASIC phosphorus (200 nm), eDRAM, MRAM and
     * FeRAM trinium (80 nm), the VPU naquadria (65 nm), the TPU transmuted neutronium; then the families the finer
     * substrates open up: SAW Filter (naquadah), Photonic IC (trinium), Spin Logic (naquadria), TMD Logic (neutronium),
     * Memristor (transmuted neutronium), Quantum-Dot IC (strange matter), QRAM and QLOS (strange matter).
     */
    private static final String[] CHIPS = { "rf_transceiver", "apu", "mcu", "asic", "edram", "mram", "feram", "vpu",
            "tpu", "saw_filter", "photonic_ic", "spin_logic", "tmd_logic", "memristor", "quantum_dot_ic", "qram",
            "qlos" };
    /**
     * The printed chips and the index of each one's own substrate ({@link #SUBSTRATES}): it says the class of the
     * chip's own reticle. Keep it in sync with maskClass() in AF9_WAFERS (server script).
     */
    private static final Object[][] RETICLES = {
            { "ilc", 0 }, { "ram", 0 }, { "cpu", 0 }, { "ulpic", 0 }, { "lpic", 0 }, { "simple_soc", 0 },
            { "rf_transceiver", 0 }, { "apu", 0 }, { "mcu", 0 }, { "nand", 1 }, { "nor", 1 }, { "mpic", 1 },
            { "soc", 1 }, { "asic", 1 }, { "advanced_soc", 2 }, { "saw_filter", 2 }, { "edram", 3 }, { "mram", 3 },
            { "feram", 3 }, { "photonic_ic", 3 }, { "vpu", 4 }, { "spin_logic", 4 }, { "highly_advanced_soc", 5 },
            { "tmd_logic", 5 }, { "tpu", 6 }, { "memristor", 6 }, { "quantum_dot_ic", 7 }, { "qram", 7 },
            { "qlos", 7 } };
    /**
     * The mask classes, by the light they fit: chrome-on-quartz (350 and 200 nm), MoSi phase-shift (100, 80 and
     * 65 nm), reflective EUV (50 nm and finer). The class of a chip's own reticle has no suffix in the item's id.
     */
    private static final String[] MASK_SUFFIXES = { "", "_psm", "_euv" };
    /** The substrates a boule of is pulled from melt charges and a seed crystal (EBF Boule Melting). */
    private static final String[] MELTS = { "silicon", "phosphorus", "naquadah", "trinium", "naquadria", "neutronium",
            "strange_matter", "chromodynium" };
    /** The Mining Drones of the Space Elevator ({@link com.af9.core.elevator.SpaceElevatorMachine}). */
    public static final int DRONES = 4;
    /** The id of a drone: this and its tier ({@code af9:space_mining_drone_mk2}). */
    public static final String DRONE = "space_mining_drone_mk";

    static {
        // ---- Asteroid fission: the FX-1 Reactor's fuel ----
        item("fx_fuel_pellet", 64, 2);
        item("fx_fuel_rod", 64, 2);
        item("fx_spent_fuel_rod", 64, 2);

        // ---- Boule Melting: ten melt charges, a seed crystal and a crucible make a boule ----
        for (String melt : MELTS) {
            item(melt + "_melt_charge", 64, 1);
            item(melt + "_seed_crystal", 64, 1);
        }
        // the boules of silicon, phosphorus, naquadah and neutronium are GT's
        for (String boule : new String[] { "trinium", "naquadria", "strange_matter", "chromodynium" }) {
            item(boule + "_boule", 64, 0);
        }
        item("fused_quartz_crucible", 64, 1);
        item("tritanium_crucible", 64, 1);

        // ---- AF9's chips ----
        for (String chip : CHIPS) {
            item(chip + "_wafer", 64, 1);
            item(chip + "_chip", 64, 1);
            // handled without gloves outside a clean room (WaferContamination)
            item("contaminated_" + chip + "_chip", 64, 1);
        }
        // the mask blanks of the finer chips: an attenuated phase-shift blank and a reflective EUV blank
        item("phase_shift_mask_blank", 64, 2);
        item("euv_mask_blank", 64, 1);
        // eDRAM next to the processor on one package: the cache chiplet
        item("edram_cpu_package", 64, 1);
        item("edram_soc_package", 64, 1);
        // Linear circuit ladder (§circuits_af9.js): silicon interposer packages that move the value
        // from loose passives into lithographed dies. ASIC package bridges HV-EV, photonic package XPS.
        item("asic_package", 64, 1);
        item("photonic_package", 64, 1);
        // Post-UHV circuits: XPS (photonic + spintronic) and NVM (memristive + quantum-dot).
        // Each tier has a processor and its mainframe so the ladder stays linear past UHV.
        item("xps_processor", 64, 1);
        item("xps_processor_mainframe", 64, 1);
        item("nvm_processor", 64, 1);
        item("nvm_processor_mainframe", 64, 1);
        // Pico circuits: GTNH's Pico components, made in the Advanced Circuit Manufacturer; parts of the XPS and NVM
        // processors (circuits_af9.js)
        item("pico_board", 64, 1);
        item("cleansed_pico_board", 64, 1);
        item("pico_cpu", 64, 1);
        item("organized_pico_circuit", 64, 1);
        item("processed_pico_circuit_casing", 64, 1);
        item("pico_circuit_rack", 64, 1);
        // AI Acceleration Card: TPU + memristor on a wetware board, traced in fine sanguinite wire
        // (circuits_af9.js, UHV); the compute card of the new DTPF (docs/dtpf.md).
        item("ai_acceleration_card", 64, 1);

        // ---- Particle Accelerator ----
        item("beryllium_spallation_target", 64, 1);
        item("magnetic_trap", 64, 1);
        item("qgp_trap", 64, 1);

        // ---- Photolithography ----
        item("photomask_blank", 64, 0);
        // adsorbent of the XCDA dryer; a saturated one is baked dry in a furnace
        item("molecular_sieve", 64, 1);
        item("saturated_molecular_sieve", 64, 1);
        // EUV optics: nothing refracts 13.5 nm light, every optic is a Mo/Si multilayer mirror on ULE glass
        item("ule_glass_substrate", 64, 1);
        item("mo_si_mirror", 64, 2);
        // without gravity there is no spin coating: the orbital station deposits its resist from the vapour
        item("dry_resist_cartridge", 64, 1);

        // ---- The reticles (photomasks): a chip's own class, and one of every finer class ----
        for (Object[] reticle : RETICLES) {
            int substrate = (Integer) reticle[1];
            int own = substrate <= 1 ? 0 : substrate <= 4 ? 1 : 2;
            for (int mask = own; mask < MASK_SUFFIXES.length; mask++) {
                item(reticle[0] + (mask == own ? "" : MASK_SUFFIXES[mask]) + "_reticle", 1, 2);
            }
        }

        // ---- Create: the circuit under assembly (server_scripts/mods/gtceu/early_circuits.js) ----
        item("incomplete_circuit", 1, 0);

        // ---- Space Elevator ----
        for (int tier = 1; tier <= DRONES; tier++) item(DRONE + tier, 1, 3);

        // ---- Dyson Swarm: the sails (the swarm's modules in GTNH). Tiers and yields: DysonSwarmMachine ----
        for (String sail : DysonSails.IDS) item(sail, 64, 2);

        // ---- Powah: the shaping molds of the Energizing Orb Mk2 (server_scripts/mods/powah/orb_recipes.js) ----
        // GT's extruder molds cover the rod, bolt, gear, small gear and rotor; the screw mold is AF9's (GT has none).
        item("screw_extruder_mold", 64, 2);

        // ---- Create: the mold of the glass tube (the spout fills it, the press empties it into a tube and the mold) ----
        item("glass_tube_mold", 64, 2);
        item("glass_tube_mold_filled", 64, 1);

        // ---- Staged Assembly: the step detector cover (a ComponentItem, so it can place the cover) ----
        ITEMS.register("staged_step_detector", () -> {
            ComponentItem item = ComponentItem.create(new Item.Properties().stacksTo(64));
            item.attachComponents(new CoverPlaceBehavior(StagedCovers.STAGED_STEP_DETECTOR));
            return item;
        });

        // ---- Wafers ----
        for (String wafer : NEW_WAFERS) item(wafer + "_wafer", 64, 0);
        // coated wafers: a blank primed and coated with its node's resist (the Coater Track); the lithography machines
        // print these. The 1 nm station deposits its resist dry, so chromodynium has none
        for (String substrate : SUBSTRATES) {
            if (!substrate.equals("chromodynium")) item("coated_" + substrate + "_wafer", 64, 1);
        }
        // failed prints and handled wafers, one of each per substrate
        for (String substrate : SUBSTRATES) {
            item("broken_" + substrate + "_wafer", 64, 1);
            item("contaminated_" + substrate + "_wafer", 64, 1);
        }
        // GT's chips handled without gloves outside a clean room
        for (String chip : GT_CHIPS) item("contaminated_" + chip, 64, 1);
    }

    private AF9Items() {}

    private static void item(String id, int stackSize, int tooltipLines) {
        ITEMS.register(id, () -> new TooltipItem(new Item.Properties().stacksTo(stackSize), tooltipLines));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
