package com.af9.core.compat.tconstruct;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

/**
 * Tinkers' Construct x GregTech: the recipes of the Smeltery, the Foundry and the Casting Table / Basin for every fluid of
 * GT that has a solid form (the KubeJS script server_scripts/mods/tconstruct/gt_molds.js registers what this builds).
 *
 * <ul>
 * <li>Melting: every solid shape of a material (dusts, ingots, plates, gears, rods, rotors, pipes, ...) melts into the
 * material's fluid, in the amount GT's own solidifier takes for it. The temperature is the material's blast temperature
 * (700 for other metals, 400 for the rest): what needs an EBF in GT needs the hottest fuel of Tinkers (or cannot be
 * melted at all), so the Smeltery does not skip GT's heat gates. Casting is not gated, the fluid had to be made in GT.
 * <li>Casting: every GT casting mold (ingot, nugget, plate, gear, small gear, rotor, block, the five fluid pipes, the
 * glass ones, the anvil) is a cast on the Casting Table / Basin, with the amount of GT's Fluid Solidifier. The
 * Tinkers casts (gold, sand) work on GT-only metals too.
 * <li>Alloys: a GT alloy of ingot metals is made in the Smeltery from the molten parts, in the ratio of its components.
 * </ul>
 *
 * A metal that Tinkers has a fluid for (molten_iron, molten_copper, ...) uses Tinkers' fluid at its scale (an ingot is 90
 * mB) instead of a second fluid of the same metal, and Tinkers' own recipes for the shapes it knows are left alone.
 * GT's fluids are at GT's scale (an ingot is 144 mB).
 */
public final class TinkersGtRecipes {

    private static final Logger LOGGER = LogManager.getLogger("AF9/TinkersGt");

    /** mB of an ingot in GT's fluids. */
    private static final int GT_UNIT = GTValues.L;
    /** mB of an ingot in Tinkers' fluids. */
    private static final int TC_UNIT = 90;
    /** GT name -> Tinkers name, where they differ. */
    private static final Map<String, String> ALIASES = Map.of("aluminium", "aluminum");

    /** What melts: all shapes with a fluid amount. */
    private static final List<TagPrefix> MELTING = List.of(dust, dustSmall, dustTiny, ingot, nugget, block, plate,
            plateDouble, plateDense, gear, gearSmall, rod, rodLong, bolt, screw, ring, foil, spring, springSmall, rotor,
            round, wireFine, pipeTinyFluid, pipeSmallFluid, pipeNormalFluid, pipeLargeFluid, pipeHugeFluid);
    /** The shapes Tinkers melts itself for its own metals (by the forge tags GT's items carry). */
    private static final Set<TagPrefix> TINKERS_MELTS = Set.of(dust, ingot, nugget, block, plate, gear, rod);

    /** Mold shape: the item it makes, the fluid amount as a fraction of an ingot, and where it is cast. */
    private record Shape(String mold, TagPrefix prefix, int times, int over, boolean basin) {}

    private static final List<Shape> SHAPES = List.of(
            new Shape("ingot_casting_mold", ingot, 1, 1, false),
            new Shape("nugget_casting_mold", nugget, 1, 9, false),
            new Shape("plate_casting_mold", plate, 1, 1, false),
            new Shape("gear_casting_mold", gear, 4, 1, false),
            new Shape("small_gear_casting_mold", gearSmall, 1, 1, false),
            new Shape("rotor_casting_mold", rotor, 4, 1, false),
            new Shape("block_casting_mold", block, 9, 1, true),
            new Shape("tiny_pipe_casting_mold", pipeTinyFluid, 1, 2, false),
            new Shape("small_pipe_casting_mold", pipeSmallFluid, 1, 1, false),
            new Shape("normal_pipe_casting_mold", pipeNormalFluid, 3, 1, false),
            new Shape("large_pipe_casting_mold", pipeLargeFluid, 6, 1, false),
            new Shape("huge_pipe_casting_mold", pipeHugeFluid, 12, 1, false));

    /** Tinkers' own casts (the gold cast keeps, the sand cast is used up): tag name, shape, ingots times over. */
    private record TinkersCast(String name, TagPrefix prefix, int times, int over) {}

    private static final List<TinkersCast> TINKERS_CASTS = List.of(
            new TinkersCast("ingot", ingot, 1, 1),
            new TinkersCast("nugget", nugget, 1, 9),
            new TinkersCast("plate", plate, 1, 1),
            new TinkersCast("gear", gear, 4, 1),
            new TinkersCast("rod", rod, 1, 2));

    /** The fluid of a material: which one, how much of it is an ingot, and whether Tinkers melts and casts the shapes. */
    private record Spec(ResourceLocation fluid, int unit, boolean tinkers, boolean tinkersShapes) {}

    private TinkersGtRecipes() {}

