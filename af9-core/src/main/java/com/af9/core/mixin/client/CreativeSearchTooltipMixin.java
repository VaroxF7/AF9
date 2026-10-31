package com.af9.core.mixin.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The creative inventory's first search builds a search tree over every creative stack (about 98,800 in the pack) on
 * the render thread, and nearly all of the 22 s it takes is the full tooltip of each stack. Vanilla builds that tree by
 * asking for the tooltip with no player and the creative flag; nothing else does, so that call alone gets just the
 * item's name (its registry id is indexed separately). Searching by tooltip text in the creative screen is gone; EMI's
 * search bar still does it.
 */
@Mixin(ItemStack.class)
public abstract class CreativeSearchTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true, require = 0)
    private void af9$cheapCreativeSearchTooltip(@Nullable Player player, TooltipFlag flag,
                                                CallbackInfoReturnable<List<Component>> cir) {
        if (player == null && flag != null && flag.isCreative()) {
            List<Component> lines = new ArrayList<>(1);
            lines.add(((ItemStack) (Object) this).getHoverName());
            cir.setReturnValue(lines);
        }
    }
}
