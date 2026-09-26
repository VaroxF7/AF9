package com.af9.core.pattern;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cleanroom filters of the AF9 machines.
 * <ul>
 * <li>The Plascrete Filter Casing ({@code kubejs:plascrete_filter_casing}, an MV block from
 * kubejs/startup_scripts/gtceu/photolithography.js) is a GT cleanroom filter like GT's Filter Casing (ISO 5): it
 * works in GT's Cleanroom and in every filter roof.</li>
 * <li>{@link #cleanroomFilters()} is GT's filter predicate with a fixed order: the structure preview and the
 * terminal's auto-build take the first candidate, which is the MV Plascrete Filter Casing, then GT's Filter Casing,
 * the sterile ones last (GT's own order is a hash map's).</li>
 * </ul>
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Filters {

    public static final ResourceLocation PLASCRETE_FILTER_ID = new ResourceLocation("kubejs",
            "plascrete_filter_casing");

    private AF9Filters() {}

    public enum FilterType implements IFilterType {

        PLASCRETE("plascrete_filter_casing", CleanroomType.CLEANROOM);

        private final String name;
        private final CleanroomType cleanroomType;

        FilterType(String name, CleanroomType cleanroomType) {
            this.name = name;
            this.cleanroomType = cleanroomType;
        }

        @Override
        public CleanroomType getCleanroomType() {
            return cleanroomType;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Common setup, after KubeJS registered the block. */
    public static void register() {
        if (!ForgeRegistries.BLOCKS.containsKey(PLASCRETE_FILTER_ID)) {
            AF9Core.LOGGER.warn("Block {} not found - is the AF9 KubeJS startup script loaded?", PLASCRETE_FILTER_ID);
            return;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(PLASCRETE_FILTER_ID);
        GTCEuAPI.CLEANROOM_FILTERS.put(FilterType.PLASCRETE, () -> block);
    }

    /** Any registered cleanroom filter, all of one type (like GT's); candidates in the order described above. */
    public static TraceabilityPredicate cleanroomFilters() {
        return new TraceabilityPredicate(state -> {
            BlockState blockState = state.getBlockState();
            for (Map.Entry<IFilterType, Supplier<Block>> entry : GTCEuAPI.CLEANROOM_FILTERS.entrySet()) {
                if (!blockState.is(entry.getValue().get())) continue;
                IFilterType type = entry.getKey();
                if (!state.getMatchContext().getOrPut("FilterType", type).equals(type)) {
                    state.setError(new PatternStringError("gtceu.multiblock.pattern.error.filters"));
                    return false;
                }
                return true;
            }
            return false;
        }, () -> ordered().stream()
                .map(entry -> BlockInfo.fromBlockState(entry.getValue().get().defaultBlockState()))
                .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("gtceu.multiblock.pattern.error.filters"));
    }

    private static List<Map.Entry<IFilterType, Supplier<Block>>> ordered() {
        return GTCEuAPI.CLEANROOM_FILTERS.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<IFilterType, Supplier<Block>> entry) -> rank(entry.getKey()))
                        .thenComparing(entry -> entry.getKey().getSerializedName()))
                .toList();
    }

    private static int rank(IFilterType type) {
        if (type == FilterType.PLASCRETE) return 0;
        return type.getCleanroomType() == CleanroomType.CLEANROOM ? 1 : 2;
    }
}
