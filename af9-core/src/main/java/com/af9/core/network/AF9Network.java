package com.af9.core.network;

import com.af9.core.AF9Core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * AF9 Core's network channel: server to client only, so far the orbital ring's death screen
 * ({@link RingDeathPacket}).
 */
public final class AF9Network {

    private static final String VERSION = "1";
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AF9Core.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private AF9Network() {}

    /** Common setup. */
    public static void register() {
        CHANNEL.messageBuilder(RingDeathPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RingDeathPacket::encode)
                .decoder(RingDeathPacket::decode)
                .consumerMainThread(RingDeathPacket::handle)
                .add();
    }

    /** The player was killed by an orbital station's light ring: show its death screen. */
    public static void sendRingDeath(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RingDeathPacket());
    }
}
