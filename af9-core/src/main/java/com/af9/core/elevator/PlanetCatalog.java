package com.af9.core.elevator;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The fluids a Space Elevator's liquid missions bring home (docs/space-elevator.md): GTNH's Space Pumping table
 * ({@code gtnhintergalactic.recipe.SpacePumpingRecipes}), a planet type and a gas type to a fluid and its amount, with
 * GTNH's numbers. Left out are the three fluids GregTech does not have here: ender goo (3, 1), extra heavy oil (3, 2)
 * and GalaxySpace's unknown water (8, 4).
 * <p>
 * GTNH pumps the amount every second, for energy alone; here it is what one mission brings, a flight of minutes with
 * the drone's hydrogen, coolant and energy ({@link SpaceMissionMachine#MISSION}). The drone says how far a mission
 * goes ({@link #droneFor}); the fluid is picked on the elevator's screen.
 * <p>
 * Planet type 9 is AF9's own (GTNH's table ends at 8): the far dark, reached by the Mk-IV, bringing home the fluids
 * of AF9's green chemistry (docs/green-chemistry.md) and richer cuts of hydrogen sulfide and radon.
 */
public final class PlanetCatalog {

    /** A fluid of a planet type: GTNH's two numbers for it, the fluid, and the buckets one mission brings. */
    public record Cargo(int planet, int gas, Fluid fluid, int buckets) {

        public int millibuckets() {
            return buckets * 1000;
        }

        /** The least drone that reaches the planet. */
        public int drone() {
            return droneFor(planet);
        }

        /** Both numbers as one (a run carries it in its data): 502 is planet type 5, gas type 2. */
        public int code() {
            return planet * 100 + gas;
        }
    }

    private static volatile List<Cargo> all;

    private PlanetCatalog() {}

    /**
     * The least Mining Drone tier that reaches a planet type: MK-I types 2 and 3, MK-II 4 and 5, MK-III 6 and 7,
     * MK-IV types 8 and 9.
     */
    public static int droneFor(int planet) {
        return planet / 2;
    }

    /** Every fluid there is, in GTNH's order: by planet type, then by gas type. */
    public static List<Cargo> all() {
        List<Cargo> list = all;
        if (list == null) all = list = build();
        return list;
    }

    private static List<Cargo> build() {
        List<Cargo> list = new ArrayList<>();
        // T2
        add(list, 2, 1, GTMaterials.Chlorobenzene, 896);
        // T3
        add(list, 3, 3, Fluids.LAVA, 1_800);
        add(list, 3, 4, GTMaterials.NaturalGas, 1_400);
        // T4
        add(list, 4, 1, GTMaterials.SulfuricAcid, 784);
        add(list, 4, 2, GTMaterials.Iron, 896);
        add(list, 4, 3, GTMaterials.Oil, 1_400);
        add(list, 4, 4, GTMaterials.OilHeavy, 1_792);
        add(list, 4, 5, GTMaterials.Lead, 896);
        add(list, 4, 6, GTMaterials.RawOil, 1_400);
        add(list, 4, 7, GTMaterials.OilLight, 780);
        add(list, 4, 8, GTMaterials.CarbonDioxide, 1_680);
        // T5
        add(list, 5, 1, GTMaterials.CarbonMonoxide, 4_480);
        add(list, 5, 2, GTMaterials.Helium3, 2_800);
        add(list, 5, 3, GTMaterials.SaltWater, 2_800);
        add(list, 5, 4, GTMaterials.Helium, 1_400);
        add(list, 5, 5, fluid(GTMaterials.Oxygen, FluidStorageKeys.LIQUID), 896);
        add(list, 5, 6, GTMaterials.Neon, 32);
        add(list, 5, 7, GTMaterials.Argon, 32);
        add(list, 5, 8, GTMaterials.Krypton, 8);
        add(list, 5, 9, GTMaterials.Methane, 1_792);
        add(list, 5, 10, GTMaterials.HydrogenSulfide, 392);
        add(list, 5, 11, GTMaterials.Ethane, 1_194);
        // T6
        add(list, 6, 1, GTMaterials.Deuterium, 1_568);
        add(list, 6, 2, GTMaterials.Tritium, 240);
        add(list, 6, 3, GTMaterials.Ammonia, 240);
        add(list, 6, 4, GTMaterials.Xenon, 16);
        add(list, 6, 5, GTMaterials.Ethylene, 1_792);
        // T7
        add(list, 7, 1, GTMaterials.HydrofluoricAcid, 672);
        add(list, 7, 2, GTMaterials.Fluorine, 1_792);
        add(list, 7, 3, GTMaterials.Nitrogen, 1_792);
        add(list, 7, 4, GTMaterials.Oxygen, 1_792);
        // T8
        add(list, 8, 1, GTMaterials.Hydrogen, 1_568);
        add(list, 8, 2, GTMaterials.LiquidAir, 875);
        add(list, 8, 3, GTMaterials.Copper, 672);
        add(list, 8, 5, GTMaterials.DistilledWater, 17_920);
        add(list, 8, 6, GTMaterials.Radon, 64);
        add(list, 8, 7, GTMaterials.Tin, 672);
        // T9 (AF9's own far dark, Mk-IV): green-chemistry fluids and richer sour gas and radon
        add(list, 9, 1, GTMaterials.BioDiesel, 1_400);
        add(list, 9, 2, af9Fluid("bioethanol"), 1_792);
        add(list, 9, 3, GTMaterials.Benzene, 1_400);
        add(list, 9, 4, GTMaterials.Chloroform, 896);
        add(list, 9, 5, GTMaterials.Chlorobenzene, 1_120);
        add(list, 9, 6, GTMaterials.HydrogenSulfide, 784);
        add(list, 9, 7, GTMaterials.Radon, 128);
        // LXA-1, the far dark's light exotic for Sanguinite (docs/uhv-superconductor.md): 64 buckets a Mk-IV mission
        add(list, 9, 8, af9Fluid("lxa_1"), 64);
        // Ceres volatiles: the lower belt's exosphere gases, richer cuts than the same fluids nearer home
        // (type 5 holds methane, ethane, helium, neon, argon and krypton; type 6 holds xenon)
        add(list, 9, 9, GTMaterials.Methane, 2_200);
        add(list, 9, 10, GTMaterials.Ethane, 1_500);
        add(list, 9, 11, GTMaterials.Helium, 1_800);
        add(list, 9, 12, GTMaterials.Argon, 64);
        add(list, 9, 13, GTMaterials.Neon, 64);
        add(list, 9, 14, GTMaterials.Krypton, 16);
        add(list, 9, 15, GTMaterials.Xenon, 32);
        // Lunar Air, the Moon's air for the Moon Sand chain (docs/semiconductor-factory.md): 1,200 buckets a Mk-IV
        // mission (drilling it on the Moon is the earlier way, vein_oil.js)
        add(list, 9, 16, af9Fluid("lunar_air"), 1_200);
        return List.copyOf(list);
    }

    private static void add(List<Cargo> list, int planet, int gas, Material material, int buckets) {
        add(list, planet, gas, material.hasFluid() ? material.getFluid() : null, buckets);
    }

    /** Nothing when GT has no such fluid here. */
    private static void add(List<Cargo> list, int planet, int gas, Fluid fluid, int buckets) {
        if (fluid != null && fluid != Fluids.EMPTY) list.add(new Cargo(planet, gas, fluid, buckets));
    }

    private static Fluid fluid(Material material, FluidStorageKey key) {
        return material.hasFluid() ? material.getFluid(key) : null;
    }

    /**
     * The fluid of one of AF9's own materials ({@link com.af9.core.registry.AF9Materials}: bioethanol), looked up by
     * id at runtime. Null when it does not exist, and the mission is then skipped like GTNH's three missing fluids.
     */
    private static Fluid af9Fluid(String id) {
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation("gtceu", id));
    }

    /** The fluid of a planet type and a gas type, null when there is none. */
    public static Cargo find(int planet, int gas) {
        for (Cargo cargo : all()) {
            if (cargo.planet() == planet && cargo.gas() == gas) return cargo;
        }
        return null;
    }

    /** The fluid of a {@link Cargo#code() code}, null when there is none. */
    public static Cargo byCode(int code) {
        return find(code / 100, code % 100);
    }

    /** The planet types that hold a fluid, from the nearest. */
    public static List<Integer> planets() {
        List<Integer> planets = new ArrayList<>();
        for (Cargo cargo : all()) {
            if (!planets.contains(cargo.planet())) planets.add(cargo.planet());
        }
        return planets;
    }

    /** The fluids of a planet type, by gas type. */
    public static List<Cargo> of(int planet) {
        List<Cargo> cargoes = new ArrayList<>();
        for (Cargo cargo : all()) {
            if (cargo.planet() == planet) cargoes.add(cargo);
        }
        return cargoes;
    }

    /** The fluids a drone of a tier reaches. */
    public static List<Cargo> reach(int droneTier) {
        List<Cargo> cargoes = new ArrayList<>();
        for (Cargo cargo : all()) {
            if (cargo.drone() <= droneTier) cargoes.add(cargo);
        }
        return cargoes;
    }
}
