package com.af9.core.network;

import com.af9.core.client.RingDeathOverlay;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the receiving player was just killed by an orbital station's light ring. No data. */
public record RingDeathPacket() {

    public void encode(FriendlyByteBuf buffer) {}

    public static RingDeathPacket decode(FriendlyByteBuf buffer) {
        return new RingDeathPacket();
    }

    /** Main thread (consumerMainThread); the overlay class only loads on the client. */
    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RingDeathOverlay::trigger);
    }
}
