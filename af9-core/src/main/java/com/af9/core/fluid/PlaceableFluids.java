package com.af9.core.fluid;

import com.af9.core.AF9Core;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Placeable versions of GregTech's cold liquids: a liquid block ({@code af9:<fluid>}) and a bucket
 * ({@code af9:<fluid>_bucket}) for each. GregTech registers its fluids without blocks, so nothing of it can go in
 * the world; this runs after GregTech's own registrations (AF9 loads after GTCEu) and adds both, fluid by fluid.
 * <p>
 * The list below is curated, not automatic: cold (below boiling) and ungated. Gases, plasmas and molten metals never
 * qualify (checked at runtime too), and neither do the fluids the pack gates behind its own chains even where cold
 * ({@link #GATED}: off-world oil, drilled acids and water, void-miner drilling fluid). What qualifies still forms
 * infinite sources like water, so only fluids whose infinity nobody minds are listed. {@code tools/textures/buckets.py}
 * reads this list for the buckets' models, blockstates and names: add the id here, rerun it, rebuild.
 */
public final class PlaceableFluids {

    // BEGIN-FLUIDS (tools/textures/buckets.py reads this list)
    static final List<String> FLUIDS = List.of(
            "distilled_water", "salt_water", "lubricant", "creosote", "biomass",
            "ethanol", "methanol", "bioethanol", "seed_oil", "fish_oil", "wood_vinegar",
            "hydrogen_peroxide", "acetone", "phenol", "toluene", "benzene",
            "dimethylbenzene", "chlorobenzene", "nitrobenzene", "glue", "concrete");
    // END-FLUIDS
    /** Below water's boiling point: only cold liquids may go in the world. */
    private static final int BOILING_KELVIN = 373;
    /** Cold but pack-gated: drilling and off-world fluids whose infinity would skip their chains. */
    private static final Set<String> GATED = Set.of(
            "oil", "oil_heavy", "oil_medium", "oil_light", "heavy_water",
            "sulfuric_acid", "hydrochloric_acid", "nitric_acid", "hydrofluoric_acid",
            "phosphoric_acid", "acetic_acid", "diluted_sulfuric_acid", "diluted_hydrochloric_acid",
            "drilling_fluid");
    /** Fluid id to its bucket, filled in the item phase for the blocks' pickup. */
    private static final Map<ResourceLocation, Item> BUCKETS = new HashMap<>();
    /** Bucket to its fluid, for the buckets' tint. */
    private static final Map<Item, Fluid> FLUID_OF_BUCKET = new HashMap<>();
    private static final Set<String> WARNED = new java.util.HashSet<>();

    private PlaceableFluids() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) registerBlocks(event);
            else if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) registerBuckets(event);
        });
    }

    private static void registerBlocks(RegisterEvent event) {
        int made = 0;
        for (String path : FLUIDS) {
            ForgeFlowingFluid flowing = flowing(path);
            if (!qualifies(path, flowing)) continue;
            ResourceLocation id = new ResourceLocation(AF9Core.MOD_ID, path);
            if (ForgeRegistries.BLOCKS.containsKey(id)) {
                warn(path + ": block " + id + " is already taken, skipped");
                continue;
            }
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(flowing);
            PlaceableFluidBlock block = new PlaceableFluidBlock(flowing, () -> BUCKETS.get(fluidId),
                    BlockBehaviour.Properties.of().noCollission().strength(100.0F).noLootTable());
            event.register(ForgeRegistries.Keys.BLOCKS, helper -> helper.register(id, block));
            made++;
        }
        AF9Core.LOGGER.info("Placeable fluids: {} liquid blocks registered", made);
    }

    private static void registerBuckets(RegisterEvent event) {
        int made = 0;
        for (String path : FLUIDS) {
            ForgeFlowingFluid flowing = flowing(path);
            if (!qualifies(path, flowing)) continue;
            ResourceLocation blockId = new ResourceLocation(AF9Core.MOD_ID, path);
            ResourceLocation bucketId = new ResourceLocation(AF9Core.MOD_ID, path + "_bucket");
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || block instanceof net.minecraft.world.level.block.AirBlock
                    || ForgeRegistries.ITEMS.containsKey(bucketId)) {
                warn(path + ": no liquid block to pair a bucket with, skipped");
                continue;
            }
            PlaceableFluidBucket bucket = new PlaceableFluidBucket(() -> flowing, () -> block,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
            event.register(ForgeRegistries.Keys.ITEMS, helper -> helper.register(bucketId, bucket));
            BUCKETS.put(ForgeRegistries.FLUIDS.getKey(flowing), bucket);
            FLUID_OF_BUCKET.put(bucket, flowing);
            made++;
        }
        AF9Core.LOGGER.info("Placeable fluids: {} buckets registered", made);
    }

    /** The flowing variant GT registers next to the still, null when GT has no such fluid. */
    private static ForgeFlowingFluid flowing(String path) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("gtceu", "flowing_" + path));
        return fluid instanceof ForgeFlowingFluid.Flowing flowing ? flowing : null;
    }

    private static boolean qualifies(String path, ForgeFlowingFluid flowing) {
        if (flowing == null) {
            warn(path + ": no gtceu flowing fluid by that name, skipped");
            return false;
        }
        if (GATED.contains(path)) return false;
        var type = flowing.getFluidType();
        if (type.getDensity() <= 0 || type.isLighterThanAir() || type.getTemperature() >= BOILING_KELVIN) {
            return false;
        }
        Material material = ChemicalHelper.getMaterial(flowing);
        if (!material.isNull() && material.getProperty(PropertyKey.BLAST) != null) return false;
        return true;
    }

    private static void warn(String message) {
        if (WARNED.add(message)) AF9Core.LOGGER.warn("Placeable fluids: {}", message);
    }

    /** The curated fluid paths, for the buckets' models, blockstates, names and tint. */
    public static List<String> paths() {
        return FLUIDS;
    }

    /** A placeable bucket's fluid, null for anything else. */
    public static Fluid fluidOfBucket(Item bucket) {
        return FLUID_OF_BUCKET.get(bucket);
    }
}
