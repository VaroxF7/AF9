package com.af9.core.client;

import com.af9.core.AF9Core;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Swaps the death screen of a player the orbital station's light ring burnt for {@link VaporizedScreen}. Started by
 * {@link com.af9.core.network.RingDeathPacket}, which the server sends right after the kill, so the vanilla death screen
 * is usually open already (swapped at once); if it is not yet, it is swapped as it opens. Ends when the player is
 * alive again.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RingDeathOverlay {

    /** DeathScreen's causeOfDeath (SRG name; the helper maps it in the dev environment). */
    private static final String CAUSE_OF_DEATH = "f_95907_";

    private static long startedAt = -1;

    private RingDeathOverlay() {}

    /** The ring killed this player (on the main thread). */
    public static void trigger() {
        startedAt = Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        var sounds = minecraft.getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.FIRECHARGE_USE, 0.6F, 0.8F));
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE, 0.5F, 0.35F));
        if (minecraft.screen instanceof DeathScreen death && !(death instanceof VaporizedScreen)) {
            minecraft.setScreen(vaporized(death));
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (startedAt < 0) return;
        if (event.getNewScreen() instanceof DeathScreen death && !(death instanceof VaporizedScreen)) {
            event.setNewScreen(vaporized(death));
        }
    }

    /** Ends once the player lives again (respawned, or a totem saved them), not before the death screen had a go. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || startedAt < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DeathScreen) return;
        boolean alive = minecraft.player != null && minecraft.player.isAlive();
        if ((alive && Util.getMillis() - startedAt > 1000) || Util.getMillis() - startedAt > 10000) startedAt = -1;
    }

    private static VaporizedScreen vaporized(DeathScreen death) {
        Component cause = null;
        try {
            cause = ObfuscationReflectionHelper.getPrivateValue(DeathScreen.class, death, CAUSE_OF_DEATH);
        } catch (RuntimeException e) {
            AF9Core.LOGGER.warn("Vaporized screen: cannot read the death message", e);
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean hardcore = minecraft.level != null && minecraft.level.getLevelData().isHardcore();
        return new VaporizedScreen(cause, hardcore);
    }
}
