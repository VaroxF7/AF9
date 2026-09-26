package com.af9.core.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The wireless energy channels, saved with the world (overworld data, "af9_wireless"). Every Wireless Energy
 * Transmitter owns one channel: it pushes the energy its multiblock gives it into the channel's buffer, and every
 * Wireless Energy Receiver linked to the channel draws from that buffer. The channel lives in the world's saved data,
 * so the two ends only need their own chunks loaded: any distance, any dimension.
 */
public final class WirelessChannels extends SavedData {

    private static final String NAME = "af9_wireless";

    private final Map<UUID, Channel> channels = new HashMap<>();

    /** One transmitter's channel: its buffer, voltage and amperage, and where the transmitter is. */
    public static final class Channel {

        public final UUID id;
        public long stored;
        public long capacity;
        public long voltage;
        public long amperage;
        public ResourceLocation dimension;
        public BlockPos pos = BlockPos.ZERO;

        Channel(UUID id) {
            this.id = id;
        }

        /** Takes up to the amount out of the buffer; returns what it took. */
        public long take(long amount) {
            long taken = Math.max(0, Math.min(amount, stored));
            stored -= taken;
            return taken;
        }

        /** Puts up to the amount into the buffer; returns what fitted. */
        public long put(long amount) {
            long room = Math.max(0, capacity - stored);
            long put = Math.max(0, Math.min(amount, room));
            stored += put;
            return put;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putLong("stored", stored);
            tag.putLong("capacity", capacity);
            tag.putLong("voltage", voltage);
            tag.putLong("amperage", amperage);
            if (dimension != null) tag.putString("dimension", dimension.toString());
            tag.putLong("pos", pos.asLong());
            return tag;
        }

        static Channel load(CompoundTag tag) {
            Channel channel = new Channel(tag.getUUID("id"));
            channel.stored = tag.getLong("stored");
            channel.capacity = tag.getLong("capacity");
            channel.voltage = tag.getLong("voltage");
            channel.amperage = tag.getLong("amperage");
            if (tag.contains("dimension")) channel.dimension = ResourceLocation.tryParse(tag.getString("dimension"));
            channel.pos = BlockPos.of(tag.getLong("pos"));
            return channel;
        }
    }

    public static WirelessChannels get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WirelessChannels::load, WirelessChannels::new,
                NAME);
    }

    public Channel get(UUID id) {
        return id == null ? null : channels.get(id);
    }

    public Channel getOrCreate(UUID id) {
        return channels.computeIfAbsent(id, key -> {
            setDirty();
            return new Channel(key);
        });
    }

    public void remove(UUID id) {
        if (id != null && channels.remove(id) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Channel channel : channels.values()) list.add(channel.save());
        tag.put("channels", list);
        return tag;
    }

    static WirelessChannels load(CompoundTag tag) {
        WirelessChannels data = new WirelessChannels();
        for (Tag entry : tag.getList("channels", Tag.TAG_COMPOUND)) {
            Channel channel = Channel.load((CompoundTag) entry);
            data.channels.put(channel.id, channel);
        }
        return data;
    }
}
