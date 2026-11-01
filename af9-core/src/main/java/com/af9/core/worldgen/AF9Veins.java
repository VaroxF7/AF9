package com.af9.core.worldgen;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTOres;

import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * AF9's ore and fluid veins (were the KubeJS scripts {@code vein_*.js}). The ore veins are made after the pack's own
 * KubeJS scripts have run ({@link com.af9.core.mixin.OreLoaderMixin}): the base pack moves every GT vein it finds to
 * the Mining Dimension, and AF9's must keep their planets. The fluid veins are registered in GT's fluid vein phase.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID)
public final class AF9Veins {

    private static final Logger LOGGER = LogManager.getLogger("AF9/Veins");

    private AF9Veins() {}

    private static ResourceLocation id(String s) {
        return new ResourceLocation(s);
    }

    private static com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer layer(String name) {
        // the layers (GT's own and AF9's SimpleWorldGenLayers) are keyed by plain name
        var layer = com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils.WORLD_GEN_LAYERS.get(name);
        if (layer == null) throw new IllegalStateException("unknown world gen layer " + name);
        return layer;
    }

    // ---- ore veins ----

    /** A dike vein of pure ore: {@code material, weight} blocks over the height range. */
    private static void dike(String veinId, int clusterSize, int weight, float density, String layer, String dimension,
            int minY, int maxY, String[] materials, int[] weights) {
        GTOreDefinition vein = GTOres.blankOreDefinition();
        vein.clusterSize(clusterSize).weight(weight).density(density).discardChanceOnAirExposure(0.0f)
                .layer(layer(layer)).dimensions(id(dimension)).heightRangeUniform(minY, maxY)
                .dikeVeinGenerator(g -> {
                    for (int i = 0; i < materials.length; i++) {
                        g.withBlock(GTMaterials.get(materials[i]), weights[i], minY, maxY);
                    }
                });
        vein.register(id(veinId));
    }

    private static void beltDike(String veinId, String dimension, String material, int weight) {
        dike(veinId, 16, weight, 1.0f, "af9_asteroid", dimension, 5, 270, new String[] { material }, new int[] { 1 });
    }

    /** A standard vein of one material's ore in the Moon's stone. */
    private static void moonVein(String veinId, String materialName, int weight) {
        Material material = GTMaterials.get(materialName);
        GTOreDefinition vein = GTOres.blankOreDefinition();
        vein.clusterSize(24).weight(weight).density(0.8f).discardChanceOnAirExposure(0.0f).layer(layer("af9_moon"))
                .dimensions(id("ad_astra:moon")).heightRangeUniform(40, 120).standardVeinGenerator(g -> {
                    // all three stone variants: GT syncs the vein registry to every client on login and reads each
                    g.withBlock(ore(TagPrefix.ore, material));
                    g.withNetherBlock(ore(TagPrefix.oreNetherrack, material));
                    g.deepBlock = ore(TagPrefix.oreDeepslate, material);
                });
        vein.register(id(veinId));
    }

    private static NonNullSupplier<Block> ore(TagPrefix prefix, Material material) {
        return NonNullSupplier.of(() -> ChemicalHelper.getBlock(prefix, material));
    }

    /** Veins of GT and the pack that hold a mineral AF9 gets elsewhere are switched off (weight 0). */
    private static void switchOff() {
        Set<String> belts = Set.of("pitchblende", "uraninite", "naquadah");
        Set<String> titanium = Set.of("ilmenite", "rutile", "scheelite", "tungstate");
        for (ResourceLocation vid : new ArrayList<>(GTRegistries.ORE_VEINS.keys())) {
            if (vid.toString().startsWith("af9:")) continue;
            GTOreDefinition vein = GTRegistries.ORE_VEINS.get(vid);
            if (vein == null) continue;
            boolean condemned = false;
            String lower = vid.toString().toLowerCase();
            for (String word : titanium) if (lower.contains(word)) condemned = true;
            try {
                List<Material> materials = new ArrayList<>(vein.veinGenerator().getAllMaterials());
                for (Material m : materials) {
                    String n = m.getName();
                    if (belts.contains(n) || titanium.contains(n) || n.equals("oilsands")) condemned = true;
                }
            } catch (RuntimeException e) {
                // a generator without materials holds none of them
            }
            if (condemned) vein.weight(0);
        }
    }

    /** Called when GT has loaded every ore vein (scripts included) and is about to build the generators. */
    public static void oreVeins() {
        try {
            beltDike("af9:brannerite_vein", "af9:ceres", "brannerite", 80);
            beltDike("af9:pentlandite_vein", "af9:ceres", "pentlandite", 45);
            beltDike("af9:magnetite_vein", "af9:ceres", "magnetite", 45);
            beltDike("af9:cooperite_vein", "af9:ceres", "cooperite", 25);
            beltDike("af9:ceresite_vein", "af9:ceres", "ceresite", 20);
            beltDike("af9:chronite_vein", "af9:ceres", "chronite", 15);
            beltDike("af9:field_naquadah_vein", "af9:asteroid_field", "naquadah", 65);
            beltDike("af9:field_platinum_vein", "af9:asteroid_field", "platinum", 45);
            beltDike("af9:quantanium_vein", "af9:asteroid_field", "quantanium", 30);
            beltDike("af9:field_iridium_vein", "af9:asteroid_field", "iridium", 22);
            dike("af9:zircon_sands_vein", 35, 30, 0.6f, "af9_mars", "ad_astra:mars", 40, 120,
                    new String[] { "zircon", "monazite", "almandine" }, new int[] { 5, 2, 1 });
            dike("af9:venus_ilmenite_vein", 30, 40, 0.7f, "af9_venus", "ad_astra:venus", 40, 120,
                    new String[] { "ilmenite", "iron" }, new int[] { 5, 2 });
            moonVein("af9:artemite_vein", "artemite", 45);
            moonVein("af9:lesolite_vein", "lesolite", 30);
            moonVein("af9:tranquillite_vein", "tranquillite", 50);
        } catch (RuntimeException e) {
            LOGGER.error("could not register AF9's ore veins", e);
        }
        try {
            switchOff();
        } catch (RuntimeException e) {
            LOGGER.error("could not switch off the veins AF9 replaces", e);
        }
    }

