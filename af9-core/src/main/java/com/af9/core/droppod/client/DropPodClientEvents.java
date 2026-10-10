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
 * The wait in the sky. Once the loading screen is gone the client tells the server (the server starts a 5 s countdown and
 * the pod launches by itself at its end), and the player sees the title and the line with the server's countdown; SPACE
 * launches at once.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DropPodClientEvents {

    private static boolean readySent;
    private static boolean launchSent;

    private DropPodClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getVehicle() instanceof DropPodEntity pod)
                || pod.isReleased()) {
            readySent = false;
            launchSent = false;
            return;
        }
        // a loading screen or any menu: the player has not seen the pod yet
        if (mc.screen != null) return;
        if (!readySent) {
            readySent = true;
            AF9Network.CHANNEL.sendToServer(new DropPodReleasePacket(false));
            mc.gui.setTimes(10, 100, 30);
            mc.gui.setTitle(Component.translatable("af9.drop_pod.title"));
            mc.gui.setSubtitle(Component.translatable("af9.drop_pod.subtitle"));
        }
        int left = pod.countdownTicks();
        int seconds = left < 0 ? (DropPodEntity.AUTO_TICKS + 19) / 20 : (left + 19) / 20;
        mc.gui.setOverlayMessage(Component.translatable("af9.drop_pod.hold", Math.max(1, seconds)), false);
        if (!launchSent && mc.options.keyJump.isDown()) {
            launchSent = true;
            AF9Network.CHANNEL.sendToServer(new DropPodReleasePacket(true));
        }
    }
}
