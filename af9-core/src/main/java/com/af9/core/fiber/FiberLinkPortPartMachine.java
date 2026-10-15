package com.af9.core.fiber;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Fiber Link Port: the end of an optical fibre between a machine and its Logistics Hub. The machine's port sits in its
 * structure (the Photolithography Scanner Mk2 takes one); the hub has the other. Link them with a data stick: right-
 * click one port (the stick remembers it), right-click the other (both are linked, in either order).
 * <p>
 * The port is a pair of buffers the machine's recipes work on like any hatch: an <b>in</b> side the recipes take from
 * (items and fluids; gases are fluids) and an <b>out</b> side they put their results into. The hub fills the in side
 * and empties the out side ({@link LogisticsHubMachine}), so everything the player puts into the hub reaches the
 * machine and everything the machine makes comes back out of the hub. Any distance and dimension, as long as both
 * ends' chunks are loaded.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class FiberLinkPortPartMachine extends TieredPartMachine implements IDataStickInteractable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FiberLinkPortPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the port's definition (KubeJS), taken by the Mk2 scanner's and the hub's patterns. */
    public static final PartAbility FIBER_LINK = new PartAbility("af9_fiber_link");
    /** Data stick tag: the port the stick remembers, {dimension, pos}. */
    public static final String TAG = "af9_fiber";

    public static final int ITEM_SLOTS = 9, FLUID_TANKS = 4;
    public static final int TANK_CAPACITY = 16_000;

    @Persisted
    public final NotifiableItemStackHandler itemsIn, itemsOut;
    @Persisted
    public final NotifiableFluidTank fluidsIn, fluidsOut;
    /** The other end: dimension and position, or empty. */
    @Persisted
    private String peerDimension = "";
    @Persisted
    private long peerPos;

    public FiberLinkPortPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        // IO.IN/OUT: the recipes read the in side and write the out side; the hub reaches the handlers directly,
        // so no pipe access (IO.NONE) is offered to the outside
        itemsIn = new NotifiableItemStackHandler(this, ITEM_SLOTS, IO.IN, IO.NONE);
        itemsOut = new NotifiableItemStackHandler(this, ITEM_SLOTS, IO.OUT, IO.NONE);
        fluidsIn = new NotifiableFluidTank(this, FLUID_TANKS, TANK_CAPACITY, IO.IN, IO.NONE);
        fluidsOut = new NotifiableFluidTank(this, FLUID_TANKS, TANK_CAPACITY, IO.OUT, IO.NONE);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ************ Link ************//
    //////////////////////////////////////

    public boolean isLinked() {
        return !peerDimension.isEmpty();
    }

    /** The port at the other end, or null (not linked, its chunk not loaded, or it is gone). */
    public FiberLinkPortPartMachine peer() {
        if (!isLinked() || !(getLevel() instanceof ServerLevel level)) return null;
        ResourceLocation id = ResourceLocation.tryParse(peerDimension);
        if (id == null) return null;
        Level other = level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, id));
        BlockPos pos = BlockPos.of(peerPos);
        if (other == null || !other.isLoaded(pos)) return null;
        return MetaMachine.getMachine(other, pos) instanceof FiberLinkPortPartMachine port ? port : null;
    }

    private void setPeer(Level level, BlockPos pos) {
        peerDimension = level.dimension().location().toString();
        peerPos = pos.asLong();
        markDirty();
    }

    private void clearPeer() {
        peerDimension = "";
        peerPos = 0;
        markDirty();
    }

    /** Right-click with a data stick: the first port is remembered, the second one is linked to it. */
    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        if (!(getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        CompoundTag root = dataStick.getTag();
        if (root == null || !root.contains(TAG)) {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", level.dimension().location().toString());
            tag.putLong("pos", getPos().asLong());
            dataStick.getOrCreateTag().put(TAG, tag);
            player.sendSystemMessage(Component.translatable("af9.fiber.stick_written", getPos().getX(),
                    getPos().getY(), getPos().getZ()).withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = root.getCompound(TAG);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
        BlockPos pos = BlockPos.of(tag.getLong("pos"));
        Level other = id == null ? null : level.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, id));
        if (other == null || !(MetaMachine.getMachine(other, pos) instanceof FiberLinkPortPartMachine port)) {
            // the remembered port is gone: this one is remembered instead
            root.remove(TAG);
            return onDataStickUse(player, dataStick);
        }
        if (port == this) {
            root.remove(TAG);
            player.sendSystemMessage(Component.translatable("af9.fiber.same_port").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        // drop the old links of both, then link them
        FiberLinkPortPartMachine oldA = peer(), oldB = port.peer();
        if (oldA != null) oldA.clearPeer();
        if (oldB != null) oldB.clearPeer();
        setPeer(other, pos);
        port.setPeer(level, getPos());
        root.remove(TAG);
        player.sendSystemMessage(Component.translatable("af9.fiber.linked").withStyle(ChatFormatting.AQUA));
        return InteractionResult.SUCCESS;
    }

    /** Shift-right-click with a data stick: cut the link (both ends). */
    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!(getLevel() instanceof ServerLevel)) return InteractionResult.SUCCESS;
        FiberLinkPortPartMachine other = peer();
        if (other != null) other.clearPeer();
        clearPeer();
        player.sendSystemMessage(Component.translatable("af9.fiber.cut").withStyle(ChatFormatting.YELLOW));
        return InteractionResult.SUCCESS;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** Short status for the screens and tooltips. */
    public List<Component> statusLines() {
        return List.of(isLinked() ?
                Component.translatable(peer() != null ? "af9.fiber.status_ok" : "af9.fiber.status_far")
                        .withStyle(peer() != null ? ChatFormatting.GREEN : ChatFormatting.YELLOW) :
                Component.translatable("af9.fiber.status_unlinked").withStyle(ChatFormatting.RED));
    }
}
