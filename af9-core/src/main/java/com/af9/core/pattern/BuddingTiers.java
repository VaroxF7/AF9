package com.af9.core.pattern;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The grades of AE2's budding quartz, as the ME Resonance Fabricator reads them (TFG's {@code TFGPredicates}, LGPL-3.0,
 * see CREDITS-textures.md): a Certus Quartz Block is grade 0, then Chipped (AE2's damaged), Flawed (chipped), Flawless
 * (flawed) and Exquisite (flawless) Budding Certus, 1 to 4. TFG names AE2's blocks in code; here they are looked up by
 * id, so AF9 Core needs no AE2 at compile time and the predicate matches nothing where AE2 is absent.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class BuddingTiers {

    public static final int MAX_TIER = 4;

    private static final String[] IDS = { "quartz_block", "damaged_budding_quartz", "chipped_budding_quartz",
            "flawed_budding_quartz", "flawless_budding_quartz" };

    private static Map<Block, Integer> tiers;

    private BuddingTiers() {}

    private static Map<Block, Integer> tiers() {
        if (tiers == null) {
            Map<Block, Integer> map = new LinkedHashMap<>();
            for (int tier = 0; tier < IDS.length; tier++) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ae2", IDS[tier]));
                if (block != null && block != Blocks.AIR) map.put(block, tier);
            }
            tiers = map;
        }
        return tiers;
    }

    /** The grade of a block, or -1 if it is none of the budding blocks. */
    public static int tierOf(Block block) {
        Integer tier = tiers().get(block);
        return tier != null ? tier : -1;
    }

    /** The block of a grade (a Certus Quartz Block for an unknown one). */
    public static Block blockOf(int tier) {
        for (Map.Entry<Block, Integer> entry : tiers().entrySet()) {
            if (entry.getValue() == tier) return entry.getKey();
        }
        Block fallback = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ae2", IDS[0]));
        return fallback != null ? fallback : Blocks.AIR;
    }

    /** The blocks that can sit in the Fabricator's middle, grade 0 to 4, for the recipe page. */
    public static List<Block> all() {
        return new ArrayList<>(tiers().keySet());
    }

    /**
     * Any of the budding blocks. The machine reads the grade and the position from the match context
     * ({@code BuddingTier}, {@code BuddingPos}).
     */
    public static TraceabilityPredicate predicate() {
        return new TraceabilityPredicate(state -> {
            Integer tier = tiers().get(state.getBlockState().getBlock());
            if (tier == null) return false;
            state.getMatchContext().set("BuddingTier", tier);
            state.getMatchContext().set("BuddingPos", state.getPos());
            return true;
        }, () -> tiers().keySet().stream()
                .map(block -> BlockInfo.fromBlockState(block.defaultBlockState()))
                .toArray(BlockInfo[]::new));
    }
}
