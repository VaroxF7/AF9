package com.af9.core.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A block's item with tooltip lines of its own, as {@link TooltipItem}'s: {@code <block's key>.tooltip.<n>}. */
public class TooltipBlockItem extends BlockItem {

    private final int lines;

    public TooltipBlockItem(Block block, Properties properties, int lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addLines(getDescriptionId(), lines, tooltip);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
