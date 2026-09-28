package com.af9.core.bus;

import com.gregtechceu.gtceu.api.gui.factory.MachineUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Machines opened from a Central Monitor (the Machine Bus Module's page): their own screen, as the machine shows it
 * off the bus; the "Part of a BUS" card ({@link com.af9.core.machine.console.BusPlacardWidget}) lets that player
 * through. Server side: which player has which machine open that way, until the screen closes.
 */
public final class BusRemote {

    private static final Map<UUID, Long> OPEN = new ConcurrentHashMap<>();

    private BusRemote() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(BusRemote::onClose);
        MinecraftForge.EVENT_BUS.addListener(BusRemote::onLogout);
    }

    /** Whether the player opened the machine at {@code pos} from a Central Monitor. */
    public static boolean isOpenedRemotely(Player player, BlockPos pos) {
        Long open = player == null ? null : OPEN.get(player.getUUID());
        return open != null && open == pos.asLong();
    }

    /**
     * Opens a machine's screen for a player at a Central Monitor, a tick later (the monitor's screen closes first).
     *
     * @return false if the machine is not in a chunk the player's client has (its screen cannot be built there)
     */
    public static boolean open(ServerPlayer player, MetaMachine machine) {
        if (!(machine.getLevel() instanceof ServerLevel level) || level != player.serverLevel()) return false;
        if (!level.getChunkSource().chunkMap.getPlayers(new ChunkPos(machine.getPos()), false).contains(player)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount(), () -> {
            if (machine.isInValid() || player.hasDisconnected()) return;
            player.closeContainer();
            OPEN.put(player.getUUID(), machine.getPos().asLong());
            if (!MachineUIFactory.INSTANCE.openUI(machine, player)) OPEN.remove(player.getUUID());
        }));
        return true;
    }

    private static void onClose(PlayerContainerEvent.Close event) {
        OPEN.remove(event.getEntity().getUUID());
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OPEN.remove(event.getEntity().getUUID());
    }
}
