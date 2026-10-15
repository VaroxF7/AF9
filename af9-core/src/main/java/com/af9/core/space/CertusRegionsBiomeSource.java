package com.af9.core.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * A biome source of one base biome with rare round regions of a second one (af9:certus_regions): the world is cut
 * into square cells of {@code spacing} blocks, a cell has a region with the chance {@code chance}, its centre
 * drawn near the middle of the cell (so two regions are never closer than {@code spacing - 2 * jitter} blocks, by
 * default 2000: at a render distance of 32 chunks a player sees one at a time) and its radius {@code radius} blocks.
 * The layout is a pure function of the position and {@code salt}, the same in every world.
 */
public class CertusRegionsBiomeSource extends BiomeSource {

    public static final Codec<CertusRegionsBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("base").forGetter(s -> s.base),
            Biome.CODEC.fieldOf("crystal").forGetter(s -> s.crystal),
            Codec.INT.optionalFieldOf("spacing", 3000).forGetter(s -> s.spacing),
            Codec.INT.optionalFieldOf("jitter", 500).forGetter(s -> s.jitter),
            Codec.INT.optionalFieldOf("radius", 180).forGetter(s -> s.radius),
            Codec.DOUBLE.optionalFieldOf("chance", 0.6).forGetter(s -> s.chance),
            Codec.INT.optionalFieldOf("salt", 0).forGetter(s -> s.salt)
    ).apply(i, CertusRegionsBiomeSource::new));

    private final Holder<Biome> base;
    private final Holder<Biome> crystal;
    private final int spacing;
    private final int jitter;
    private final int radius;
    private final double chance;
    private final int salt;

    public CertusRegionsBiomeSource(Holder<Biome> base, Holder<Biome> crystal, int spacing, int jitter, int radius,
                                    double chance, int salt) {
        this.base = base;
        this.crystal = crystal;
        this.spacing = spacing;
        this.jitter = jitter;
        this.radius = radius;
        this.chance = chance;
        this.salt = salt;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(base, crystal);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = quartX * 4 + 2;
        int z = quartZ * 4 + 2;
        int cellX = Mth.floorDiv(x, spacing);
        int cellZ = Mth.floorDiv(z, spacing);
        long hash = mix(mix(cellX * 0x9E3779B97F4A7C15L + salt) ^ (cellZ * 0xC2B2AE3D27D4EB4FL));
        if ((hash >>> 40) / (double) (1L << 24) >= chance) return base;
        long hash2 = mix(hash);
        long hash3 = mix(hash2);
        int centerX = cellX * spacing + spacing / 2 + (int) Math.floorMod(hash2, 2L * jitter + 1) - jitter;
        int centerZ = cellZ * spacing + spacing / 2 + (int) Math.floorMod(hash3, 2L * jitter + 1) - jitter;
        long dx = x - centerX;
        long dz = z - centerZ;
        return dx * dx + dz * dz <= (long) radius * radius ? crystal : base;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        value ^= value >>> 33;
        return value;
    }
}
