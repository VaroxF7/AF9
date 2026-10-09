package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The stars the Dyson Swarms circle. A swarm takes its star from the solar system of the dimension it stands in (Ad
 * Astra's planet data: the Sun for the Overworld, the Moon, Mars, Venus, Mercury, the asteroid field, AF9's planets and
 * every orbit; Alpha Centauri, Ad Astra's {@code proxima_centauri}, for Glacio and its orbit; a datapack's own system
 * is its own star). One swarm can run on a star: the first one holds it (saved with the world, "af9_dyson_stars"), any
 * number more can be built but stay idle until the star is free.
 */
public final class DysonStars extends SavedData {

    /** Ad Astra's ids of the two systems the pack has. */
    public static final ResourceLocation SUN = new ResourceLocation("ad_astra", "solar_system");
    public static final ResourceLocation ALPHA_CENTAURI = new ResourceLocation("ad_astra", "proxima_centauri");

    private static final String NAME = "af9_dyson_stars";
    /** Dimensions with no star in them at all. */
    private static final Set<String> NO_STAR = Set.of("minecraft:the_nether", "allthemodium:mining");

    /** Where a swarm holds a star. */
    public record Claim(ResourceLocation dimension, BlockPos pos) {

        public String where() {
            return pos.getX() + " " + pos.getY() + " " + pos.getZ() + " (" + dimension + ")";
        }
    }

    private final Map<ResourceLocation, Claim> claims = new HashMap<>();

    public static DysonStars get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DysonStars::load, DysonStars::new, NAME);
    }

    //////////////////////////////////////
    // ************* Star ************//
    //////////////////////////////////////

    /** The star a dimension circles, or null for a dimension with none. */
    public static ResourceLocation starOf(Level level) {
        if (level == null) return SUN;
        if (NO_STAR.contains(level.dimension().location().toString())) return null;
        if (ModList.get().isLoaded("ad_astra")) {
            ResourceLocation system = AdAstra.system(level);
            if (system != null) return system;
        }
        // a dimension no planet claims (the End, other mods' worlds) sees the Sun
        return SUN;
    }

    /** Kept apart so Ad Astra's classes are only touched when the mod is there. */
    private static final class AdAstra {

        static ResourceLocation system(Level level) {
            var planet = earth.terrarium.adastra.api.planets.PlanetApi.API.getPlanet(level);
            return planet == null ? null : planet.solarSystem();
        }
    }

    /** "the Sun", "Alpha Centauri", another system by its id. */
    public static Component starName(ResourceLocation star) {
        String fallback = star.equals(SUN) ? "the Sun" : star.equals(ALPHA_CENTAURI) ? "Alpha Centauri" : pretty(star.getPath());
        return Component.translatableWithFallback("af9.dyson_swarm.star." + star.getNamespace() + "." + star.getPath(),
                fallback);
    }

    private static String pretty(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }

    //////////////////////////////////////
    // *********** Claims ************//
    //////////////////////////////////////

    /** Who holds the star (null: nobody). */
    public Claim holder(ResourceLocation star) {
        return claims.get(star);
    }

    /**
     * Takes the star for the swarm at the position, unless another one holds it. A holder whose controller is gone (or
     * whose structure is broken) lets go; one in an unloaded chunk cannot be told, it keeps the star until it is looked
     * at again, so a restart never hands the star to the other swarm.
     *
     * @return whether the swarm holds the star now
     */
    public boolean claim(MinecraftServer server, ResourceLocation star, ResourceLocation dimension, BlockPos pos) {
        Claim held = claims.get(star);
        if (held != null && held.dimension().equals(dimension) && held.pos().equals(pos)) return true;
        if (held != null && isLive(server, held)) return false;
        claims.put(star, new Claim(dimension, pos.immutable()));
        setDirty();
        return true;
    }

    /** The swarm at the position lets go of the star, if it holds it. */
    public void release(ResourceLocation star, ResourceLocation dimension, BlockPos pos) {
        Claim held = claims.get(star);
        if (held != null && held.dimension().equals(dimension) && held.pos().equals(pos)) {
            claims.remove(star);
            setDirty();
        }
    }

    private static boolean isLive(MinecraftServer server, Claim claim) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, claim.dimension()));
        if (level == null) return false;
        if (!level.isLoaded(claim.pos())) return true;
        return MetaMachine.getMachine(level, claim.pos()) instanceof DysonSwarmMachine swarm && swarm.isFormed();
    }

    //////////////////////////////////////
    // ************ Save *************//
    //////////////////////////////////////

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        claims.forEach((star, claim) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("star", star.toString());
            entry.putString("dimension", claim.dimension().toString());
            entry.putLong("pos", claim.pos().asLong());
            list.add(entry);
        });
        tag.put("claims", list);
        return tag;
    }

    static DysonStars load(CompoundTag tag) {
        DysonStars data = new DysonStars();
        for (Tag raw : tag.getList("claims", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation star = ResourceLocation.tryParse(entry.getString("star"));
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (star == null || dimension == null) continue;
            data.claims.put(star, new Claim(dimension, BlockPos.of(entry.getLong("pos"))));
        }
        return data;
    }
}
