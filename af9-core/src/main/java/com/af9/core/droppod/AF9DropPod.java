package com.af9.core.droppod;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The drop pod a new player arrives in. A player who joins a world for the first time (a play time of zero, and not
 * yet marked) is taken to the top of the sky above their spawn, set in a {@link DropPodEntity} and let fall: the pod
 * flattens the leaves, lands, opens and lifts off again. Creative and spectator players skip it, and so does every
 * player who has already played in a world the pack is added to. Settings: {@link AF9Config} ({@code dropPod}).
 * Spec: docs/drop-pod.md
 */
public final class AF9DropPod {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            AF9Core.MOD_ID);

    public static final RegistryObject<EntityType<DropPodEntity>> DROP_POD = ENTITIES.register("drop_pod",
            () -> EntityType.Builder.<DropPodEntity>of(DropPodEntity::new, MobCategory.MISC)
                    .sized(1.8F, 3.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("drop_pod"));

    /** The marker on a player who has had their drop (Forge's persisted player data, kept through death). */
    private static final String ARRIVED = "af9_drop_pod_arrived";

    private AF9DropPod() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID)
    public static final class Events {

        private Events() {}

        /** The rider of a pod (and a few seconds after) takes no fall or wall damage: no death on the ground. */
        @SubscribeEvent
        public static void onAttack(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (player.getPersistentData().getLong(DropPodEntity.SAFE_KEY) < player.level().getGameTime()) return;
            DamageSource source = event.getSource();
            if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL)
                    || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.CRAMMING)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (!AF9Config.DROP_POD.get()) return;
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.getBoolean(ARRIVED)) return;
            // marked either way: a player who skips it now (creative, or already played) never gets it later
            persisted.putBoolean(ARRIVED, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

            boolean newPlayer = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) == 0;
            if (!newPlayer || player.isSpectator() || (player.isCreative() && !AF9Config.DROP_POD_CREATIVE.get())) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
            drop(player, level);
        }

        /** {@code /af9 droppod [player]}: an operator sends a player down again (any world, any time). */
        @SubscribeEvent
        public static void onCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("af9").requires(source -> source.hasPermission(2))
                    .then(Commands.literal("droppod")
                            .executes(context -> send(context.getSource(), context.getSource().getPlayerOrException()))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(context -> send(context.getSource(),
                                            EntityArgument.getPlayer(context, "player"))))));
        }

        private static int send(CommandSourceStack source, ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) return 0;
            player.stopRiding();
            DropPodEntity pod = drop(player, level);
            // the player is in the world already: no waiting for the loading screen
            if (pod != null) pod.release();
            source.sendSuccess(() -> Component.translatable("af9.drop_pod.sent", player.getDisplayName()), true);
            return 1;
        }

        /** Whether a pod (falling, landed or lifting off) is already within 5 blocks of the column. */
        private static boolean crowded(ServerLevel level, double x, double z) {
            AABB column = new AABB(x - 5, level.getMinBuildHeight(), z - 5, x + 5, level.getMaxBuildHeight() + 64,
                    z + 5);
            return !level.getEntitiesOfClass(DropPodEntity.class, column).isEmpty();
        }

        /** Takes the player to the top of the sky above them, in a pod, with the title on their screen. */
        private static DropPodEntity drop(ServerPlayer player, ServerLevel level) {
            int height = AF9Config.DROP_POD_HEIGHT.get();
            double y = Math.min(level.getMaxBuildHeight() - 6, player.getY() + height);
            // players who join together each get a column of their own: a pod of another is never within 5 blocks
            double x = player.getX();
            double z = player.getZ();
            for (int i = 0; i < 16 && crowded(level, x, z); i++) {
                double angle = i * 2.4;
                double radius = 6 + i * 1.5;
                x = player.getX() + Math.cos(angle) * radius;
                z = player.getZ() + Math.sin(angle) * radius;
            }
            player.teleportTo(level, x, y, z, player.getYRot(), 0);

            DropPodEntity pod = DROP_POD.get().create(level);
            if (pod == null) return null;
            pod.moveTo(x, y, z, player.getYRot(), 0);
            pod.setRider(player.getUUID());
            level.addFreshEntity(pod);
            player.startRiding(pod, true);
            return pod;
        }
    }
}
