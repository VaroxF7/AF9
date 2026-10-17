package com.af9.core.quantum;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What goes in a Quantum Computer's rack hatches: a processor (computes and heats) or a heat vent (cools). The
 * numbers are TecTech's (GT5-Unofficial {@code MTEHatchRack.RackComponent}, LGPL-3.0): the circuits of the pack's
 * ladder take the places of GTNH's, and the vents (GTNH's IC2 reactor vents) are AF9's Rack Heat Vents.
 * <p>
 * A processor gives {@code computation} CWU/t (before the overclock maths of {@link RackPartMachine}) and makes
 * {@code heat} a second; a vent takes {@code 10} heat a second and, the hotter the rack, the more ({@code cool}).
 * A component in a rack hotter than its {@code maxHeat} is destroyed.
 *
 * @param item        registry id of the item
 * @param computation CWU/t of a processor, 0 for a vent
 * @param heat        heat per second of a processor, 0 for a vent
 * @param cool        a vent's cooling constant (heat taken per second is {@code 10 * (1 + cool * rackHeat / 100000)}), 0 for a processor
 * @param maxHeat     rack heat above which the component burns out
 */
public record RackComponent(ResourceLocation item, int computation, int heat, float cool, int maxHeat) {

    private static final Map<ResourceLocation, RackComponent> COMPONENTS = new HashMap<>();

    static {
        // Crystal circuits (GTNH: IV-UV crystal circuits, 60-120 CWU, hot)
        processor("gtceu:crystal_processor", 60, 56, 2000);
        processor("gtceu:crystal_processor_assembly", 80, 54, 2000);
        processor("gtceu:crystal_processor_computer", 100, 52, 2000);
        processor("gtceu:crystal_processor_mainframe", 120, 50, 2000);
        // Wetware circuits (GTNH: 160-220 CWU)
        processor("gtceu:wetware_processor", 160, 46, 4000);
        processor("gtceu:wetware_processor_assembly", 180, 44, 4000);
        processor("gtceu:wetware_processor_computer", 200, 42, 4000);
        processor("gtceu:wetware_processor_mainframe", 220, 40, 4000);
        // The pack's two circuit tiers past UHV take the places of GTNH's bio / optical / piko / quantum / Planck circuits
        processor("af9:xps_processor", 260, 26, 8000);
        processor("af9:xps_processor_mainframe", 300, 22, 8000);
        processor("af9:nvm_processor", 340, 14, 10000);
        processor("af9:nvm_processor_mainframe", 400, 10, 10000);
        // Vents (GTNH: reactor heat vent 40, reactor vent 80, overclocked 120, advanced 160)
        vent("af9:rack_heat_vent_mk1", 40, 2000);
        vent("af9:rack_heat_vent_mk2", 80, 4000);
        vent("af9:rack_heat_vent_mk3", 120, 6000);
        vent("af9:rack_heat_vent_mk4", 160, 8000);
    }

    @SuppressWarnings("removal") // new ResourceLocation(String) is the only constructor on 1.20.1
    private static void processor(String id, int computation, int heat, int maxHeat) {
        ResourceLocation key = new ResourceLocation(id);
        COMPONENTS.put(key, new RackComponent(key, computation, heat, 0F, maxHeat));
    }

    @SuppressWarnings("removal")
    private static void vent(String id, float cool, int maxHeat) {
        ResourceLocation key = new ResourceLocation(id);
        COMPONENTS.put(key, new RackComponent(key, 0, 0, cool, maxHeat));
    }

    /** The component an item stack is, or null. */
    public static RackComponent of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : COMPONENTS.get(id);
    }

    public static boolean isComponent(ItemStack stack) {
        return of(stack) != null;
    }

    public boolean isVent() {
        return cool > 0;
    }

    /** Tooltip lines of a component item (client). */
    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        RackComponent component = of(stack);
        if (component == null) return;
        int at = Math.min(1, tooltip.size());
        if (component.isVent()) {
            tooltip.add(at, Component.translatable("af9.quantum.component.vent", String.format("%.0f", component.cool),
                    component.maxHeat).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(at, Component.translatable("af9.quantum.component.processor", component.computation,
                    component.heat, component.maxHeat).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(at, Component.translatable("af9.quantum.component.title").withStyle(ChatFormatting.GRAY));
    }
}
