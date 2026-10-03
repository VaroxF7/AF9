package com.af9.core.compat.emi;

import com.af9.core.AF9Core;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeSet;

/**
 * EMI Accelerator (a mod that keeps EMI's list of items and fluids from one launch to the next) knows its cache by the
 * mods' ids and versions alone. The pack's items come from KubeJS scripts and from AF9 Core, and neither changes a mod
 * version: after an update the cache of the launch before went on being used, and EMI showed none of the new items
 * (the Space Elevator's blocks and Mining Drones, the platinum line's dusts: their recipes were there, the items were
 * not in the list or the search).
 * <p>
 * So the cache is cleared whenever the items and fluids that are registered are not the ones it was made with: a hash of
 * their names is kept beside it. EMI then builds its list anew at the next world, once, and the mod caches that. Does
 * nothing without EMI Accelerator.
 */
public final class EmiAcceleratorCompat {

    public static final String MOD_ID = "emi_accelerator";
    /** EMI Accelerator's folder in the config directory, its cache in it, and the hash kept beside it. */
    private static final String FOLDER = "emi-accelerator", CACHE = "stack-cache.emic", MARK = "af9-registry.sha256";

    private EmiAcceleratorCompat() {}

    /** Client setup: the registries are complete, and EMI loads its list only when a world is joined. */
    public static void checkCache() {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        Path folder = FMLPaths.CONFIGDIR.get().resolve(FOLDER);
        Path mark = folder.resolve(MARK);
        try {
            String now = registryHash();
            String before = Files.exists(mark) ? Files.readString(mark, StandardCharsets.UTF_8).trim() : "";
            if (now.equals(before)) return;
            boolean cleared = Files.deleteIfExists(folder.resolve(CACHE));
            Files.createDirectories(folder);
            Files.writeString(mark, now, StandardCharsets.UTF_8);
            AF9Core.LOGGER.info("EMI Accelerator: the registered items or fluids changed since its stack cache was made" +
                    (cleared ? ", the cache is cleared (EMI builds its list anew at the next world)" :
                            " (there was no cache)"));
        } catch (IOException | NoSuchAlgorithmException | RuntimeException exception) {
            AF9Core.LOGGER.warn("EMI Accelerator: could not check its stack cache against the registered items",
                    exception);
        }
    }

    /** SHA-256 over the names of every registered item and fluid, in order. */
    private static String registryHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (ResourceLocation name : new TreeSet<>(ForgeRegistries.ITEMS.getKeys())) {
            digest.update(name.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        digest.update((byte) '|');
        for (ResourceLocation name : new TreeSet<>(ForgeRegistries.FLUIDS.getKeys())) {
            digest.update(name.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
