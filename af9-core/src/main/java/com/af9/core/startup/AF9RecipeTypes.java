package com.af9.core.startup;

import com.af9.core.common.AF9Sounds;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

/**
 * AF9's GregTech recipe types (they were KubeJS startup scripts: GTCEuStartupEvents.registry('gtceu:recipe_type')).
 * Registered from GregTech's recipe type event in the order the scripts loaded (file name order), under the gtceu
 * namespace like before: the ids are unchanged ({@code gtceu:<name>}). Where each type's recipes and machine are:
 * docs/ and the machine classes; the comments say what the slots are for.
 */
public final class AF9RecipeTypes {

    private AF9RecipeTypes() {}

    private static final String MULTIBLOCK = "multiblock";
    private static final FillDirection L2R = FillDirection.LEFT_TO_RIGHT;

    /** One type: category, item in/out, fluid in/out, progress bar, sound. Returns it for the odd extra setting. */
    private static GTRecipeType type(String name, String category, IO io, int itemsIn, int itemsOut, int fluidsIn,
                                     int fluidsOut, ResourceTexture bar, SoundEntry sound) {
        GTRecipeType t = GTRecipeTypes.register(name, category);
        t.setEUIO(io);
        t.setMaxIOSize(itemsIn, itemsOut, fluidsIn, fluidsOut);
        t.setProgressBar(bar, L2R);
        t.setSound(sound);
        return t;
    }

    private static GTRecipeType multi(String name, int itemsIn, int itemsOut, int fluidsIn, int fluidsOut,
                                      ResourceTexture bar, SoundEntry sound) {
        return type(name, MULTIBLOCK, IO.IN, itemsIn, itemsOut, fluidsIn, fluidsOut, bar, sound);
    }

