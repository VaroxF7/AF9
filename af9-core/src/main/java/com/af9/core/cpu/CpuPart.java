package com.af9.core.cpu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What goes into the racks of a Crafting CPU Array ({@link CraftingCpuMachine}): memory that gives the ME crafting CPU
 * its bytes, processors that give it co-processors (the parallel crafts). The items are KubeJS's
 * ({@code startup_scripts/gtceu/crafting_cpu.js}).
 *
 * @param id      registry path in {@code kubejs:}
 * @param bytes   crafting storage one gives (AE2's 256k cell is 262,144 bytes)
 * @param threads co-processors one gives (AE2's 16x unit is 16)
 * @param eut     what one draws from the array, EU/t
 */
public enum CpuPart {
    /** 8x the RAM card of the Silicon rack cards. */
    HBM_STICK("hbm_memory_stick", 4L << 20, 0, 8),
    /** 8x the eDRAM card of the Nano rack cards. */
    HBM_STACK("hbm_memory_stack", 32L << 20, 0, 64),
    /** Four CPUs on one board. */
    CPU_CLUSTER("cpu_cluster", 0, 16, 64),
    /** Eight quantum CPUs, superposed: more than four times a cluster. */
    SUPERPOSITIONED_CLUSTER("superpositioned_cpu_cluster", 0, 64, 512);

    public final String id;
    public final long bytes;
    public final int threads;
    public final long eut;

    CpuPart(String id, long bytes, int threads, long eut) {
        this.id = id;
        this.bytes = bytes;
        this.threads = threads;
        this.eut = eut;
    }

    public boolean isMemory() {
        return bytes > 0;
    }

    /** The part an item is, or null. */
    @SuppressWarnings("removal") // ResourceLocation.getNamespace / getPath are fine; only the constructor is flagged
    public static CpuPart of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("kubejs")) return null;
        for (CpuPart part : values()) {
            if (part.id.equals(key.getPath())) return part;
        }
        return null;
    }

    /** Bytes as AE2 writes them: 4 MiB, 1.5 GiB. */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = { "KiB", "MiB", "GiB", "TiB" };
        double value = bytes;
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return (value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f",
                value)) + " " + units[unit];
    }
}
