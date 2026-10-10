package com.af9.core.droppod.client;

import com.af9.core.droppod.DropPodEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The engine of the pod while its thrusters burn: Ad Astra's rocket sound, played the way its rocket plays it (a looping
 * ambient sound at volume 10 that follows the vehicle), and it stops when the thrusters stop.
 */
public final class DropPodSound extends AbstractTickableSoundInstance {

    private final DropPodEntity pod;

    private DropPodSound(DropPodEntity pod, SoundEvent event) {
        super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.pod = pod;
        this.looping = true;
        this.volume = 10.0F;
        this.x = pod.getX();
        this.y = pod.getY();
        this.z = pod.getZ();
    }

    /** Called by the pod on the client when its thrusters start. */
    public static void start(DropPodEntity pod) {
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("ad_astra", "rocket"));
        if (event == null) event = SoundEvents.FIREWORK_ROCKET_BLAST;
        Minecraft.getInstance().getSoundManager().play(new DropPodSound(pod, event));
    }

    @Override
    public void tick() {
        if (pod.isRemoved() || !pod.isExhausting()) {
            stop();
            return;
        }
        x = pod.getX();
        y = pod.getY();
        z = pod.getZ();
    }
}
