package com.af9.core.pattern;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds out why a pattern does not form, which GT's own error cannot say: when a cell does not match, GT shifts the whole pattern
 * back by one aisle and tries again without resetting its counts of limited parts (hatches, mufflers), so the error left at the
 * end is that of a shifted try, often "Maximum: 3" from counts that ran over. This walks the pattern once, in place, with fresh
 * counts, and reports the first cells that do not match (their place, what stands there, what the cell wants).
 */
public final class PatternProbe {
    private static Field blockMatches;
    private static Field centerOffset;
    private static Method offset;
    private static boolean broken;

    private PatternProbe() {}

    /** Up to {@code limit} lines about the first cells of the pattern that do not match; empty if all match (or not readable). */
    public static List<String> mismatches(IMultiController controller, Level level, int limit) {
        List<String> out = new ArrayList<>();
        if (broken) return out;
        try {
            BlockPattern pattern = controller.getPattern();
            if (blockMatches == null) {
                blockMatches = BlockPattern.class.getDeclaredField("blockMatches");
                blockMatches.setAccessible(true);
                centerOffset = BlockPattern.class.getDeclaredField("centerOffset");
                centerOffset.setAccessible(true);
                offset = BlockPattern.class.getDeclaredMethod("setActualRelativeOffset", int.class, int.class, int.class,
                        Direction.class, Direction.class, boolean.class);
                offset.setAccessible(true);
            }
            TraceabilityPredicate[][][] cells = (TraceabilityPredicate[][][]) blockMatches.get(pattern);
            int[] center = (int[]) centerOffset.get(pattern);
            BlockPos origin = controller.self().getPos();
            Direction facing = controller.self().getFrontFacing();
            Direction up = controller.self().getUpwardsFacing();
            boolean flipped = controller.self().isFlipped();
            MultiblockState state = new MultiblockState(level, origin);
            int z = -center[4];
            for (int c = 0; c < cells.length; c++, z++) {
                for (int b = 0; b < cells[c].length; b++) {
                    for (int a = 0; a < cells[c][b].length; a++) {
                        TraceabilityPredicate predicate = cells[c][b][a];
                        BlockPos pos = ((BlockPos) offset.invoke(pattern, a - center[0], b - center[1], z, facing, up, flipped))
                                .offset(origin.getX(), origin.getY(), origin.getZ());
                        state.update(pos, predicate);
                        if (predicate.test(state)) continue;
                        Block found = level.getBlockState(pos).getBlock();
                        var key = ForgeRegistries.BLOCKS.getKey(found);
                        out.add(pos.getX() + " " + pos.getY() + " " + pos.getZ() + ": has " + key + ", wants "
                                + wants(predicate) + (state.error == null ? "" : " [" + state.error.getErrorInfo().getString() + "]"));
                        if (out.size() >= limit) return out;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            broken = true;
            AF9Core.LOGGER.warn("Pattern probe failed: {}", e.toString());
        }
        return out;
    }

    private static String wants(TraceabilityPredicate predicate) {
        Set<String> names = new LinkedHashSet<>();
        List<SimplePredicate> all = new ArrayList<>(predicate.common);
        all.addAll(predicate.limited);
        for (SimplePredicate simple : all) {
            if (simple.candidates == null) {
                names.add("(any)");
                continue;
            }
            BlockInfo[] infos = simple.candidates.get();
            int n = 0;
            for (BlockInfo info : infos) {
                if (info == null || n++ >= 2) break;
                var key = ForgeRegistries.BLOCKS.getKey(info.getBlockState().getBlock());
                if (key != null) names.add(key.getPath() + (simple.maxCount != -1 ? " (max " + simple.maxCount + ")" : ""));
            }
            if (infos.length > 2) names.add("...");
        }
        return String.join(" | ", names);
    }
}
