package com.af9.core.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * An item that is a name, a texture and a few lines of tooltip: the lines are {@code <item's key>.tooltip.0}, {@code .1}
 * ... in the lang file, plain as written there (a line colours itself with formatting codes if it wants to).
 */
public class TooltipItem extends Item {

    private final int lines;

    public TooltipItem(Properties properties, int lines) {
        super(properties);
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        addLines(getDescriptionId(), lines, tooltip);
    }

    /** The tooltip lines of a translation key, as many as it has. */
    static void addLines(String key, int lines, List<Component> tooltip) {
        for (int i = 0; i < lines; i++) tooltip.add(Component.translatable(key + ".tooltip." + i));
    }
}