    // ---- fluid veins ----

    private static void deposit(String name, String fluidId, int weight, int minYield, int maxYield, String dimension) {
        Supplier<Fluid> fluid = () -> ForgeRegistries.FLUIDS.getValue(id(fluidId));
        BedrockFluidDefinition.builder(id("af9:" + name)).fluid(fluid).weight(weight).yield(minYield, maxYield)
                .depletionAmount(1).depletionChance(100).depletedYield(25)
                .dimensions(Set.of(ResourceKey.create(Registries.DIMENSION, id(dimension)))).register();
    }

    /** GT's fluid vein phase (AF9Addon#registerFluidVeins): GT's oil deposits go, the belts' deposits come. */
    public static void fluidVeins() {
        for (String oil : new String[] { "gtceu:heavy_oil_deposit", "gtceu:light_oil_deposit", "gtceu:oil_deposit",
                "gtceu:raw_oil_deposit" }) {
            try {
                GTRegistries.BEDROCK_FLUID_DEFINITIONS.remove(id(oil));
            } catch (RuntimeException e) {
                LOGGER.error("could not remove the fluid vein {}", oil, e);
            }
        }
        String field = "af9:asteroid_field";
        String ceres = "af9:ceres";
        Object[][] deposits = {
                { "void_nitrogen", "gtceu:nitrogen", 40, 250, 600, field },
                { "void_oxygen", "gtceu:oxygen", 40, 250, 600, field },
                { "void_heavy_water", "gtceu:heavy_water", 20, 100, 300, field },
                { "void_sulfuric_acid", "gtceu:sulfuric_acid", 20, 150, 350, field },
                { "void_hydrochloric_acid", "gtceu:hydrochloric_acid", 15, 150, 350, field },
                { "void_nitric_acid", "gtceu:nitric_acid", 15, 150, 350, field },
                { "void_hydrofluoric_acid", "gtceu:hydrofluoric_acid", 10, 100, 250, field },
                { "void_phosphoric_acid", "gtceu:phosphoric_acid", 10, 100, 250, field },
                { "void_acetic_acid", "gtceu:acetic_acid", 8, 100, 250, field },
                { "void_ares_gas", "gtceu:ares_gas", 8, 100, 250, field },
                { "ceres_hydrogen", "gtceu:hydrogen", 40, 250, 600, ceres },
                { "ceres_helium", "gtceu:helium", 30, 200, 500, ceres },
                { "ceres_methane", "gtceu:methane", 30, 200, 500, ceres },
                { "ceres_argon", "gtceu:argon", 20, 100, 300, ceres },
                { "ceres_carbon_dioxide", "gtceu:carbon_dioxide", 20, 150, 350, ceres },
                { "ceres_neon", "gtceu:neon", 12, 50, 150, ceres },
                { "ceres_krypton", "gtceu:krypton", 8, 40, 120, ceres },
                { "ceres_xenon", "gtceu:xenon", 6, 30, 100, ceres },
                { "lunar_air", "gtceu:lunar_air", 100, 250, 600, "ad_astra:moon" },
        };
        for (Object[] d : deposits) {
            try {
                deposit((String) d[0], (String) d[1], (Integer) d[2], (Integer) d[3], (Integer) d[4], (String) d[5]);
            } catch (RuntimeException e) {
                LOGGER.error("could not register the fluid deposit af9:{}", d[0], e);
            }
        }
    }

    /** vein_oil.js: a belt's saved vein whose deposit no longer exists is forgotten, or the prospector draws nothing. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        int forgotten = 0;
        try {
            for (String dim : new String[] { "af9:asteroid_field", "af9:ceres", "ad_astra:moon" }) {
                ServerLevel level = event.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id(dim)));
                if (level == null) continue;
                BedrockFluidVeinSavedData data = BedrockFluidVeinSavedData.getOrCreate(level);
                int dimForgotten = 0;
                for (ChunkPos chunk : new ArrayList<>(data.veinFluids.keySet())) {
                    if (data.veinFluids.get(chunk).getDefinition() == null) {
                        data.veinFluids.remove(chunk);
                        dimForgotten++;
                    }
                }
                if (dimForgotten > 0) data.setDirty();
                forgotten += dimForgotten;
            }
            LOGGER.info("{} empty fluid veins of the belts forgotten", forgotten);
        } catch (RuntimeException e) {
            LOGGER.error("could not look at the fluid veins of the belts", e);
        }
    }
}
