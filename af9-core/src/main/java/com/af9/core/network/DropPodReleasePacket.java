package com.af9.core.network;

import com.af9.core.droppod.DropPodEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server about the player's hanging drop pod: {@code now} false, the loading screen is gone and the player can
 * see the pod (the server starts the launch countdown); {@code now} true, SPACE was pressed (launch).
 */
public final class DropPodReleasePacket {

    private final boolean now;

    public DropPodReleasePacket(boolean now) {
        this.now = now;
    }

    public static void encode(DropPodReleasePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.now);
    }

    public static DropPodReleasePacket decode(FriendlyByteBuf buffer) {
        return new DropPodReleasePacket(buffer.readBoolean());
    }

    public static void handle(DropPodReleasePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        if (player != null && player.getVehicle() instanceof DropPodEntity pod && !pod.isReleased()) {
            if (packet.now) pod.release();
            else pod.startCountdown();
        }
        ctx.setPacketHandled(true);
    }
}
