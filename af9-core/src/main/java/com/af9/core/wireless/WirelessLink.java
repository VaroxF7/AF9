package com.af9.core.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A transmitter's link as a data stick carries it (item tag "af9_wireless"): the channel, and where the transmitter
 * is (for the tooltip and the messages).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public record WirelessLink(UUID channel, ResourceLocation dimension, BlockPos pos, int amperage) {

    public static final String TAG = "af9_wireless";

    public void write(ItemStack stick) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("channel", channel);
        tag.putString("dimension", dimension.toString());
        tag.putLong("pos", pos.asLong());
        tag.putInt("amperage", amperage);
        stick.getOrCreateTag().put(TAG, tag);
    }

    /** The link on a data stick, or null. */
    public static WirelessLink read(ItemStack stick) {
        CompoundTag root = stick.getTag();
        if (root == null || !root.contains(TAG)) return null;
        CompoundTag tag = root.getCompound(TAG);
        if (!tag.hasUUID("channel")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        return new WirelessLink(tag.getUUID("channel"),
                dimension == null ? new ResourceLocation("minecraft", "overworld") : dimension,
                BlockPos.of(tag.getLong("pos")), tag.getInt("amperage"));
    }

    /** "x y z in dimension". */
    public Component where() {
        return Component.translatable("af9.wireless.where", pos.getX(), pos.getY(), pos.getZ(), dimension.toString());
    }

    /** First 8 characters of the channel id, as shown in the hatches' screens. */
    public static String shortId(UUID channel) {
        return channel == null ? "-" : channel.toString().substring(0, 8);
    }
}
