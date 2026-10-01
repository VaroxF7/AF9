package com.af9.core.cpu;

import com.af9.core.ae2.CpuCoreBlockEntity;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Crafting CPU Array: AE2's autocrafting CPU as a GregTech multiblock. Its racks ({@link CpuRackPartMachine}) hold HBM
 * Memory Sticks and Stacks, which give the CPU its bytes, and CPU Clusters, which give it co-processors
 * ({@link CpuPart}). In the structure sits one Crafting CPU Core block (an AE2 crafting unit, {@code af9:crafting_cpu_core}):
 * it is the part of the array on the ME network (cable against any of its faces), and the CPU the network sees is
 * the array's: this controller sets the bytes and the co-processors of the core's cluster every second
 * ({@link CpuCoreBlockEntity#apply}).
 * <p>
 * No recipes: switched on and formed, the array draws its energy every tick (its own and its parts'). Without energy,
 * switched off or unformed, the CPU has 0 bytes and no co-processors: nothing is crafted on it, and a running job
 * waits. Structure in KubeJS ({@code startup_scripts/gtceu/crafting_cpu.js}). Spec: docs/crafting-cpu.md
 */
public class CraftingCpuMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CraftingCpuMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** The array's own draw. */
    public static final long BASE_EUT = 256;
    /** Co-processors an array gives at most: AE2 runs one craft per co-processor each tick, on the server thread. */
    public static final int MAX_THREADS = 1024;

    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    private static final ResourceLocation CORE = new ResourceLocation("af9", "crafting_cpu_core");

    // what the racks add up to (recomputed every second and when a rack changes)
    private final Map<CpuPart, Integer> counts = new EnumMap<>(CpuPart.class);
    private int racks;
    private long bytes;
    private int threads;
    private long eut;
    private boolean dirty = true;
    private boolean powered;
    /** The core block in the structure, and what it said to the last set: whether the CPU is on the network. */
    private BlockPos corePos;
    private boolean coreLinked;
    private boolean lastLinked;
    private long appliedBytes = -1;
    private int appliedThreads = -1;

    public CraftingCpuMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new CpuLogic(this);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        dirty = true;
        appliedBytes = -1;
    }

    @Override
    public void onStructureInvalid() {
        // the CPU goes off the structure's core before the structure's blocks are forgotten
        apply(0, 0);
        super.onStructureInvalid();
        powered = false;
        corePos = null;
        coreLinked = false;
    }

    /** A rack's parts changed. */
    public void onRacksChanged() {
        dirty = true;
    }

    private void evaluate() {
        counts.clear();
        int rackCount = 0;
        for (IMultiPart part : getParts()) {
            if (!(part instanceof CpuRackPartMachine rack)) continue;
            rackCount++;
            for (CpuPart kind : CpuPart.values()) counts.merge(kind, rack.count(kind), Integer::sum);
        }
        racks = rackCount;
        long totalBytes = 0;
        long totalThreads = 0;
        long totalEut = BASE_EUT;
        for (CpuPart kind : CpuPart.values()) {
            int count = counts.getOrDefault(kind, 0);
            totalBytes += count * kind.bytes;
            totalThreads += (long) count * kind.threads;
            totalEut += count * kind.eut;
        }
        bytes = totalBytes;
        threads = (int) Math.min(MAX_THREADS, totalThreads);
        eut = totalEut;
        corePos = findCore();
    }

    /** The Crafting CPU Core in the structure, or null. */
    private BlockPos findCore() {
        Level level = getLevel();
        if (level == null) return null;
        for (BlockPos pos : getMultiblockState().getCache()) {
            if (CORE.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()))) return pos;
        }
        return null;
    }

    /** One tick while switched on and formed. Returns whether the CPU is on. */
    boolean cpuTick() {
        Level level = getLevel();
        long now = level == null ? 0 : level.getGameTime();
        boolean second = now % 20 == 0;
        if (dirty || second) {
            evaluate();
            dirty = false;
        }
        powered = energyContainer != null && energyContainer.getEnergyStored() >= eut;
        if (powered) energyContainer.removeEnergy(eut);
        if (second || appliedBytes < 0) {
            if (powered) apply(bytes, threads);
            else apply(0, 0);
        }
        return powered;
    }

    /** Switched off: the CPU has nothing. */
    void idle() {
        powered = false;
        if (appliedBytes != 0 || appliedThreads != 0) apply(0, 0);
    }

    /** Gives the core's cluster its bytes and co-processors (when there is a core, and AE2). */
    private void apply(long newBytes, int newThreads) {
        Level level = getLevel();
        if (level == null || level.isClientSide || corePos == null || !ModList.get().isLoaded("ae2")) {
            coreLinked = false;
            return;
        }
        coreLinked = CpuCoreBlockEntity.apply(level, corePos, newBytes, newThreads);
        if (coreLinked) {
            appliedBytes = newBytes;
            appliedThreads = newThreads;
        }
        if (coreLinked != lastLinked) {
            lastLinked = coreLinked;
            markDirty();
        }
    }

    public long getBytes() {
        return bytes;
    }

    public int getThreads() {
        return threads;
    }

    public long getEUt() {
        return eut;
    }

    public int getRacks() {
        return racks;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> text) {
        if (!isFormed()) {
            text.add(Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED));
            return;
        }
        boolean on = getRecipeLogic().isWorkingEnabled();
        text.add(Component.translatable(!on ? "af9.cpu_array.off" : powered ? "af9.cpu_array.running" :
                "af9.cpu_array.stalled").withStyle(!on ? ChatFormatting.GRAY : powered ? ChatFormatting.GREEN :
                ChatFormatting.RED));
        text.add(Component.translatable("af9.cpu_array.racks", racks));
        text.add(Component.translatable("af9.cpu_array.bytes", CpuPart.formatBytes(bytes))
                .withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.cpu_array.threads", threads).withStyle(ChatFormatting.AQUA));
        if (threads == MAX_THREADS) {
            text.add(Component.translatable("af9.cpu_array.threads_capped", MAX_THREADS)
                    .withStyle(ChatFormatting.YELLOW));
        }
        for (CpuPart kind : CpuPart.values()) {
            int count = counts.getOrDefault(kind, 0);
            if (count > 0) {
                text.add(Component.translatable("af9.cpu_array.part", count,
                        Component.translatable("item.kubejs." + kind.id)).withStyle(ChatFormatting.GRAY));
            }
        }
        text.add(Component.translatable("af9.compute.array.eut", FormattingUtil.formatNumbers(eut),
                GTValues.VNF[GTUtil.getTierByVoltage(eut)]).withStyle(powered || !on ? ChatFormatting.GRAY :
                        ChatFormatting.RED));
        if (on && !powered) {
            text.add(Component.translatable("af9.compute.array.no_power").withStyle(ChatFormatting.RED));
        }
        if (!ModList.get().isLoaded("ae2")) {
            text.add(Component.translatable("af9.cpu_array.no_ae2").withStyle(ChatFormatting.RED));
        } else if (corePos == null) {
            text.add(Component.translatable("af9.cpu_array.no_core").withStyle(ChatFormatting.RED));
        } else {
            text.add(Component.translatable(coreLinked ? "af9.cpu_array.linked" : "af9.cpu_array.no_network")
                    .withStyle(coreLinked ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        for (IMultiPart part : getParts()) part.addMultiText(text);
    }

    /** Runs the array every tick instead of looking for recipes (as the computation arrays do). */
    public static class CpuLogic extends RecipeLogic {

        public CpuLogic(CraftingCpuMachine machine) {
            super(machine);
        }

        @Override
        public void serverTick() {
            CraftingCpuMachine array = (CraftingCpuMachine) machine;
            if (!array.isFormed() || !isWorkingEnabled()) {
                setStatus(Status.IDLE);
                isActive = false;
                array.idle();
                return;
            }
            boolean running = array.cpuTick();
            setStatus(running ? Status.WORKING : Status.WAITING);
            isActive = running;
        }
    }
}
