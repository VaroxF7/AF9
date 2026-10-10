package com.af9.core.droppod.client;

import com.af9.core.AF9Core;
import com.af9.core.droppod.DropPodEntity;
import com.af9.core.network.AF9Network;
import com.af9.core.network.DropPodReleasePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The wait in the sky: a player who rides a hanging pod sees the title and the line "press SPACE" once the loading screen
 * is gone (so they do see it), and the pod is let go by SPACE, or by itself a few seconds after the screen cleared.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DropPodClientEvents {

    /** Ticks after the loading screen is gone at which the pod launches by itself (6 s). */
    private static final int AUTO_TICKS = 120;
    private static final int SENT = Integer.MIN_VALUE / 2;
    /** Ticks the screen has been clear while the pod hangs; a large negative once the release was sent. */
    private static int ready;

    private DropPodClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getVehicle() instanceof DropPodEntity pod)
                || pod.isReleased()) {
            ready = 0;
            return;
        }
        // a loading screen or any menu: not seen yet
        if (mc.screen != null) {
            ready = ready < 0 ? ready : 0;
            return;
        }
        if (ready < 0) return;
        ready++;
        if (ready == 1) {
            mc.gui.setTimes(10, 100, 30);
            mc.gui.setTitle(Component.translatable("af9.drop_pod.title"));
            mc.gui.setSubtitle(Component.translatable("af9.drop_pod.subtitle"));
        }
        int seconds = (AUTO_TICKS - ready + 19) / 20;
        mc.gui.setOverlayMessage(Component.translatable("af9.drop_pod.hold", Math.max(0, seconds)), false);
        if (mc.options.keyJump.isDown() || ready >= AUTO_TICKS) {
            AF9Network.CHANNEL.sendToServer(new DropPodReleasePacket());
            ready = SENT;
        }
    }
}