    /** Recipe id, recipe json, recipe id, recipe json, ... (flat, for the script); empty without Tinkers' Construct. */
    public static String[] build() {
        if (!ModList.get().isLoaded("tconstruct")) return new String[0];
        Map<String, String> recipes = new LinkedHashMap<>();
        Set<String> melted = new HashSet<>();
        Set<String> cast = new HashSet<>();
        int materials = 0;
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            try {
                Spec spec = spec(material);
                if (spec == null) continue;
                materials++;
                melting(recipes, melted, material, spec);
                casting(recipes, cast, material, spec);
            } catch (RuntimeException e) {
                LOGGER.error("Tinkers x GT: {} skipped", material.getName(), e);
            }
        }
        try {
            alloys(recipes);
            misc(recipes, cast);
        } catch (RuntimeException e) {
            LOGGER.error("Tinkers x GT: alloys / extras skipped", e);
        }
        LOGGER.info("Tinkers x GT: {} recipes for {} fluids", recipes.size(), materials);
        String[] flat = new String[recipes.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            flat[i++] = entry.getKey();
            flat[i++] = entry.getValue();
        }
        return flat;
    }

    // ---- the fluid of a material ----

    private static Spec spec(Material material) {
        if (material.isNull()) return null;
        FluidProperty property = material.getProperty(PropertyKey.FLUID);
        if (property == null) return null;
        Fluid liquid = property.solidifiesFrom();
        if (liquid == null) return null;
        ResourceLocation gtFluid = ForgeRegistries.FLUIDS.getKey(liquid);
        if (gtFluid == null) return null;
        if (material.hasProperty(PropertyKey.INGOT)) {
            String name = ALIASES.getOrDefault(material.getName(), material.getName());
            ResourceLocation molten = new ResourceLocation("tconstruct", "molten_" + name);
            if (ForgeRegistries.FLUIDS.containsKey(molten)) {
                return new Spec(molten, TC_UNIT, true, name.equals(material.getName()));
            }
        }
        return new Spec(gtFluid, GT_UNIT, false, false);
    }

    /** Tinkers' temperature (no fuel burns hotter than 1800): what needs GT's EBF needs the hottest fuel or cannot melt. */
    private static int temperature(Material material) {
        int blast = material.getBlastTemperature();
        if (blast > 0) return blast;
        return material.hasProperty(PropertyKey.INGOT) ? 700 : 400;
    }

    /** Time of Tinkers' own recipes: 57 ticks for an ingot of 700, as many times the square root of the ingots. */
    private static int time(int temperature, int amount, int unit) {
        double ingots = (double) amount / unit;
        return Math.max(1, (int) Math.round(temperature / 12.0 * Math.sqrt(ingots)));
    }

    // ---- melting ----

    private static void melting(Map<String, String> recipes, Set<String> melted, Material material, Spec spec) {
        int temperature = temperature(material);
        for (TagPrefix prefix : MELTING) {
            if (spec.tinkersShapes() && TINKERS_MELTS.contains(prefix)) continue;
            long units = prefix.getMaterialAmount(material);
            if (units <= 0) continue;
            int amount = (int) (spec.unit() * units / GTValues.M);
            if (amount <= 0) continue;
            ResourceLocation item = itemOf(ChemicalHelper.get(prefix, material));
            if (item == null || !melted.add(item.toString())) continue;

            JsonObject json = new JsonObject();
            json.addProperty("type", "tconstruct:melting");
            json.add("ingredient", item(item));
            json.add("result", fluidOut(spec.fluid(), amount));
            json.addProperty("temperature", temperature);
            json.addProperty("time", time(temperature, amount, spec.unit()));
            recipes.put(id("melting", spec.fluid(), item.getPath()), json.toString());
        }
    }

    // ---- casting ----

    private static void casting(Map<String, String> recipes, Set<String> cast, Material material, Spec spec) {
        int temperature = temperature(material);
        for (Shape shape : SHAPES) {
            ResourceLocation result = itemOf(ChemicalHelper.get(shape.prefix(), material));
            if (result == null) continue;
            int amount = spec.unit() * shape.times() / shape.over();
            cast(recipes, cast, spec.fluid(), amount, result, "gtceu:" + shape.mold(), shape.basin(), false,
                    time(temperature, amount, spec.unit()), shape.mold());
        }
        if (spec.tinkersShapes()) return; // Tinkers casts these for its own metals
        for (TinkersCast tinkers : TINKERS_CASTS) {
            ResourceLocation result = itemOf(ChemicalHelper.get(tinkers.prefix(), material));
            if (result == null) continue;
            int amount = spec.unit() * tinkers.times() / tinkers.over();
            int cooling = time(temperature, amount, spec.unit());
            cast(recipes, cast, spec.fluid(), amount, result, "#tconstruct:casts/multi_use/" + tinkers.name(), false,
                    false, cooling, "gold_" + tinkers.name());
            cast(recipes, cast, spec.fluid(), amount, result, "#tconstruct:casts/single_use/" + tinkers.name(), false,
                    true, cooling, "sand_" + tinkers.name());
        }
        // a block needs no cast in the Basin
        ResourceLocation block = itemOf(ChemicalHelper.get(TagPrefix.block, material));
        if (block != null) {
            cast(recipes, cast, spec.fluid(), spec.unit() * 9, block, null, true, false,
                    time(temperature, spec.unit() * 9, spec.unit()), "block");
        }
    }

    /**
     * One casting recipe. {@code castIngredient} is an item id, "#tag" or null (no cast); duplicates (two materials
     * with the same item) are dropped.
     */
    private static void cast(Map<String, String> recipes, Set<String> seen, ResourceLocation fluid, int amount,
                             ResourceLocation result, String castIngredient, boolean basin, boolean consumed,
                             int cooling, String label) {
        if (!seen.add(fluid + "|" + castIngredient + "|" + result)) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", basin ? "tconstruct:casting_basin" : "tconstruct:casting_table");
        if (castIngredient != null) {
            JsonObject cast = new JsonObject();
            if (castIngredient.startsWith("#")) cast.addProperty("tag", castIngredient.substring(1));
            else cast.addProperty("item", castIngredient);
            json.add("cast", cast);
            json.addProperty("cast_consumed", consumed);
        }
        JsonObject in = new JsonObject();
        in.addProperty("name", fluid.toString());
        in.addProperty("amount", amount);
        json.add("fluid", in);
        json.add("result", item(result));
        json.addProperty("cooling_time", Math.max(1, cooling));
        recipes.put(id("casting", fluid, result.getPath() + "_" + label), json.toString());
    }

    // ---- alloys ----

    /** GT alloys of ingot metals, mixed from the molten parts in GT's ratio (Tinkers' own alloys are left alone). */
    private static void alloys(Map<String, String> recipes) {
        Set<String> made = new HashSet<>();
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            try {
                if (!material.hasProperty(PropertyKey.INGOT) || material.getMaterialComponents().size() < 2) continue;
                Spec spec = spec(material);
                if (spec == null || spec.tinkers() || !made.add(spec.fluid().toString())) continue;
                JsonArray inputs = new JsonArray();
                Set<String> parts = new HashSet<>();
                long total = 0;
                int temperature = temperature(material);
                boolean ok = true;
                for (MaterialStack component : material.getMaterialComponents()) {
                    Material part = component.material();
                    Spec partSpec = part.hasProperty(PropertyKey.INGOT) ? spec(part) : null;
                    if (partSpec == null || component.amount() <= 0 || !parts.add(partSpec.fluid().toString())) {
                        ok = false;
                        break;
                    }
                    JsonObject in = new JsonObject();
                    in.addProperty("name", partSpec.fluid().toString());
                    in.addProperty("amount", partSpec.unit() * component.amount());
                    inputs.add(in);
                    total += component.amount();
                    temperature = Math.max(temperature, temperature(part));
                }
                if (!ok) {
                    made.remove(spec.fluid().toString());
                    continue;
                }
                JsonObject json = new JsonObject();
                json.addProperty("type", "tconstruct:alloy");
                json.add("inputs", inputs);
                json.add("result", fluidOut(spec.fluid(), (int) (spec.unit() * total)));
                json.addProperty("temperature", temperature);
                recipes.put(id("alloy", spec.fluid(), "mix"), json.toString());
            } catch (RuntimeException e) {
                LOGGER.error("Tinkers x GT: alloy {} skipped", material.getName(), e);
            }
        }
    }

    // ---- glass, the anvil ----

    /** The molds that make something else than a shape of a material: glass bottle / tube / block, the anvil. */
    private static void misc(Map<String, String> recipes, Set<String> cast) {
        Spec glass = spec(GTMaterials.Glass);
        if (glass != null) {
            int glassTemperature = 400;
            extra(recipes, cast, glass, glass.unit(), "minecraft:glass_bottle", "bottle_casting_mold", false,
                    glassTemperature);
            extra(recipes, cast, glass, glass.unit(), "gtceu:glass_tube", "ball_casting_mold", false, glassTemperature);
            extra(recipes, cast, glass, glass.unit(), "minecraft:glass", "block_casting_mold", true, glassTemperature);
        }
        Spec iron = spec(GTMaterials.Iron);
        if (iron != null) {
            extra(recipes, cast, iron, iron.unit() * 31, "minecraft:anvil", "anvil_casting_mold", true,
                    temperature(GTMaterials.Iron));
        }
    }

    private static void extra(Map<String, String> recipes, Set<String> cast, Spec spec, int amount, String result,
                              String mold, boolean basin, int temperature) {
        ResourceLocation item = new ResourceLocation(result);
        if (!ForgeRegistries.ITEMS.containsKey(item)) return;
        cast(recipes, cast, spec.fluid(), amount, item, "gtceu:" + mold, basin, false,
                time(temperature, amount, spec.unit()), mold);
    }

    // ---- json ----

    private static ResourceLocation itemOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return ForgeRegistries.ITEMS.getKey(stack.getItem());
    }

    private static JsonObject item(ResourceLocation id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return json;
    }

    private static JsonObject fluidOut(ResourceLocation fluid, int amount) {
        JsonObject json = new JsonObject();
        json.addProperty("fluid", fluid.toString());
        json.addProperty("amount", amount);
        return json;
    }

    private static String id(String kind, ResourceLocation fluid, String name) {
        return "af9:tinkers_gt/" + kind + "/" + clean(fluid.getNamespace() + "_" + fluid.getPath()) + "/" + clean(name);
    }

    private static String clean(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.\\-]", "_");
    }
}
