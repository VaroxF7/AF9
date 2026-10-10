package com.af9.core.network;

import com.af9.core.droppod.DropPodEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: the player in a hanging drop pod is ready (SPACE pressed, or the loading screen is long gone). */
public final class DropPodReleasePacket {

    public DropPodReleasePacket() {}

    public static void encode(DropPodReleasePacket packet, FriendlyByteBuf buffer) {}

    public static DropPodReleasePacket decode(FriendlyByteBuf buffer) {
        return new DropPodReleasePacket();
    }

    public static void handle(DropPodReleasePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        if (player != null && player.getVehicle() instanceof DropPodEntity pod && !pod.isReleased()) pod.release();
        ctx.setPacketHandled(true);
    }
}
