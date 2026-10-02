package com.af9.core.space;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ores of the Asteroid Field, grown into the rock as it is made (docs/asteroid-fission.md §2.3): GregTech's own ore blocks of
 * the rock's stone (andesite, tuff, basalt, blackstone), wherever a noise of the ore is high. The noises are of the position in the
 * world, not of the rock, so an ore runs through the rock at every height and on into the rock next to it, in blobs about a dozen
 * blocks across, the same whichever chunk is made first.
 * <p>
 * This replaces GT's ore veins in the field: a standard GT vein is one small blob at one random height, which in rocks that hang
 * anywhere in 250 blocks of height lies in the void or cuts the rock in a thin slab.
 */
final class AsteroidOres {

    /** An ore: its material, the height of its noise above which the rock is that ore (higher: rarer), and its salt. */
    private enum Ore {
        // about 9 %, 4.5 %, 4.5 % and 2.5 % of the rock
        BRANNERITE("brannerite", 0.60, 11),
        PENTLANDITE("pentlandite", 0.68, 23),
        MAGNETITE("magnetite", 0.68, 37),
        COOPERITE("cooperite", 0.74, 41);

        final String material;
        final double threshold;
        final long salt;

        Ore(String material, double threshold, long salt) {
            this.material = material;
            this.threshold = threshold;
            this.salt = salt;
        }
    }

    /** The noise's scale: its blobs are about 1 / this many blocks across. */
    private static final double SCALE = 0.07;

    private static final Ore[] ORES = Ore.values();

    /** The noises of one world, made once per seed. */
    private record Fields(long seed, SimplexNoise[] noises) {}

    private static volatile Fields fields;

    /** Material and ore blocks looked up once: "material/stone" -> the ore block (Air: there is none). */
    private static final Map<String, Block> BLOCKS = new ConcurrentHashMap<>();

    private AsteroidOres() {}

    private static SimplexNoise[] noises(long seed) {
        Fields current = fields;
        if (current == null || current.seed != seed) {
            SimplexNoise[] made = new SimplexNoise[ORES.length];
            for (int i = 0; i < made.length; i++) {
                made[i] = new SimplexNoise(new XoroshiroRandomSource(seed ^ (ORES[i].salt * 0x9E3779B97F4A7C15L)));
            }
            current = new Fields(seed, made);
            fields = current;
        }
        return current.noises;
    }

    /** The ore block for the stone at a point, or {@code stone} itself when there is no ore there. */
    static BlockState oreAt(long seed, int x, int y, int z, BlockState stone) {
        SimplexNoise[] noises = noises(seed);
        for (int i = 0; i < ORES.length; i++) {
            if (noises[i].getValue(x * SCALE, y * SCALE, z * SCALE) > ORES[i].threshold) {
                Block ore = oreBlock(ORES[i].material, stone.getBlock());
                return ore == Blocks.AIR ? stone : ore.defaultBlockState();
            }
        }
        return stone;
    }

    private static TagPrefix prefixOf(Block stone) {
        if (stone == Blocks.ANDESITE) return TagPrefix.oreAndesite;
        if (stone == Blocks.TUFF) return TagPrefix.oreTuff;
        if (stone == Blocks.BASALT) return TagPrefix.oreBasalt;
        if (stone == Blocks.BLACKSTONE) return TagPrefix.oreBlackstone;
        return null;
    }

    /** GT's ore block of a material in a stone; Air when GT has none (the log says so once). */
    private static Block oreBlock(String materialName, Block stone) {
        return BLOCKS.computeIfAbsent(materialName + "/" + stone.getDescriptionId(), key -> {
            TagPrefix prefix = prefixOf(stone);
            Material material = GTCEuAPI.materialManager.getRegisteredMaterials().stream()
                    .filter(candidate -> candidate.getName().equals(materialName)).findFirst().orElse(null);
            if (prefix == null || material == null) {
                AF9Core.LOGGER.warn("Asteroid ore {} in {}: GT has no such material or stone prefix", materialName,
                        stone.getDescriptionId());
                return Blocks.AIR;
            }
            Block block = ChemicalHelper.getBlock(prefix, material);
            if (block == null || block == Blocks.AIR) {
                AF9Core.LOGGER.warn("Asteroid ore {} in {}: GT has no ore block", materialName, stone.getDescriptionId());
                return Blocks.AIR;
            }
            return block;
        });
    }
}
