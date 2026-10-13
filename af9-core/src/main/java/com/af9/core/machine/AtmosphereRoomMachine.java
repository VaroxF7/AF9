package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * The Atmospheric Simulation Room (structure and recipe type in
 * {@code kubejs/startup_scripts/gtceu/atmospheric_simulation.js}): a pressure room after GT's
 * Cleanroom that holds a Lunar atmosphere anywhere. The controller in the ceiling centre houses the
 * Atmospheric Simulator; XPS and NVM circuits (the tiers circuits_af9.js had parked) build in its
 * lunar air.
 * <p>
 * The room is genuinely lunar inside: a player who walks in without a full Ad Astra space suit
 * takes Ad Astra's own oxygen damage (like its airless dimensions), suit or hurt, every second.
 */
public class AtmosphereRoomMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AtmosphereRoomMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    /** The room's recipe type (gtceu namespace). */
    public static final String RECIPE_TYPE = "atmospheric_simulation";

    /** Ticks between two suit checks. */
    private static final int SUIT_INTERVAL = 20;

    private TickableSubscription suitSubs;

    public AtmosphereRoomMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.atmosphere.console.title";
    }

    @Override
    public int modeColor(int index) {
        return 0xFF8FD0FF;
    }

    //////////////////////////////////////
    // ********* Lunar air **********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        suitSubs = subscribeServerTick(suitSubs, this::tickSuits);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribeSuits();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribeSuits();
    }

    private void unsubscribeSuits() {
        if (suitSubs != null) {
            suitSubs.unsubscribe();
            suitSubs = null;
        }
    }

    /** Hurts the unsuited inside the room's lunar air, like Ad Astra's airless dimensions. */
    private void tickSuits() {
        if (getOffsetTimer() % SUIT_INTERVAL != 0 || !isFormed()) return;
        if (!ModList.get().isLoaded("ad_astra")) return;
        var level = getLevel();
        if (level == null) return;
        AABB room = interiorBox();
        if (room == null) return;
        for (Player player : level.getEntitiesOfClass(Player.class, room,
                e -> e instanceof ServerPlayer && !e.isCreative() && !e.isSpectator())) {
            if (earth.terrarium.adastra.common.items.armor.SpaceSuitItem.hasFullSet(player)) continue;
            player.hurt(earth.terrarium.adastra.common.registry.ModDamageSources.create(level,
                    earth.terrarium.adastra.common.registry.ModDamageSources.OXYGEN), 2.0F);
            player.setAirSupply(-80);
        }
    }

    /**
     * The room's air, in world coords: 3 x 3 x 5 inside the 7 x 5 x 7 shell, under the ceiling
     * controller (offsets mirror the KubeJS pattern: x -1..1, up -3..-1, back -5..-1).
     */
    private AABB interiorBox() {
        boolean flipped = this instanceof MultiblockControllerMachine controller && controller.isFlipped();
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), flipped);
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), flipped);
        Direction left = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(), flipped);
        double px = getPos().getX() + 0.5, py = getPos().getY() + 0.5, pz = getPos().getZ() + 0.5;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (double lx : new double[] { -1.5, 1.5 }) {
            for (double ly : new double[] { -3.5, -0.5 }) {
                for (double lz : new double[] { -5.5, -0.5 }) {
                    double wx = px + lx * left.getStepX() + ly * up.getStepX() + lz * back.getStepX();
                    double wy = py + lx * left.getStepY() + ly * up.getStepY() + lz * back.getStepY();
                    double wz = pz + lx * left.getStepZ() + ly * up.getStepZ() + lz * back.getStepZ();
                    minX = Math.min(minX, wx);
                    minY = Math.min(minY, wy);
                    minZ = Math.min(minZ, wz);
                    maxX = Math.max(maxX, wx);
                    maxY = Math.max(maxY, wy);
                    maxZ = Math.max(maxZ, wz);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    //////////////////////////////////////
    // *********** Screen **********//
    //////////////////////////////////////

    /** Lunar atmosphere, and what the room is doing. */
    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.atmosphere.console.air"));
        var logic = getRecipeLogic();
        if (logic.isWorking()) {
            lines.add(Component.translatable("af9.atmosphere.console.running"));
        } else if (!ModList.get().isLoaded("ad_astra")) {
            lines.add(Component.translatable("af9.atmosphere.console.no_suits"));
        } else {
            lines.add(Component.translatable("af9.atmosphere.console.idle"));
        }
        String output = getCurrentOutput();
        if (!output.isEmpty()) lines.add(Component.translatable("af9.atmosphere.console.output", output));
        return lines;
    }
}
