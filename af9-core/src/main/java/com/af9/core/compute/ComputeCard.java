package com.af9.core.compute;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The cards of the computation arrays' racks: processors (CPU, GPU) make computation and heat; RAM feeds them. Every
 * number of the computers is here. Items: {@link AF9Compute}; recipes in KubeJS
 * ({@code server_scripts/mods/gtceu/computation.js}); spec: docs/machine-bus.md §9.
 * <p>
 * A processor needs a RAM card of its tier or higher in the same rack to run at full speed; each RAM card feeds one
 * processor. A processor without RAM runs at a quarter of its computation (and still takes its full energy and heat).
 */
public enum ComputeCard {

    // tier, kind, CWU/t (full speed), heat per tick, EU/t. The draw is a share of an amp of the card's voltage
    // (MV, HV, IV, LuV, UV): a CPU 1/16, a GPU 1/8, RAM 1/32.
    TUBE_CPU(1, Kind.CPU, 1, 1, 8),
    TUBE_GPU(1, Kind.GPU, 2, 3, 16),
    TUBE_RAM(1, Kind.RAM, 0, 1, 4),
    SILICON_CPU(2, Kind.CPU, 2, 2, 32),
    SILICON_GPU(2, Kind.GPU, 4, 5, 64),
    SILICON_RAM(2, Kind.RAM, 0, 1, 16),
    NANO_CPU(3, Kind.CPU, 4, 3, 512),
    NANO_GPU(3, Kind.GPU, 8, 8, 1024),
    NANO_RAM(3, Kind.RAM, 0, 2, 256),
    QUANTUM_CPU(4, Kind.CPU, 8, 5, 2048),
    QUANTUM_GPU(4, Kind.GPU, 16, 12, 4096),
    QUANTUM_RAM(4, Kind.RAM, 0, 3, 1024),
    TENSOR_CPU(5, Kind.CPU, 16, 8, 32768),
    TENSOR_GPU(5, Kind.GPU, 32, 18, 65536),
    TENSOR_RAM(5, Kind.RAM, 0, 4, 16384);

    public enum Kind {
        CPU, GPU, RAM;

        public boolean isProcessor() {
            return this != RAM;
        }
    }

    /** Card tiers: Tube (MV, before the lithography line), Silicon (HV), Nano (IV), Quantum (LuV), Tensor (UV). */
    public static final String[] TIER_NAMES = { "", "tube", "silicon", "nano", "quantum", "tensor" };
    public static final int MAX_TIER = 5;

    public final int tier;
    public final Kind kind;
    /** Computation at full speed (fed by RAM), CWU/t. */
    public final int cwut;
    public final int heat;
    public final long eut;

    ComputeCard(int tier, Kind kind, int cwut, int heat, long eut) {
        this.tier = tier;
        this.kind = kind;
        this.cwut = cwut;
        this.heat = heat;
        this.eut = eut;
    }

    /** Item id (af9:&lt;tier&gt;_&lt;kind&gt;_card), e.g. af9:tube_cpu_card. */
    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_card";
    }

    public static ComputeCard of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }

    public static ComputeCard of(Item item) {
        return item instanceof ComputeCardItem card ? card.card : null;
    }

    /**
     * What a rack's cards add up to: RAM goes to the highest processors first, each to the lowest RAM that still
     * feeds it. Computation in quarter CWU/t (a processor without RAM gives a quarter).
     */
    public record Load(int quarterCwut, int heat, long eut, int processors, int unfed) {

        public static final Load EMPTY = new Load(0, 0, 0, 0, 0);

        public Load plus(Load other) {
            return new Load(quarterCwut + other.quarterCwut, heat + other.heat, eut + other.eut,
                    processors + other.processors, unfed + other.unfed);
        }
    }

    public static Load evaluate(List<ComputeCard> cards) {
        List<ComputeCard> processors = new ArrayList<>();
        List<ComputeCard> ram = new ArrayList<>();
        int heat = 0;
        long eut = 0;
        for (ComputeCard card : cards) {
            if (card == null) continue;
            heat += card.heat;
            eut += card.eut;
            (card.kind.isProcessor() ? processors : ram).add(card);
        }
        processors.sort(Comparator.comparingInt((ComputeCard c) -> c.tier).reversed());
        ram.sort(Comparator.comparingInt(c -> c.tier));
        int quarters = 0;
        int unfed = 0;
        for (ComputeCard processor : processors) {
            ComputeCard feeder = null;
            for (ComputeCard memory : ram) {
                if (memory.tier >= processor.tier) {
                    feeder = memory;
                    break;
                }
            }
            if (feeder != null) {
                ram.remove(feeder);
                quarters += processor.cwut * 4;
            } else {
                quarters += processor.cwut;
                unfed++;
            }
        }
        return new Load(quarters, heat, eut, processors.size(), unfed);
    }
}
