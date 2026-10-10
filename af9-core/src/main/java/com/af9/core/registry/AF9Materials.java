package com.af9.core.registry;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.DustProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AF9's materials. GregTech makes their items, blocks and fluids (dust, ingot, bucket ...) and, where a material has
 * components or a blast temperature, the recipes that follow from them. They go into GregTech's own material registry,
 * so their ids are {@code gtceu:<name>} ({@code gtceu:kovar_ingot}, the fluid {@code gtceu:endion}), as they were when
 * KubeJS registered them: recipes, tanks and worlds name them that way.
 * <p>
 * Most of them are a form, a colour and a formula and nothing else ({@link #dust}, {@link #liquid}, {@link #gas}): a
 * material without components gets no electrolyzer or centrifuge recipe from GT, so no chain can be skipped. The
 * recipes are KubeJS's ({@code kubejs/server_scripts/mods/gtceu}); the names are {@code material.gtceu.<name>} in
 * {@code kubejs/assets/gtceu/lang}.
 * <p>
 * To add one: a line in the method of its topic (or a new method, called from {@link #register}), its name in the lang
 * file, then the dev run, which writes the linters' list of what is registered ({@code tools/lint/README.md}).
 */
public final class AF9Materials {

    /**
     * The two quark materials' own animated looks: Strange Matter a dark violet void with twinkling glints,
     * Chromodynium a pearl metal with a sheen sweeping through its colour charge. Models and textures:
     * {@code assets/gtceu/{models,textures}/item/material_sets/<set>}, Chromodynium's block and frame in
     * {@code textures/block}; the shapes the sets lack come from GT's shiny set.
     */
    private static final MaterialIconSet STRANGE_MATTER = new MaterialIconSet("strange_matter", MaterialIconSet.SHINY);
    private static final MaterialIconSet CHROMODYNIUM = new MaterialIconSet("chromodynium", MaterialIconSet.SHINY);
    private static final MaterialIconSet IDONTKNOWIUM = new MaterialIconSet("idontknowium", MaterialIconSet.SHINY);
    private static final MaterialIconSet ANTI_MATTER = new MaterialIconSet("anti_matter", MaterialIconSet.SHINY);
    private static final MaterialIconSet SUPERSTATE_STAR_MATTER = new MaterialIconSet("superstate_star_matter",
            MaterialIconSet.SHINY);

    private static final List<Material> ALL = new ArrayList<>();

    private AF9Materials() {}

    /** Every material registered here, in the order of registration. */
    public static List<Material> all() {
        return Collections.unmodifiableList(ALL);
    }

    /**
     * GregTech's material phase (mod bus): its own materials exist and the registries are still open. The order is
     * the one the materials had as scripts; GT's creative tabs and recipe viewers list them in it.
     */
    public static void register(MaterialEvent event) {
        aromatics();
        asteroidFission();
        biofuels();
        bouleMelting();
        cryogenics();
        diesel();
        electronicsMetallurgy();
        fabChemistry();
        fusionPlasmas();
        lithoProcess();
        moonSilicon();
        oil();
        planetMetals();
        particleAccelerator();
        antimatter();
        photolithography();
        platinumGroup();
        quantanium();
        solders();
        uhvSuperconductor();
    }

    private static Material.Builder material(String id) {
        return new Material.Builder(GTCEu.id(id));
    }

    private static Material add(Material.Builder builder) {
        Material material = builder.buildAndRegister();
        ALL.add(material);
        return material;
    }

    /** A plain dust: a form, a colour and a formula. */
    private static Material dust(String id, int color, String formula) {
        return add(material(id).dust().color(color).formula(formula));
    }

    /** A dust with the rough icon set (residues, salts, filter cakes). */
    private static Material roughDust(String id, int color, String formula) {
        return add(material(id).dust().iconSet(MaterialIconSet.ROUGH).color(color).formula(formula));
    }

    /** A metal without parts of its own: ingots with the metallic icon set. */
    private static Material metal(String id, int color, String formula) {
        return add(material(id).ingot().iconSet(MaterialIconSet.METALLIC).color(color).formula(formula));
    }

    private static Material liquid(String id, int color, String formula) {
        return add(material(id).liquid().color(color).formula(formula));
    }

    /** A liquid that is a mixture and has no formula. */
    private static Material liquid(String id, int color) {
        return add(material(id).liquid().color(color));
    }

    private static Material gas(String id, int color, String formula) {
        return add(material(id).gas().color(color).formula(formula));
    }

    /**
     * Fusion power: the plasmas of GTNH's fusion reactions that GT has no plasma for. GT gives eight elements a plasma
     * (helium, nitrogen, oxygen, argon, iron, tin, nickel, americium); GTNH's reactor also fuses sulfur, calcium, zinc,
     * niobium, silver, bismuth, radon, lead, thorium and plutonium-241 plasma, which the plasma turbines burn.
     * Recipes and fuel values: fusion_power.js. Spec: docs/fusion-power.md
     */
    private static void fusionPlasmas() {
        for (Material element : new Material[] { GTMaterials.Sulfur, GTMaterials.Calcium, GTMaterials.Zinc,
                GTMaterials.Niobium, GTMaterials.Silver, GTMaterials.Bismuth, GTMaterials.Radon, GTMaterials.Lead,
                GTMaterials.Thorium, GTMaterials.Plutonium241 }) {
            FluidProperty fluid = element.getProperty(PropertyKey.FLUID);
            if (fluid == null) {
                // sulfur and calcium have no fluid of their own: their plasma is their only one, like the quark
                // matters'. A property is verified the moment it is set, so it is made with its plasma queued
                element.setProperty(PropertyKey.FLUID,
                        new FluidProperty(FluidStorageKeys.PLASMA, new FluidBuilder().state(FluidState.PLASMA)));
            } else if (fluid.get(FluidStorageKeys.PLASMA) == null &&
                    fluid.getQueuedBuilder(FluidStorageKeys.PLASMA) == null) {
                fluid.enqueueRegistration(FluidStorageKeys.PLASMA, new FluidBuilder().state(FluidState.PLASMA));
            }
        }
    }

    /**
     * Aromatics support: dichloromethane, the middle step of the GTNH-style methane chlorination chain (methane,
     * chloromethane, dichloromethane, chloroform). GT has chloromethane and chloroform but nothing between them, and
     * the chain ends at chloroform on purpose (no carbon tetrachloride). Recipes: aromatics.js. Spec:
     * docs/green-chemistry.md
     */
    private static void aromatics() {
        // Dichloromethane: colourless, sweet-smelling, bp 40 C; the paint-stripper step of the chain
        add(material("dichloromethane")
                .liquid()
                .color(0xd8e4e8)
                .formula("CH2Cl2"));
    }

    /**
     * Asteroid fission, the new way to uranium and plutonium: brannerite, a uranium-titanium ore, generates only in
     * the Asteroid Field. The ore becomes yellowcake, the yellowcake the FX-1 Reactor's fuel rods, and the reactor
     * turns the rods, water and a sodium-potassium coolant into supercritical steam and spent rods, which are
     * reprocessed into plutonium. Recipes: asteroid_fission.js, rockets.js. Spec: docs/asteroid-fission.md
     */
    private static void asteroidFission() {
        // ---- The ore and the uranium chain ----
        // Formulas only (no components), so GT adds no electrolyzer or centrifuge shortcut past the chain.
        // Brannerite: uranium, titanium and rare earths in one oxide. Crushing gives two crushed ores per ore. Its
        // titanium stays in the residue (no rutile by-product): titanium comes from Mars sand only (planet_metals.js).
        add(material("brannerite")
                .dust().ore(2, 1)
                .color(0x4a4636).secondaryColor(0xd2c24a)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .formula("(U,Ca,Ce)(Ti,Fe)2O6")
                .radioactiveHazard(0.6F)
                .addOreByproducts(GTMaterials.Thorium, GTMaterials.Neodymium, GTMaterials.Iron));

        // The acid leach of the ore: uranyl sulfate in solution, and the ammonia precipitates it as yellowcake
        add(material("uranyl_sulfate_solution")
                .liquid()
                .color(0xc9d63c)
                .formula("UO2SO4"));
        add(material("yellowcake")
                .dust()
                .color(0xe8c51c)
                .iconSet(MaterialIconSet.ROUGH)
                .formula("U3O8")
                .radioactiveHazard(0.8F));

        // ---- Spent fuel ----
        // What the reactor leaves in a rod, dissolved in nitric acid; the centrifuge splits plutonium and uranium off.
        add(material("irradiated_fuel")
                .dust()
                .color(0x4b5a22).secondaryColor(0x8cff3c)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .formula("(U,Pu,FP)O2")
                .radioactiveHazard(1.5F));
        add(material("spent_fuel_solution")
                .liquid()
                .color(0x7a9a2e)
                .formula("(U,Pu)(NO3)x"));

        // ---- The reactor's coolant circuit ----
        // The coolant is GT's own sodium-potassium alloy (NaK, liquid at room temperature). It carries the heat out of
        // the core and gives it to the water in the same machine; the hot alloy goes back to NaK in a Vacuum Freezer.
        add(material("hot_sodium_potassium")
                .liquid(800)
                .color(0xff8a3d)
                .formula("NaK*"));
        // Water above the critical point (647 K, 22 MPa): no boiling, so no drying out. 80 EU per mB in a steam turbine
        // (steam: 0.5), 320 FE in Extreme Reactors' (af9-core ExtremeReactorsCompat).
        add(material("supercritical_steam")
                .gas(647)
                .color(0xe6f4ff)
                .formula("H2O*"));

        // ---- The propellant of the rockets (server_scripts/mods/gtceu/rockets.js) ----
        // Triethylaluminium, the hypergolic igniter of real rockets (the Merlin's TEA-TEB): aluminium, ethylene and
        // hydrogen in a chemical reactor, MV
        add(material("triethylaluminium")
                .liquid()
                .color(0xd7dbf2)
                .formula("Al(C2H5)3"));
        // Hydrogen and oxygen with aluminium powder burning in them and the igniter: more thrust per mB than any of its
        // parts. The only fuel Ad Astra's rockets take in this pack.
        add(material("aluminised_hydrolox")
                .liquid()
                .color(0xf2c96a)
                .formula("H2/O2/Al"));
    }

    /**
     * Biofuels: bioethanol, the farm-grown spirit behind fuel ethanol and oil-free ethylene. GT already has biodiesel
     * ({@code gtceu:bio_diesel}: seed oil + methanol, and its own fuel value), ethanol, methanol, seed oil, glycerol,
     * biomass and fermented biomass, so only the missing spirit is registered here. Recipes: biofuels.js. Spec:
     * docs/green-chemistry.md
     */
    private static void biofuels() {
        // Bioethanol: the distillation azeotrope (~95 %), dried to fuel ethanol over AF9's molecular sieves
        add(material("bioethanol")
                .liquid()
                .color(0xe4efe4)
                .formula("C2H5OH(H2O)"));
    }

    /**
     * Boule Melting: Endion, a heavy noble gas found only in the End's air (the centrifuge and the distillation tower
     * separate it from Ender Air), and Endionite, the wire of the Endion coils ({@link AF9Blocks}). Recipes:
     * boule_melting.js. Spec: docs/semiconductor-factory.md
     */
    private static void bouleMelting() {
        add(material("endion")
                .gas()
                .color(0x9b6bff)
                .formula("Ed"));

        // Tungstensteel and naquadah soaked in endion: the wire of the Endion coils
        add(material("endionite")
                .ingot()
                .fluid()
                .color(0x5e2a9e).secondaryColor(0x1a0b33)
                .iconSet(MaterialIconSet.SHINY)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_FOIL, MaterialFlags.GENERATE_FINE_WIRE,
                        MaterialFlags.GENERATE_ROD, MaterialFlags.GENERATE_FRAME)
                .blastTemp(5400, GasTier.HIGHEST, GTValues.VA[GTValues.IV], 1200)
                .formula("(W2Fe2Nq)Ed"));
    }

    /**
     * Cryogenics: what the Supercooling Cryostat makes of a gas. Dense cooling turns it into a dense liquid (compressed
     * and chilled, the intermediate), supercooling turns that into the supercooled fluid, the only kind a Coolant
     * Hatch takes. GT fluids cannot be below 0 K, so the supercooled fluids sit at 1 K; the -5000 K is the cryostat's
     * rating. Recipes: cryogenics.js. Spec: docs/semiconductor-factory.md
     */
    private static void cryogenics() {
        // gas, dense colour, supercooled colour, formula, dense liquid temperature (K)
        cryogen("hydrogen", 0x7fb2e6, 0xc8e6ff, "H2", 14);
        cryogen("argon", 0x3fb8c9, 0x9fefff, "Ar", 84);
        cryogen("xenon", 0x6a4fc9, 0xb9a3ff, "Xe", 161);
        cryogen("endion", 0x4a22b0, 0x8f6bff, "Ed", 40);
    }

    private static void cryogen(String gas, int dense, int supercooled, String formula, int kelvin) {
        add(material("dense_" + gas).liquid(kelvin).color(dense).formula(formula));
        add(material("supercooled_" + gas).liquid(1).color(supercooled).formula(formula));
    }

    /**
     * Diesels: shiny diesel (shiny oil + refinery gas), chloromethane diesel (cetane-boosted with chloromethane) and
     * mana diesel (shiny diesel infused with Botania mana). GT has diesel, bio_diesel and cetane_boosted_diesel; these
     * three are AF9's own ladder above them. Recipes: diesel.js. Spec: docs/green-chemistry.md §6
     */
    private static void diesel() {
        // Shiny diesel: hydrotreated shiny oil, desulfurized with refinery gas (GTNH's HOG)
        add(material("shiny_diesel")
                .liquid()
                .color(0xe8b83a)
                .formula("C12H26(S)"));

        // Chloromethane diesel: diesel with a chlorinated cetane booster, hotter ignition
        add(material("chloromethane_diesel")
                .liquid()
                .color(0x9ad87a)
                .formula("C12H26(CH3Cl)"));

        // Mana diesel: shiny diesel holding a mana diamond's mana (the diamond survives, emptied)
        add(material("mana_diesel")
                .liquid()
                .color(0x5eead4)
                .formula("C12H26(Mana)"));
    }

    /**
     * Electronics metallurgy. Circuits are built from their own tier's metals; where the real part is an alloy of that
     * metal, AF9 adds it, melted in the EBF: Aluminium-Silicon bond wire and Kovar pins (MV), Platinum-Iridium fine
     * wire (EV). GT makes the parts (bolts, fine wires) and the EBF and vacuum freezer recipes from these properties.
     * Plus the Zircon heavy-mineral-sand ore, refined like the real thing: plasma dissociation, carbochlorination,
     * extractive distillation (ZrCl4 / HfCl4), Kroll process. HfCl4 is the high-k precursor of the 50 nm node.
     * Recipes: electronics_metallurgy.js. Spec: docs/semiconductor-factory.md
     */
    private static void electronicsMetallurgy() {
        // GT defines zirconium as a bare element with no items. Give it dust and ingots; the Kroll process makes the
        // dust (sponge), GT's EBF recipe melts it (2128 K = hot ingot, cooled in the vacuum freezer). It is the zircon
        // chain's main metal next to the hafnium tetrachloride; no circuit uses it (not a tier metal).
        Material zirconium = GTMaterials.Zirconium;
        if (!zirconium.hasProperty(PropertyKey.DUST)) {
            zirconium.setProperty(PropertyKey.DUST, new DustProperty());
            zirconium.setProperty(PropertyKey.INGOT, new IngotProperty());
            zirconium.setProperty(PropertyKey.BLAST, new BlastProperty(2128, GasTier.MID,
                    GTValues.VA[GTValues.HV], 800, GTValues.VA[GTValues.HV], 200));
        }

        // The ZPM and UV circuits bond with fine naquadah alloy wire (circuits_af9.js); GT's naquadah alloy has wires and
        // foil but no fine wire. GT makes it in the wiremill from this flag.
        if (!GTMaterials.NaquadahAlloy.hasFlag(MaterialFlags.GENERATE_FINE_WIRE)) {
            GTMaterials.NaquadahAlloy.addFlags(MaterialFlags.GENERATE_FINE_WIRE);
        }

        // ---- Circuit alloys (mixed in server_scripts, melted in the EBF) ----
        // MV: aluminium wedge-bonding wire (the silicon keeps it from work-softening). Aluminium is the MV metal.
        // EBF at MV voltage, which two LV hatches can supply.
        add(material("aluminium_silicon")
                .ingot()
                .color(0xc8ccd2).iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Aluminium, 16, GTMaterials.Silicon, 1)
                .flags(MaterialFlags.GENERATE_FINE_WIRE)
                .blastTemp(1700, GasTier.LOW, GTValues.VA[GTValues.MV], 400));

        // MV: expands like glass, so it seals into IC packages; used for the pins
        add(material("kovar")
                .ingot()
                .color(0x8e9ba6).iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Iron, 6, GTMaterials.Nickel, 3, GTMaterials.Cobalt, 2)
                .flags(MaterialFlags.GENERATE_BOLT_SCREW)
                .blastTemp(1720, GasTier.LOW, GTValues.VA[GTValues.MV], 600));

        // EV: hard, inert platinum wire (probe tips, electrodes). Platinum is the EV metal; iridium comes from the EV-
        // era platinum group chain, so the EV bootstrap circuit uses plain platinum wire instead.
        add(material("platinum_iridium")
                .ingot()
                .color(0xe6e6dc).iconSet(MaterialIconSet.SHINY)
                .components(GTMaterials.Platinum, 9, GTMaterials.Iridium, 1)
                .flags(MaterialFlags.GENERATE_FINE_WIRE)
                .blastTemp(2100, GasTier.MID, GTValues.VA[GTValues.EV], 600));

        // ---- Zircon ----
        // Heavy mineral sand with ilmenite, rutile and monazite. Formulas only (no components), so GT adds no
        // electrolyzer shortcut past the refining chain.
        add(material("zircon")
                .dust().ore()
                .color(0xb77f4f).iconSet(MaterialIconSet.ROUGH)
                .formula("ZrSiO4")
                .addOreByproducts(GTMaterials.Ilmenite, GTMaterials.Rutile, GTMaterials.Monazite));

        add(material("zirconia")
                .dust()
                .color(0xebe4d4)
                .formula("ZrO2"));

        // Zirconium tetrachloride still carrying the hafnium (the two are chemically almost identical)
        add(material("crude_zirconium_tetrachloride")
                .gas()
                .color(0xd9d4c4)
                .formula("(Zr,Hf)Cl4"));

        add(material("zirconium_tetrachloride")
                .gas()
                .color(0xe6e2d6)
                .formula("ZrCl4"));

        add(material("hafnium_tetrachloride")
                .gas()
                .color(0xc8d0c8)
                .formula("HfCl4"));
    }

    /**
     * Fab chemistry: the long, real-world supply chains behind the Photolithography Line. Every material is a real
     * compound or a real process stream. Recipes: fab_chemistry.js. Spec: docs/semiconductor-factory.md §6.11-6.15
     * <pre>
     *   1. Electronic-grade silicon (MV)  quartz, MG-Si, trichlorosilane, Siemens polysilicon, CZ boules
     *   2. Fluorochemicals (MV/HV)        fluorspar, HF, KF.2HF electrolysis, F2; Simons ECF, triflic acid
     *   3. Air gases (MV/HV)              cold box: argon, neon, krypton, xenon; KrF / ArF laser premix
     *   4. KrF resist (HV)                t-BOC polyhydroxystyrene + photoacid generator + amine quencher in PGMEA
     *   5. ArF resist (EV)                methacrylate terpolymer from the acetone cyanohydrin route
     * </pre>
     */
    private static void fabChemistry() {
        // ---- 1. Electronic-grade silicon ----
        dust("high_purity_quartz", 0xf4f2ee, "SiO2");  // acid-leached quartzite
        dust("metallurgical_grade_silicon", 0x7d8087, "Si");  // 98-99 %, from the arc furnace
        liquid("crude_chlorosilanes", 0xc9ccb8, "(SiHCl3)(SiCl4)(SiH2Cl2)");  // hydrochlorination product
        liquid("trichlorosilane", 0xdadfe0, "SiHCl3");  // TCS, bp 32 C
        liquid("silicon_tetrachloride", 0xd2d8d6, "SiCl4");  // STC, bp 58 C
        gas("dichlorosilane", 0xe2e7e4, "SiH2Cl2");  // DCS, bp 8 C
        gas("boron_trichloride", 0xd9e3d0, "BCl3");  // the boron impurity, bp 13 C
        metal("polysilicon", 0x8a93a3, "Si");  // Siemens rods, 11N
        liquid("silicon_etchant", 0xe8e0a8, "(HNO3)(HF)(CH3COOH)");  // mixed-acid chunk etch
        dust("electronic_grade_silicon", 0x9aa6b8, "Si");  // etched poly chunks, CZ charge

        // ---- 2. Fluorochemicals ----
        liquid("crude_hydrogen_fluoride", 0xd6dccf, "(HF)(H2SO4)(H2O)");  // kiln gas, condensed
        dust("potassium_fluoride", 0xefefe8, "KF");
        liquid("potassium_bifluoride_electrolyte", 0xe0e6d8, "KF(HF)2");  // molten KF.2HF, 90 C
        gas("crude_fluorine", 0xd9e38a, "(F2)(HF)");  // cell gas, ~10 % HF
        dust("sodium_fluoride", 0xf2f2ec, "NaF");
        dust("sodium_bifluoride", 0xe6ece0, "NaHF2");  // spent HF trap
        liquid("methanesulfonic_acid", 0xe0e2d6, "CH3SO3H");
        liquid("sulfur_dichloride", 0xb33a2a, "SCl2");  // cherry-red liquid
        liquid("thionyl_chloride", 0xe8d890, "SOCl2");
        liquid("methanesulfonyl_chloride", 0xe4dcc0, "CH3SO2Cl");
        liquid("methanesulfonyl_fluoride", 0xe2e4d0, "CH3SO2F");
        liquid("simons_cell_electrolyte", 0xd8e0cc, "(CH3SO2F)(HF)3");  // substrate in anhydrous HF
        gas("trifluoromethanesulfonyl_fluoride", 0xdde6de, "CF3SO2F");
        dust("potassium_triflate", 0xf0f0ea, "CF3SO3K");

        // ---- 3. Air gases ----
        gas("crude_argon", 0xb8d8f0, "(Ar)(O2)(N2)");  // side draw, ~95 % Ar
        gas("crude_neon", 0xf0b8a8, "(Ne)(He)(N2)(H2)");  // non-condensables of the condenser
        gas("neon_helium_mixture", 0xf4c8b8, "(Ne)(He)");
        gas("krypton_xenon_concentrate", 0xc8d0f0, "(O2)(Kr)(Xe)(CH4)");  // from the oxygen sump
        gas("crude_krypton_xenon", 0xbcc4ec, "(O2)(Kr)(Xe)(CO2)(N2O)");  // hydrocarbons burnt out
        gas("purified_krypton_xenon", 0xb0b8e8, "(O2)(Kr)(Xe)");

        // ---- 4. KrF resist ----
        dust("hydroxyacetophenone", 0xece4cc, "HOC6H4COCH3");  // 4-HAP
        dust("acetoxyacetophenone", 0xe8e0c8, "CH3COOC6H4COCH3");  // 4-AAP
        dust("palladium_chloride", 0x8a5a3a, "PdCl2");
        dust("palladium_on_carbon", 0x2a2a2e, "(Pd)(C)");  // 5 % Pd/C hydrogenation catalyst
        liquid("acetoxyphenyl_methyl_carbinol", 0xe6e0cc, "CH3COOC6H4CH(OH)CH3");
        liquid("acetoxystyrene", 0xe8e4d0, "CH3COOC6H4CH=CH2");  // 4-ASM monomer
        liquid("hydrazine", 0xe4ecf2, "N2H4");  // Olin-Raschig process
        liquid("acetone_cyanohydrin", 0xe0d8b8, "(CH3)2C(OH)CN");
        dust("hydrazobisisobutyronitrile", 0xf0ecdf, "C8H14N4");
        dust("azobisisobutyronitrile", 0xf6f4ee, "C8H12N4");  // AIBN radical initiator
        dust("poly_acetoxystyrene", 0xeee8d6, "(C10H10O2)n");
        gas("isobutylene", 0xe8ecd8, "(CH3)2C=CH2");
        dust("acidic_ion_exchange_resin", 0x9a5a2a, "(C8H7SO3H)n");  // sulfonated polystyrene beads
        liquid("tert_butanol", 0xecf0ea, "(CH3)3COH");
        dust("sodium_tert_butoxide", 0xf2eee4, "(CH3)3CONa");
        gas("phosgene", 0xe8ecc8, "COCl2");
        liquid("di_tert_butyl_dicarbonate", 0xf0ece0, "((CH3)3COCO)2O");  // Boc anhydride
        dust("tboc_polyhydroxystyrene", 0xe6dcc0, "(C8H8O)n(C5H8O2)m");  // the KrF resin, ~30 % protected
        dust("aluminium_chloride", 0xf0ecd8, "AlCl3");
        dust("diphenyl_sulfoxide", 0xf2f0e8, "(C6H5)2SO");
        dust("vanadyl_pyrophosphate", 0x3c5a6a, "(VO)2P2O7");  // VPO butane oxidation catalyst
        dust("maleic_anhydride", 0xf4f2ea, "C4H2O3");
        liquid("tetrahydrofuran", 0xe6eef0, "C4H8O");
        liquid("phenylmagnesium_chloride", 0x8c7a5a, "C6H5MgCl(C4H8O)");  // Grignard reagent in THF
        dust("triphenylsulfonium_chloride", 0xf0eee6, "(C6H5)3SCl");
        liquid("butanol", 0xe8eee4, "C4H9OH");
        liquid("tributylamine", 0xe4e8d4, "(C4H9)3N");  // base quencher
        liquid("tetraethyl_orthosilicate", 0xe8ecf0, "Si(OC2H5)4");  // TEOS
        dust("titanium_silicalite", 0xeeeef4, "(TiO2)(SiO2)50");  // TS-1 epoxidation catalyst
        liquid("propylene_oxide", 0xe6ecee, "C3H6O");
        liquid("unfiltered_krf_photoresist", 0xc8a458);

        // ---- 5. ArF resist ----
        liquid("methacrylamide_sulfate", 0xcfc6a0, "CH2=C(CH3)CONH2(H2SO4)");
        liquid("methacrylic_acid", 0xe6eae0, "CH2=C(CH3)COOH");
        liquid("tert_butyl_methacrylate", 0xe8ece4, "CH2=C(CH3)COOC(CH3)3");  // the acid-labile monomer
        dust("ammonium_bisulfate", 0xf2f2ea, "NH4HSO4");
        liquid("unfiltered_arf_photoresist", 0xd8cc98);

        // ---- MUV chemistry support ----
        dust("molybdenum_trioxide", 0xe0e4a0, "MoO3");
        dust("iron_molybdate", 0xa89a4a, "Fe2(MoO4)3");  // Formox formaldehyde catalyst
        liquid("tetramethylammonium_chloride_solution", 0xd6ecf2, "(CH3)4NCl(H2O)");

        // ---- Materials the Photolithography Line consumes directly ----
        // Excimer laser premixes: about 1 % rare gas and 0.1 % fluorine in a neon buffer. The discharge slowly uses up
        // the fluorine, so the gas is topped up while the laser runs.
        add(material("krf_excimer_gas").gas().color(0xd6c8f2).formula("(Ne)(Kr)(F2)"));
        add(material("arf_excimer_gas").gas().color(0xc6d8f2).formula("(Ne)(Ar)(F2)"));

        // Chemically amplified resists. DNQ-novolac stops working below ~300 nm (novolac turns opaque, DNQ barely
        // bleaches); here light frees an acid from the photoacid generator (PAG) and in the post-exposure bake each
        // acid unblocks hundreds of polymer groups. The amine quencher stops the acid from wandering into dark areas.
        add(material("trifluoromethanesulfonic_acid").liquid().color(0xe9edf0).formula("CF3SO3H"));
        add(material("triphenylsulfonium_triflate").dust().color(0xf2f0ea).formula("(C6H5)3S(CF3SO3)"));
        // KrF polymer backbone: transparent at 248 nm; its phenol groups carry the t-BOC protection
        add(material("polyhydroxystyrene").dust().color(0xefe6d2).formula("(C8H8O)n"));
        // ArF: aromatic rings absorb 193 nm, so ArF resists are built on methacrylates instead
        add(material("methyl_methacrylate").liquid().color(0xe4eef2).formula("C5H8O2"));
        // ArF resin: MMA / tert-butyl methacrylate / methacrylic acid terpolymer (IBM's first 193 nm resist platform)
        add(material("methacrylate_resin").dust().color(0xdfe8ea).formula("(C5H8O2)n(C8H14O2)m(C4H6O2)k"));
        add(material("propylene_glycol_methyl_ether").liquid().color(0xe6f0ea).formula("C4H10O2"));
        add(material("propylene_glycol_methyl_ether_acetate").liquid().color(0xdcebe6).formula("C6H12O3"));
        add(material("krf_photoresist").liquid().color(0xd9b45c));
        add(material("arf_photoresist").liquid().color(0xe6dcaa));

        // Immersion film for LUV: 18 MOhm cm, degassed. Water (n = 1.44 at 193 nm) lets the lens reach NA 1.35.
        add(material("ultrapure_water").liquid().color(0x8cc4ff).formula("H2O"));
    }

    /**
     * The lithography process around the print: the chemistry of the wafer clean-up and of the finer coatings, the etch
     * plasma, and the functional layers of the new chip families. Recipes: litho_process.js. Spec:
     * docs/semiconductor-factory.md §18
     */
    private static void lithoProcess() {
        // GT defines germanium, selenium and tellurium as bare elements with no items. The functional layers below take
        // them as dusts (tungsten diselenide, the Ge2Sb2Te5 alloy, the CdSe quantum dots): give them one. Without it
        // those recipes name an item that does not exist, which GT 7.5 refuses (and with it every recipe after them).
        for (Material element : new Material[] { GTMaterials.Germanium, GTMaterials.Selenium, GTMaterials.Tellurium }) {
            if (!element.hasProperty(PropertyKey.DUST)) element.setProperty(PropertyKey.DUST, new DustProperty());
        }

        // RCA clean, the wafer cleaning every fab starts with: SC-1 (ammonia, peroxide, water, 1:1:5) lifts particles
        // and organics, SC-2 (HCl, peroxide, water, 1:1:6) takes the metal ions off. Piranha (SPM, 3:1 sulfuric acid
        // to peroxide) strips baked resist and heavy organics.
        liquid("sc1_solution", 0xcfe8f5, "(NH3)(H2O2)(H2O)5");
        liquid("sc2_solution", 0xe6f0d2, "(HCl)(H2O2)(H2O)6");
        liquid("piranha_solution", 0xf2e2b8, "(H2SO4)3(H2O2)");
        liquid("spent_piranha", 0x6b5a3c, "(H2SO4)(H2O)(C)");

        // Ethyl lactate, the green solvent of the coatings: acetaldehyde from ethanol over copper, lactonitrile with
        // hydrogen cyanide, hydrolysed to lactic acid, esterified with ethanol.
        liquid("acetaldehyde", 0xeef3e0, "CH3CHO");
        liquid("lactonitrile", 0xe8eadc, "CH3CH(OH)CN");
        liquid("lactic_acid", 0xf0ecd8, "CH3CH(OH)COOH");
        liquid("ethyl_lactate", 0xe9efe2, "CH3CH(OH)COOC2H5");

        // BARC, the bottom anti-reflective coat under the resist (KrF and ArF): an acrylic polymer with a dye that
        // soaks up the light that passed the resist, in ethyl lactate. The dye is a nitrated naphthalene.
        dust("nitronaphthalene", 0xd9b24a, "C10H7NO2");
        liquid("barc", 0xc9a447, "(C5H8O2)n(C10H7NO2)(C5H10O3)");

        // TARC, the top anti-reflective coat over the resist of the immersion nodes (65 and 50 nm): a fluoropolymer in
        // PGMEA. What a coating leaves in the bowl is spent resist solvent; it is distilled back, never all of it.
        liquid("tarc", 0xbfe3e8, "(C2F4)n(C6H12O3)");
        liquid("spent_resist_solvent", 0x8a7f4a, "(C6H12O3)(C)");
        // Plasma etching: carbon tetrafluoride, chlorine, argon and oxygen make the etch plasma of the print
        gas("tetrafluoromethane", 0xdfe8ef, "CF4");
        gas("etch_plasma_gas", 0xb48cf0, "(CF4)(Cl2)(Ar)4(O2)");

        // The functional layers of the new chip families ({@code AF9Items}): the cards that use the chips take them
        // too. Acoustic wave: the piezo films of SAW and BAW filters
        dust("aluminium_nitride", 0xb8c4d0, "AlN");
        dust("lithium_niobate", 0xdcd8e8, "LiNbO3");
        // Photonics: the silicon nitride waveguide (germanium and indium phosphide come from GT)
        dust("silicon_nitride", 0x9aa0b4, "Si3N4");
        // Spintronics: the free layer of the magnetic tunnel junction (the MgO barrier is GT's magnesia)
        dust("cobalt_iron_boron", 0x6c7a96, "(Co)(Fe)(B)");
        // 2D materials: tungsten diselenide channels, hexagonal boron nitride dielectric (MoS2 is GT's molybdenite)
        dust("tungsten_diselenide", 0x4a5260, "WSe2");
        dust("boron_nitride", 0xf0f0f4, "BN");
        // Neuromorphic: the phase-change memory material
        dust("gst_alloy", 0x8a7a96, "Ge2Sb2Te5");
        // Quantum dots: CdSe nanocrystals in solution
        liquid("quantum_dot_colloid", 0xe0503c, "(CdSe)n(C8H10)");
    }

    /**
     * The Moon's silicon: Artemite, the lunar borate that dopes the first silicon boules, and the Moon Sand chain
     * behind every polysilicon. Artemite generates only on the Moon (vein_moon.js); Moon Sand is blended from sand
     * silica and L-01, the light cut of Lunar Air, the Moon's own air (a bedrock fluid drilled there, or a far-dark
     * elevator mission). Recipes: fab_chemistry.js (sand silica, moon sand, the polysilicon bath, lunar air
     * distillation), boule_melting.js (the dopant). Spec: docs/semiconductor-factory.md
     */
    private static void moonSilicon() {
        // Artemite: a calcium-sodium borate of the lunar highlands, named for Artemis. The p-type dopant of the
        // silicon melt charges and seed crystals (a tiny pile per charge, like boron was). Formulas only (no
        // components), so GT adds no electrolyzer or centrifuge shortcut past the ore chain. Crushing gives two
        // crushed ores per ore.
        add(material("artemite")
                .dust().ore(2, 1)
                .color(0xcfd8ec).secondaryColor(0x7a86c8)
                .iconSet(MaterialIconSet.SHINY)
                .formula("(Ca,Na)2B4O7")
                .addOreByproducts(GTMaterials.Aluminium, GTMaterials.Calcium, GTMaterials.Silicon));

        // Moon Sand: regolith silica activated with L-01. The only polysilicon there is: a chemical bath under
        // oxygen turns it into polysilicon dust (fab_chemistry.js numbers: a sand block is 8 polysilicon).
        add(material("moon_sand")
                .dust()
                .color(0xb0aca4)
                .iconSet(MaterialIconSet.ROUGH)
                .formula("(SiO2)(L01)"));

        // Lunar Air: the Moon's thin exosphere, bottled by the fluid drilling rig (vein_oil.js). Distilled like the
        // other airs: argon and radon off the sides, L-01 as the light product.
        add(material("lunar_air")
                .gas()
                .color(0xd6e8f5)
                .formula("(Ar)(He)(L01)"));

        // L-01: Lunar Air's light cut. A chemical reactor binds it into sand silica, which is what makes Moon Sand.
        add(material("l_01")
                .gas()
                .color(0x7df0d0)
                .formula("L-01"));
    }

    /**
     * The planet metals: Desh, the Moon's orange structural metal (Ad Astra's Moon Desh Ore; the tier 2 rockets are
     * built from it). A plain metal with plates, rods, bolts and screws (GT makes the parts and their recipes);
     * formulas only (no components), so GT adds no electrolyzer or centrifuge shortcut past the ore. It smelts like
     * Ad Astra smelts it (no blast furnace); the rocket hulls stay Ad Astra's blocks (ad_astra:desh_block). Recipes:
     * planet_metals.js. Spec: docs/asteroid-fission.md
     */
    private static void planetMetals() {
        add(material("desh")
                .ingot()
                .color(0xc47a3a).iconSet(MaterialIconSet.METALLIC)
                .formula("Ds")
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_BOLT_SCREW));
    }

    /**
     * The new oil: the fluids of the chain Oil Regolith, Impure Oil, Shiny Oil, Oil and Heavy Oil, and Heavy Water.
     * The regolith block is {@link com.af9.core.space.OilRegolithBlock}, grown in the Asteroid Field's rock. Recipes:
     * oil.js, vein_oil.js. Spec: docs/oil.md
     */
    private static void oil() {
        // Impure Oil: black and brown, thick. Its own animated texture (kubejs/assets/gtceu/textures/block/fluids/
        // fluid.impure_oil.png), no tint on top of it.
        add(material("impure_oil")
                .liquid(new FluidBuilder().customStill().disableColor())
                .color(0x2a1a0e));

        // Shiny Oil: what the wash leaves, amber with glints running over it (fluid.shiny_oil.png, animated)
        add(material("shiny_oil")
                .liquid(new FluidBuilder().customStill().disableColor())
                .color(0xb87a1a));

        // Heavy Water: D2O, from the Asteroid Field's deposits (GT has deuterium, no heavy water)
        add(material("heavy_water")
                .liquid()
                .color(0x8fd0ff)
                .formula("D2O"));
    }

    /**
     * The Particle Accelerator's quark matter: heavy-ion collisions give a quark-gluon plasma, quark synthesis
     * condenses it into strange matter, and with strange matter into chromodynium, the quark-level metal of the 1 nm
     * wafers. Recipes: particle_accelerator.js. Spec: docs/semiconductor-factory.md
     */
    private static void particleAccelerator() {
        // stable strangelets: up, down and strange quarks in one bag
        // (the plasma of strange matter and of chromodynium is what the Plasma Forge forges: dtpf.js)
        add(material("strange_matter")
                .dust()
                .plasma()
                .color(0x8a1e6a).secondaryColor(0x2a0033)
                .iconSet(STRANGE_MATTER)
                .formula("(uds)n"));

        // colour-charged quark matter held in a lattice: the metal of the 1 nm wafers
        add(material("chromodynium")
                .ingot()
                .fluid()
                .plasma()
                .color(0xff3c78).secondaryColor(0x3cffb4)
                .iconSet(CHROMODYNIUM)
                // the fine wire: the traces and bond wires of the Pico circuits
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_FOIL, MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_FRAME, MaterialFlags.GENERATE_FINE_WIRE)
                .blastTemp(12000, GasTier.HIGHEST, GTValues.VA[GTValues.UHV], 2400)
                .formula("Qc"));
    }

    /**
     * Antimatter and the superstate: past chromodynium. The Particle Accelerator condenses quark-gluon plasma with
     * chromodynium into anti-quarks, then with anti-quarks into anti-matter, then with anti-matter into superstate
     * star matter, the next quark-level state (recipes: particle_accelerator.js). The Plasma Forge ionises
     * superstate dust into Superstate Star Matter Plasma and forges it back into plates (recipes: dtpf.js); Anti
     * Matter Plasma is fused from strange matter and chromodynium plasma in the Mega Fusion Reactor
     * (fusion_reactor.js).
     */
    private static void antimatter() {
        // the anti-quark: the quark of anti-matter (dust only, like a quark flavour)
        add(material("anti_quark")
                .dust()
                .color(0x00e5ff).secondaryColor(0xff00e5)
                .iconSet(ANTI_MATTER)
                .formula("Aq"));

        // stabilised anti-quarks in one bag, past strange matter
        // (its dust is the superstate's feedstock; its plasma is what the Mega Fusion Reactor fuses: fusion_reactor.js)
        add(material("anti_matter")
                .dust()
                .plasma()
                .color(0x7a00ff).secondaryColor(0x00fff2)
                .iconSet(ANTI_MATTER)
                .formula("(Aq)n"));

        // the superstate: star matter held past plasma, the metal past chromodynium
        add(material("superstate_star_matter")
                .ingot()
                .fluid()
                .plasma()
                .color(0xfff200).secondaryColor(0xff6a00)
                .iconSet(SUPERSTATE_STAR_MATTER)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_FOIL, MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_FRAME, MaterialFlags.GENERATE_FINE_WIRE)
                .blastTemp(15000, GasTier.HIGHEST, GTValues.VA[GTValues.UHV], 3200)
                .formula("Ss"));

        // Idontknowium: nobody knows what it is. Only its plasma exists, 12,000 K, forged in the Plasma Forge from star
        // matter plasma and the anti-matter plasma the Mega Fusion Reactor fuses (dtpf.js, fusion_reactor.js)
        add(material("idontknowium")
                .dust()
                // its own animated texture (fluid.idontknowium_plasma.png), no tint on top of it; tools/textures/idontknowium.py
                .plasma(new FluidBuilder().state(FluidState.PLASMA).temperature(12000).customStill().disableColor())
                .color(0x28aaff).secondaryColor(0xff3cd2)
                .iconSet(IDONTKNOWIUM)
                .formula("Idk"));
    }

    /**
     * Photolithography: the extreme clean dry air of the exposure tools, the HMDS adhesion promoter, the DNQ-novolac
     * resist and its developer (350 nm), and the metal-oxide EUV resist (20 and 7 nm). The KrF and ArF resists are in
     * {@link #fabChemistry}. Recipes: photolithography.js. Spec: docs/semiconductor-factory.md
     */
    private static void photolithography() {
        // ---- Extreme clean dry air (XCDA) chain, as in a fab's clean-dry-air plant ----
        // air -> catalytic oxidation (CO, H2, hydrocarbons -> CO2 + H2O) -> CO2 scrubbing -> molecular sieve drying
        // -> cryogenic cooling (freezes out the last traces) -> re-warmed through a membrane filter = XCDA
        add(material("oxidized_air")
                .gas()
                .color(0xd8e4ec));

        add(material("decarbonated_air")
                .gas()
                .color(0xdfeefa));

        add(material("dry_air")
                .gas()
                .color(0xe8f4ff));

        // Held just above its liquefaction point (95 K): cryogenic, like liquid air
        add(material("cryogenic_supercooled_air")
                .gas(95)
                .color(0x9fd8ff));

        // Purge gas for the exposure tool
        add(material("extreme_clean_dry_air")
                .gas()
                .color(0xe6f2ff));

        // Room-temperature CO oxidation catalyst (copper manganese spinel, as in gas-mask filters)
        add(material("hopcalite")
                .dust()
                .color(0x2e2a28)
                .formula("CuMn2O4"));

        // HMDS adhesion promoter chain: (CH3)2SiCl2 + CH3Cl + Mg -> (CH3)3SiCl + MgCl2
        add(material("trimethylchlorosilane")
                .liquid()
                .color(0xdde3e8)
                .formula("(CH3)3SiCl"));

        // 2 (CH3)3SiCl + 3 NH3 -> [(CH3)3Si]2NH + 2 NH4Cl
        add(material("hexamethyldisilazane")
                .liquid()
                .color(0xe3e8d8)
                .formula("((CH3)3Si)2NH"));

        // HMDS vapour carried in nitrogen, as fed to a vapour prime oven
        add(material("hmds_vapor")
                .gas()
                .color(0xc9d6e3)
                .formula("((CH3)3Si)2NH(N2)"));

        // Positive DNQ-novolac photoresist chain
        // C6H5OH + CH2O -> novolac repeat unit + H2O
        add(material("novolac_resin")
                .liquid()
                .color(0xb5651d)
                .formula("(C7H6O)n"));

        // Photoactive compound (simplified, balanced): C10H8 + HNO3 + NH3 -> C10H6N2O + 2 H2O + H2
        add(material("diazonaphthoquinone")
                .dust()
                .color(0xe8c547)
                .formula("C10H6N2O"));

        // Novolac + DNQ dissolved in xylene
        add(material("photoresist")
                .liquid()
                .color(0xb04a1f));

        // TMAH developer chain: (CH3)2NH + 2 CH3Cl -> (CH3)4NCl + HCl
        add(material("tetramethylammonium_chloride")
                .dust()
                .color(0xf5f5f0)
                .formula("(CH3)4NCl"));

        // Electrolysed out of the chloride (membrane cell), diluted to the industry standard 2.38 %
        add(material("tmah_developer")
                .liquid()
                .color(0xcfe8f0)
                .formula("(CH3)4NOH(H2O)"));

        // ---- Metal-oxide EUV resist (20 nm and 7 nm) ----
        // Sn + 2 Cl2 -> SnCl4, the tin source of the tin-oxo clusters
        add(material("tin_tetrachloride")
                .liquid()
                .color(0xe8e8d0)
                .formula("SnCl4"));

        // Tin-oxo carboxylate clusters (methacrylate ligands) in PGMEA: dense tin absorbs EUV far better than carbon
        add(material("euv_photoresist")
                .liquid()
                .color(0xd9c27a));
    }

    /**
     * The platinum group metals line: the materials of the refinery that turns purified Cu-Ni sulfide ore and
     * cooperite into platinum, palladium, gold, rhodium, ruthenium, iridium and osmium. Every material is a real
     * compound or process stream. GT's own platinum group sludge chain stays as it is; its sludge is one more feed of
     * this line. Recipes: platinum_group.js. Spec: docs/platinum-group-metals.md
     * <pre>
     *   1. Matte        purified ore, smelted with a silica flux in oxygen: PGM matte (a smelter's converter matte)
     *   2. Leach        matte + sulfuric acid + oxygen: the nickel and copper go into solution, the PGMs stay
     *   3. Dissolution  residue + HCl + chlorine: Pt, Pd and Au go into solution; Rh, Ir, Ru and Os stay as residue
     *   4. Gold         sulfur dioxide reduces gold out of the liquor
     *   5. Platinum     ammonium chloride precipitates yellow (NH4)2PtCl6, calcined to platinum sponge
     *   6. Palladium    what stays in the filtrate: ammonia, then hydrochloric acid give Pd(NH3)2Cl2; hydrogen
     *                   reduces it
     *   7. Ru and Os    alkaline oxidising fusion, water leach, chlorine drives off RuO4 and OsO4, distillation
     *                   splits them
     *   8. Ir and Rh    chlorination of the oxide left from the leach, (NH4)2IrCl6 precipitates, Rh goes on as the
     *                   nitrite
     * </pre>
     */
    private static void platinumGroup() {
        // ---- 1-3. Matte, leach residue, dissolution ----
        roughDust("pgm_matte", 0x8a7a52, "(Ni,Cu,Fe)xSy(PGM)");  // converter matte, slowly cooled and ground
        roughDust("pgm_leach_residue", 0x5c5a4a, "(Pt,Pd,Au,Rh,Ir,Ru,Os)");  // what the acid leach leaves: ~60 % PGM
        roughDust("pgm_insoluble_residue", 0x4a4a46, "(Rh,Ir,Ru,Os)");  // refractory metals the chlorine does not take
        liquid("platinum_group_chloride_liquor", 0xc0601a, "H2PtCl6(H2PdCl4)(HAuCl4)");
        liquid("gold_free_liquor", 0xb8641e, "H2PtCl6(H2PdCl4)");

        // ---- 5-6. Platinum and palladium ----
        roughDust("ammonium_hexachloroplatinate", 0xe8c020, "(NH4)2PtCl6");  // the yellow salt
        liquid("palladium_filtrate", 0xc27a2a, "H2PdCl4");
        liquid("palladium_tetraammine_solution", 0xcfd6e8, "[Pd(NH3)4]Cl2");
        roughDust("palladium_diammine_dichloride", 0xe6d24a, "Pd(NH3)2Cl2");  // Pd "yellow salt"

        // ---- 7. Ruthenium and osmium ----
        roughDust("pgm_fusion_cake", 0x3a5a46, "(Na2RuO4)(Na2OsO4)(Rh2O3)(IrO2)");
        liquid("ruthenate_osmate_liquor", 0x2a6a5a, "(Na2RuO4)(Na2OsO4)");
        gas("platinum_group_tetroxide_vapour", 0xd8d070, "(RuO4)(OsO4)");
        gas("ruthenium_tetroxide_vapour", 0xd8c040, "RuO4");  // bp 40 C
        gas("osmium_tetroxide_vapour", 0xe6e6c8, "OsO4");  // bp 130 C
        roughDust("ammonium_hexachlororuthenate", 0x7a2a2a, "(NH4)2RuCl6");
        roughDust("ammonium_hexachloroosmate", 0x8a3a2a, "(NH4)2OsCl6");

        // ---- 8. Iridium and rhodium ----
        roughDust("rhodium_iridium_oxide", 0x3a3a3e, "(Rh2O3)(IrO2)");
        liquid("rhodium_iridium_chloride_liquor", 0x8a2a3a, "(H2IrCl6)(H3RhCl6)");
        roughDust("ammonium_hexachloroiridate", 0x6a1a1a, "(NH4)2IrCl6");  // dark red
        liquid("rhodium_chloride_filtrate", 0xc03a50, "H3RhCl6");
        roughDust("sodium_hexanitritorhodate", 0xe8e0a0, "Na3Rh(NO2)6");
        liquid("rhodium_trichloride_solution", 0xd04a60, "RhCl3");
        roughDust("zinc_chloride", 0xf0f0f0, "ZnCl2");  // the cementation of rhodium leaves it
    }

    /**
     * Sanguinite: the bright-red UHV superconductor, smelted in the Sanguinite Hearth Furnace
     * ({@code gtceu:sanguinite_hearth_furnace}, a standalone Rotary-Hearth copy running only
     * {@code gtceu:sanguinite_hearth}: the EBF cannot smelt it, the auto EBF/hot-ingot recipes are removed).
     * Tin alloy, barium, europium, titanium, electrum and crude sanguinite dusts are smelted at
     * 6000 K into molten sanguinite (normal + helium-boosted prints, circuit 8, ZPM, 60 s), which the vacuum
     * freezer casts into ingots under supercooled hydrogen. The hearth only holds its heat on coolant
     * (Coolant Hatches) plus 4A LuV heating. Recipes: uhv_superconductor.js.
     * Spec: docs/uhv-superconductor.md
     */
    private static void uhvSuperconductor() {
        // Ares gas: the rust-red noble-gas wisp of the Martian asteroid field (the field's sky is Mars orbit),
        // drilled with the Fluid Drilling Rig (vein_oil.js). Formulas only, so GT adds no shortcuts past the chain.
        add(material("ares_gas")
                .gas()
                .color(0xc46a3d)
                .formula("Mrs"));

        // LXA-1: the far dark's light exotic, brought home by the Space Elevator's liquid missions
        // (PlanetCatalog, planet type 9, Mk-IV). A gas at room temperature, kept as GT's fluid.
        add(material("lxa_1")
                .gas()
                .color(0xbfefff)
                .formula("Lx"));

        // What the Large Chemical Reactor blends out of the dusts and the three gases: 4 neutronium, 10 tritanium.
        add(material("crude_sanguinite")
                .dust()
                .color(0x8a2a1a)
                .iconSet(MaterialIconSet.ROUGH)
                .formula("(Nt4Ke10MrsLx)"));

        // Sanguinite: blood-red, lossless at UV 4A. No blast property and no components, so GT adds no
        // dust-to-hot EBF print, no mixer/centrifuge/electrolyzer shortcut past the hearth: the hearth smelts
        // dusts straight to the molten fluid (uhv_superconductor.js, blastFurnaceTemp 6000 K on the recipe for
        // the coil display), and the vacuum freezer casts it under supercooled hydrogen. The fluid itself is
        // molten (6000 K, liquid(6000)): without a blast property GT would leave it at the 1200 K dust default.
        // GT still derives the standard molten <-> ingot solidifier/extractor prints from ingot+fluid; the
        // server scripts remove the plain solidifier so only the supercooled casting runs. The cable property
        // with no loss makes it the UHV superconductor wire.
        add(material("sanguinite")
                .ingot().liquid(6000)
                .color(0xff1a1a).secondaryColor(0x5c0a0a)
                .iconSet(MaterialIconSet.SHINY)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_FINE_WIRE, MaterialFlags.GENERATE_FOIL)
                .cableProperties(GTValues.VA[GTValues.UV], 4, 0, true)
                .formula("(Nt2Ke5)"));
    }

    /**
     * Soldering alloys: the pack's own solders beyond GT's tin and soldering alloy. Recipes: solders.js (production),
     * circuits_af9.js (HV-UV full replacement + wetware), plasma_soldering (UHV atomic soldering in the Orbital Array
     * Mk2). Spec: docs/solders.md
     * <pre>
     *   high_grade_solder  HV-UV circuits, replaces soldering alloy (tin stays as budget option)
     *   living_solder      UV wetware + neuron circuits, grown sterile from stem cells (bio-chain)
     *   plasma_solder      UHV+ atomic soldering, condensed from quark-gluon plasma + quantanium
     * </pre>
     */
    private static void solders() {
        // High-grade lead-free solder: SAC-InBi (tin-silver-copper-indium-bismuth family, simplified to
        // Sn9Bi1In1Ag1). Mixed at MV (one tier below its first users, the HV circuits), melted in the EBF at HV
        // like the MV circuit alloys (one tier above the mixer). GT derives the EBF recipe, parts and
        // decomposition from the components + blast property; the mixer recipe is in circuits_af9.js.
        add(material("high_grade_solder")
                .ingot().fluid()
                .color(0xd8dee8).iconSet(MaterialIconSet.METALLIC)
                .components(GTMaterials.Tin, 9, GTMaterials.Bismuth, 1, GTMaterials.Indium, 1,
                        GTMaterials.Silver, 1)
                .blastTemp(1500, GasTier.LOW, GTValues.VA[GTValues.HV], 500)
                .formula("(Sn9BiInAg)"));

        // Living solder: a conductive bio-hydrogel (silver nanowires in a crosslinked protein matrix, kept alive in
        // sterilized growth medium). No components and no blast property, so GT adds no electrolyzer, centrifuge or
        // EBF shortcut past the sterile bio-chain (solders.js): stem cells + sterilized growth medium + mutagen,
        // incubated sterile at UV. Used as the fluid in all wetware / neuron circuit assembler recipes.
        add(material("living_solder")
                .liquid()
                .color(0x6fe8a8)
                .formula("Ag(C2H5NO2)(H2O)"));

        // Plasma solder: quark-stabilised metallic plasma for atomic-level soldering (ion-by-ion deposition, no
        // reflow). Dust + ingot + fluid, no components and no blast property, so GT adds no mixer/EBF shortcut: the
        // dust is condensed in the Particle Accelerator's quark synthesis from QGP traps + quantanium (solders.js)
        // and melts to the fluid in GT's own extractor (dust -> fluid), which is what the plasma_soldering recipe
        // type in the Orbital Lithography Array Mk2 consumes (extended pattern, in orbit).
        add(material("plasma_solder")
                .dust().ingot().fluid()
                .color(0xb47cff)
                .formula("(Qc)(Qn)(Ed*)"));
    }

    /**
     * Quantanium: the ore that unlocks UHV. A metal of the Asteroid Field and nothing else: it has no GT vein anywhere
     * else, no other source, and no recipe takes it yet (the UHV hulls and circuits that will need it come
     * separately). Furnace-smeltable, so the gate is finding the ore, not a coil tier. The vein: vein_field.js.
     * Spec: docs/quantanium.md
     */
    private static void quantanium() {
        // Crushing gives two crushed ores per raw ore. Byproducts: the high-tech
        // metals the ore grew with (all obtainable elsewhere already). The formula
        // uses real element symbols in brannerite's substitution notation.
        add(material("quantanium")
                .ingot().fluid().dust().ore(2, 1)
                .color(0x9d4edd).secondaryColor(0x3c096c)
                .iconSet(MaterialIconSet.SHINY)
                .formula("(Ti,Nb)2O3")
                .addOreByproducts(GTMaterials.Titanium, GTMaterials.Trinium, GTMaterials.Niobium));
    }
}
