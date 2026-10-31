package com.af9.core.mixin.client;

import java.util.List;

import com.af9.core.client.CreativeSearchCache;

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
 * Vanilla builds the creative search tree by asking every stack for its tooltip with no player and the creative flag;
 * nothing else does. CreativeSearchCache has worked those tooltips out a few at a time since the world was joined, so
 * that call is answered from there and the first search no longer freezes for 22 s. A stack not cached is computed
 * as usual.
 */
@Mixin(ItemStack.class)
public abstract class CreativeSearchTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true, require = 0)
    private void af9$cheapCreativeSearchTooltip(@Nullable Player player, TooltipFlag flag,
                                                CallbackInfoReturnable<List<Component>> cir) {
        if (player == null && flag != null && flag.isCreative() && !CreativeSearchCache.isComputing()) {
            List<Component> cached = CreativeSearchCache.lookup((ItemStack) (Object) this);
            if (cached != null) {
                cir.setReturnValue(cached);
            }
        }
    }
}