    public static void register() {
        // advanced_circuit_manufacturer: up to 16 items and 4 fluids in, one item out; computation through the
        // computation hatch
        multi("advanced_circuit_manufacturer", 16, 1, 4, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ASSEMBLER);

        // agri_sim: one seed (not a pack's circuit), water; the crop's yield and the seed back
        multi("agri_sim_elemental", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("agri_sim_tier2", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("agri_sim_tier3", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("agri_sim_tier4", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("agri_sim_tier5", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("agri_sim_crops", 1, 2, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);

        // asteroid_fission: the molten-salt loop's second half (hot FLiBe and water in, steam and cooled FLiBe out)
        multi("heat_exchanger", 0, 0, 2, 2, GuiTextures.PROGRESS_BAR_BOILER_HEAT, GTSoundEntries.BOILER);
        // cooling decladding: spent rods and water in, decladded dusts out
        multi("spent_fuel_pool", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_BATH, GTSoundEntries.BATH);
        // dusts straight to clad rods in one passive step
        multi("fuel_fabrication", 3, 1, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ASSEMBLER);

        // atmospheric_simulation: seven item inputs (the NVM mainframe lean takes processor, rack, dies, both 7 nm
        // calibration chips and wire)
        multi("atmospheric_simulation", 7, 1, 2, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ASSEMBLER);

        // bio_vat
        multi("bio_vat", 2, 0, 2, 1, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);

        // boule_melting: melt charges, seed crystal, crucible + protective gas -> boule
        multi("boule_melting", 3, 1, 1, 0, GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, GTSoundEntries.FURNACE);

        // cryogenics
        multi("dense_cooling", 0, 0, 1, 1, GuiTextures.PROGRESS_BAR_COMPRESS, GTSoundEntries.COOLING);
        multi("supercooling", 0, 0, 1, 1, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.COOLING);

        // dtpf: plasma and matter in, forged matter and fluids out (three of each)
        multi("plasma_forge", 3, 3, 3, 3, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ARC);

        // electric_implosion: the charged recipes (no explosive, no vapor), the Large machine's second mode
        type("charged_implosion", null, IO.IN, 2, 2, 0, 0, GuiTextures.PROGRESS_BAR_COMPRESS, GTSoundEntries.ARC);
        // dust and a second item in, the plate (and a little residue) out, the vapor in
        type("electric_implosion_compressor", null, IO.IN, 2, 2, 1, 1, GuiTextures.PROGRESS_BAR_COMPRESS,
                GTSoundEntries.ARC);

        // fab_machines: the semiconductor chemistry, modes of the normal GT machines (com.af9.core.fab.FabModes).
        // [id, items in, items out, fluids in, fluids out, bar, sound]
        fab("fab_synthesis", 3, 2, 4, 3, GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL);
        fab("fab_blending", 3, 2, 4, 3, GuiTextures.PROGRESS_BAR_MIXER, GTSoundEntries.MIXER);
        fab("fab_wet_processing", 3, 2, 4, 3, GuiTextures.PROGRESS_BAR_BATH, GTSoundEntries.BATH);
        fab("fab_distillation", 1, 1, 2, 6, GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.CHEMICAL);
        fab("fab_cryogenic_rectification", 1, 1, 2, 6, GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE,
                GTSoundEntries.COOLING);
        fab("fab_fractionation", 2, 1, 3, 2, GuiTextures.PROGRESS_BAR_ARROW_MULTIPLE, GTSoundEntries.BOILER);
        fab("fab_purification", 2, 1, 3, 2, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        fab("fab_electrolysis", 2, 2, 3, 4, GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER);
        fab("fab_electrofluorination", 2, 2, 3, 4, GuiTextures.PROGRESS_BAR_EXTRACT, GTSoundEntries.ELECTROLYZER);
        fab("fab_calcination", 3, 2, 2, 2, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE);
        fab("fab_cvd", 3, 2, 2, 2, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE);
        fab("fab_crystal_growth", 3, 2, 2, 2, GuiTextures.PROGRESS_BAR_CRYSTALLIZATION, GTSoundEntries.FURNACE);

        // litho_process: a blank wafer in, the coated wafer out; the track's chemicals in, the spent solvent out
        multi("wafer_coating", 2, 1, 5, 2, GuiTextures.PROGRESS_BAR_MIXER, GTSoundEntries.BATH);

        // me_resonance_fabricator: 9 items in, 1 out, 3 fluids in; the research slot takes a data orb
        multi("me_assembler", 9, 1, 3, 0, GuiTextures.PROGRESS_BAR_ASSEMBLER, GTSoundEntries.ASSEMBLER)
                .setHasResearchSlot(true);

        // micro_universe_orb (base-pack file, kept in the repo)
        type("micro_universe_reactor", "gregstar", IO.OUT, 12, 0, 6, 0, GuiTextures.PROGRESS_BAR_ARROW,
                GTSoundEntries.ARC).setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY);
        type("micro_universe_collector", "gregstar", IO.IN, 12, 12, 6, 6, GuiTextures.PROGRESS_BAR_ARROW,
                GTSoundEntries.ARC).setSlotOverlay(false, false, GuiTextures.SOLIDIFIER_OVERLAY);

        // particle_accelerator: one fluid input each, the coolant (neutron irradiation gives the empty cell back)
        multi("neutron_irradiation", 2, 2, 1, 0, GuiTextures.PROGRESS_BAR_MASS_FAB, AF9Sounds.PARTICLE_ACCELERATOR);
        multi("ion_collision", 2, 1, 1, 0, GuiTextures.PROGRESS_BAR_FUSION, AF9Sounds.PARTICLE_ACCELERATOR);
        multi("quark_synthesis", 2, 2, 1, 0, GuiTextures.PROGRESS_BAR_REPLICATOR, AF9Sounds.PARTICLE_ACCELERATOR);

        // photolithography: +3 fluids on every mode (the transistors' polyethylene, silicon and electrum). The orbital
        // station's modes sound like it, the rest play the Circuit Assembler's.
        // GT's chip wafers + the chanced broken wafer out (2 items out); a lens overlay on the last item slot
        litho("lithography_350nm", 2, 6, GTSoundEntries.ASSEMBLER);
        litho("lithography_200nm", 2, 8, GTSoundEntries.ASSEMBLER);
        litho("lithography_100nm", 2, 8, GTSoundEntries.ASSEMBLER);
        litho("lithography_80nm", 3, 8, GTSoundEntries.ASSEMBLER);
        litho("lithography_65nm", 3, 9, GTSoundEntries.ASSEMBLER);
        litho("lithography_50nm", 2, 10, AF9Sounds.ORBITAL_STATION);
        litho("lithography_20nm", 3, 10, AF9Sounds.ORBITAL_STATION);
        litho("lithography_7nm", 3, 11, AF9Sounds.ORBITAL_STATION);
        // chromodynium wafer, reticle, dry resist cartridge (the 1 nm transistors: polyethylene, silicon, electrum)
        multi("orbital_lithography", 3, 2, 3, 0, GuiTextures.PROGRESS_BAR_ARROW, AF9Sounds.ORBITAL_STATION);
        // plasma atomic soldering (UHV+ circuits), runs in the Hyper-Intensity Laser Engraver
        multi("plasma_soldering", 9, 1, 2, 0, GuiTextures.PROGRESS_BAR_ARROW, AF9Sounds.ORBITAL_STATION);

        // planetary_satellite: the rays: a reflector in the dish, liquid neutronium out
        multi("neutron_collection", 1, 0, 0, 1, GuiTextures.PROGRESS_BAR_MASS_FAB, GTSoundEntries.ARC);

        // plasma_transformer
        multi("plasma_transform", 2, 2, 2, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.COOLING);

        // precision_fabricator: seed, dopant and the crucible in; the polysilicon, a dopant and a gas in
        multi("high_temperature_precision_fabricator", 6, 1, 3, 0, GuiTextures.PROGRESS_BAR_CRYSTALLIZATION,
                GTSoundEntries.FURNACE);

        // sanguinite_hearth: dusts + circuit 8 in, coolant + optional helium in, 1000 mB molten out
        multi("sanguinite_hearth", 9, 0, 2, 1, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.FURNACE);

        // space_elevator
        multi("space_mining", 4, 1, 2, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ARC);
        multi("space_pumping", 2, 0, 2, 1, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ARC);
        // the modules run no recipes: the elevator flies the missions (SpaceModuleMachine)
        multi("space_module", 1, 1, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ARC);

        // spooling: the Wiremill's second mode (com.af9.core.fab.SpoolingMode)
        type("spooling", null, IO.IN, 2, 1, 0, 0, GuiTextures.PROGRESS_BAR_WIREMILL, GTSoundEntries.MOTOR);

        // staged_assembly: the merged recipe (all stages) has to fit
        multi("staged_assembly", 9, 2, 4, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.ASSEMBLER);

        // void_mining: MK2 and MK3 run each mode as its own copy of the type
        multi("void_mining_overworld", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_nether", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_end", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_asteroids", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_moon", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_overworld_mk2", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_nether_mk2", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_end_mk2", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_asteroids_mk2", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_moon_mk2", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_overworld_mk3", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_nether_mk3", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_end_mk3", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_asteroids_mk3", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);
        multi("void_mining_moon_mk3", 1, 4, 1, 0, GuiTextures.PROGRESS_BAR_ARROW, GTSoundEntries.CHEMICAL);

        // water_filtration: filter (not consumed) + circuit in, up to 4 debris out, dirty water + culture in,
        // water + reclaim out
        multi("water_filtration", 2, 4, 2, 2, GuiTextures.PROGRESS_BAR_BATH, GTSoundEntries.BATH);
    }

    private static void litho(String name, int itemsIn, int fluidsIn, SoundEntry sound) {
        multi(name, itemsIn, 2, fluidsIn, 0, GuiTextures.PROGRESS_BAR_ARROW, sound)
                .setSlotOverlay(false, false, true, GuiTextures.LENS_OVERLAY);
    }

    private static void fab(String id, int itemsIn, int itemsOut, int fluidsIn, int fluidsOut, ResourceTexture bar,
                            SoundEntry sound) {
        type(id, "af9_fab", IO.IN, itemsIn, itemsOut, fluidsIn, fluidsOut, bar, sound);
    }
}
