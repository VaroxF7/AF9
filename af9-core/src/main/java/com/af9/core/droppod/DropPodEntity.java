package com.af9.core.droppod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * The drop pod a new player arrives in (config {@code dropPod}): a rideable pod that falls from the top of the sky at a
 * terminal speed, flattens whatever soft a block it meets (leaves, plants, snow, hardness below 0.3), lands with a
 * thump, opens its door, lets its rider go after 1.5 s and lifts off again after 7 s. It never explodes and never
 * breaks anything hard: a pod that cannot lift off is simply gone.
 * <p>
 * The rider cannot leave it while it falls: a player who dismounts is put back (server side).
 */
public class DropPodEntity extends Entity {

    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(DropPodEntity.class,
            EntityDataSerializers.BOOLEAN);
    /** Whether the pod has been let go of: until then it hangs in the sky (the player is still loading, or has not pressed). */
    private static final EntityDataAccessor<Boolean> RELEASED = SynchedEntityData.defineId(DropPodEntity.class,
            EntityDataSerializers.BOOLEAN);
    /** Ticks until a hanging pod launches by itself: -1 while the player has not seen the pod yet (loading). */
    private static final EntityDataAccessor<Integer> COUNTDOWN = SynchedEntityData.defineId(DropPodEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SINCE_LANDING = SynchedEntityData.defineId(DropPodEntity.class,
            EntityDataSerializers.INT);

    /** Ticks after landing at which the rider is let go of (1.5 s). */
    public static final int RELEASE_TICKS = 30;
    /** Ticks after landing at which the pod lifts off (7 s). */
    public static final int TAKEOFF_TICKS = 140;
    /** Ticks of the lift-off after which the pod is gone, wherever it is. */
    private static final int FLIGHT_TICKS = 160;
    /** The speed it falls at, blocks a tick (10 blocks a second). */
    private static final double TERMINAL = 0.5;
    /** The speed it touches down at: the thrusters brake it to this over the last 30 blocks (1.4 blocks a second). */
    private static final double TOUCHDOWN = 0.07;
    /** From this height above the ground the thrusters fire. */
    private static final double BRAKING_HEIGHT = 32.0;
    /** Ticks of the countdown once the player's screen is clear (5 s). */
    public static final int AUTO_TICKS = 100;
    /** The rider is safe from falls and walls this long after the last tick in the pod (10 s). */
    public static final int SAFE_TICKS = 200;
    /** Persistent player data: the game time until which fall and wall damage is cancelled. */
    public static final String SAFE_KEY = "af9_pod_safe_until";
    /** The pod lets go by itself after this many ticks, whatever the client does (5 minutes). */
    private static final int HOLD_TIMEOUT = 6000;
    /** Blocks softer than this are flattened by a falling or rising pod. */
    private static final float SOFT = 0.3F;

    /** The player it carries: put back when they dismount while it falls. */
    private UUID rider;

    public DropPodEntity(EntityType<? extends DropPodEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.blocksBuilding = false;
    }

    public void setRider(UUID rider) {
        this.rider = rider;
    }

    public boolean isReleased() {
        return entityData.get(RELEASED);
    }

    /** Ticks left of the launch countdown, -1 before it began. */
    public int countdownTicks() {
        return entityData.get(COUNTDOWN);
    }

    /** The player's screen is clear: the launch countdown begins (once). */
    public void startCountdown() {
        if (entityData.get(COUNTDOWN) < 0) entityData.set(COUNTDOWN, AUTO_TICKS);
    }

    /** Lets the hanging pod go. */
    public void release() {
        entityData.set(RELEASED, true);
    }

    public boolean hasLanded() {
        return entityData.get(LANDED);
    }

    public int ticksSinceLanding() {
        return entityData.get(SINCE_LANDING);
    }

    /** 0 closed .. 1 open: the door and the restraint of the model. */
    public float openness(float partialTick) {
        if (!hasLanded()) return 0;
        float t = ticksSinceLanding() + partialTick;
        float open = Mth.clamp((t - 6) / 16F, 0, 1);
        float close = Mth.clamp((TAKEOFF_TICKS - 24 - t) / 14F, 0, 1);
        return Math.min(open, close);
    }

    //////////////////////////////////////
    // ************* Data ************//
    //////////////////////////////////////

    @Override
    protected void defineSynchedData() {
        entityData.define(LANDED, false);
        entityData.define(RELEASED, false);
        entityData.define(COUNTDOWN, -1);
        entityData.define(SINCE_LANDING, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(LANDED, tag.getBoolean("Landed"));
        // a pod saved before it could hang is simply let go
        entityData.set(RELEASED, !tag.contains("Released") || tag.getBoolean("Released"));
        entityData.set(SINCE_LANDING, tag.getInt("SinceLanding"));
        if (tag.hasUUID("Rider")) rider = tag.getUUID("Rider");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Landed", hasLanded());
        tag.putBoolean("Released", isReleased());
        tag.putInt("SinceLanding", ticksSinceLanding());
        if (rider != null) tag.putUUID("Rider", rider);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    //////////////////////////////////////
    // ********** Behaviour **********//
    //////////////////////////////////////

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.9;
    }

    /** The rider stands in the pod, it does not sit. */
    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        serverTick();
    }

    private void serverTick() {
        ServerLevel level = (ServerLevel) level();
        for (Entity passenger : getPassengers()) {
            passenger.fallDistance = 0;
            // the rider takes no fall or wall damage while in the pod and for a few seconds after
            if (passenger instanceof ServerPlayer player) {
                player.getPersistentData().putLong(SAFE_KEY, level.getGameTime() + SAFE_TICKS);
            }
        }

        if (!hasLanded()) {
            // a rider who got out is put back while the pod falls
            if (rider != null && getPassengers().isEmpty() && level.getPlayerByUUID(rider) instanceof ServerPlayer player
                    && !player.isSpectator() && player.isAlive()) {
                player.startRiding(this, true);
            }
            if (!isReleased()) {
                // hanging in the sky: nothing moves until the player is ready (or five minutes have passed)
                setDeltaMovement(Vec3.ZERO);
                int left = entityData.get(COUNTDOWN);
                if (left > 0) entityData.set(COUNTDOWN, left - 1);
                if (left == 0 || tickCount > HOLD_TIMEOUT) release();
                return;
            }
            flattenSoft(-1.0);
            double target = descentSpeed(groundDistance());
            double vy = Mth.lerp(0.15, getDeltaMovement().y, -target);
            setDeltaMovement(0, vy, 0);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround() || (isInWaterOrBubble() && tickCount > 20) || isInLava()) land(level);
            if (getY() < level.getMinBuildHeight()) discard();
            return;
        }

        int since = ticksSinceLanding() + 1;
        entityData.set(SINCE_LANDING, since);
        if (since == 6) level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL,
                1.5F, 0.6F);
        if (since == RELEASE_TICKS) ejectPassengers();
        if (since == TAKEOFF_TICKS) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 4.0F,
                    0.5F);
        }
        if (since >= TAKEOFF_TICKS) {
            // the lift-off: up, faster and faster, through soft blocks; anything hard above and the pod is just gone
            double vy = Math.min(2.0, Math.max(0.05, getDeltaMovement().y) * 1.12 + 0.02);
            setDeltaMovement(0, vy, 0);
            if (blockedAbove()) {
                discard();
                return;
            }
            flattenSoft(2.2);
            move(MoverType.SELF, getDeltaMovement());
            if (since > TAKEOFF_TICKS + FLIGHT_TICKS || getY() > level.getMaxBuildHeight() + 40) discard();
        }
    }

    private void land(ServerLevel level) {
        entityData.set(LANDED, true);
        setDeltaMovement(Vec3.ZERO);
        BlockPos below = BlockPos.containing(getX(), getY() - 0.2, getZ());
        BlockState state = level.getBlockState(below);
        if (!state.isAir()) {
            level.playSound(null, getX(), getY(), getZ(), state.getSoundType().getPlaceSound(), SoundSource.BLOCKS,
                    state.getSoundType().getVolume() * 1.5F, 0.5F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY() + 0.1, getZ(), 30,
                    1.0, 0.1, 1.0, 0.08);
        }
        // a soft touchdown: the legs settle, no crash
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.NEUTRAL, 1.5F, 0.5F);
    }

    /** Blocks between the pod's feet and the first solid or liquid block below it (48 at most). */
    private double groundDistance() {
        Level level = level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(getX()), 0, Mth.floor(getZ()));
        int top = Mth.floor(getY());
        for (int d = 0; d <= 48; d++) {
            int y = top - d - 1;
            if (y < level.getMinBuildHeight()) break;
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return Math.max(0, getY() - (y + 1));
            }
        }
        return 48;
    }

    /** The speed the thrusters hold at a height: terminal above 32 blocks, a walking pace at the ground. */
    private static double descentSpeed(double height) {
        double t = Mth.clamp((height - 3.0) / (BRAKING_HEIGHT - 3.0), 0, 1);
        return TOUCHDOWN + (TERMINAL - TOUCHDOWN) * t * t;
    }

    /** Flattens the soft blocks in the 3 x 3 a pod's size away: below it falling, above it rising. */
    private void flattenSoft(double dy) {
        Level level = level();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = BlockPos.containing(getX() + dx, getY() + (dy < 0 ? dy + 0.4 : dy), getZ() + dz);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0 || hardness >= SOFT) continue;
                // the rider's own rights count: spawn protection and claims keep their blocks
                if (rider != null && level.getPlayerByUUID(rider) instanceof ServerPlayer player) {
                    if (level.mayInteract(player, pos)) level.destroyBlock(pos, false, player);
                } else {
                    level.destroyBlock(pos, false);
                }
            }
        }
    }

    /** Whether something hard sits in the pod's way up. */
    private boolean blockedAbove() {
        Level level = level();
        BlockPos pos = BlockPos.containing(getX(), getY() + 3.0, getZ());
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        return hardness < 0 || hardness >= SOFT;
    }

    //////////////////////////////////////
    // ************ Client ***********//
    //////////////////////////////////////

    private void clientTick() {
        boolean falling = isReleased() && !hasLanded();
        boolean rising = hasLanded() && ticksSinceLanding() >= TAKEOFF_TICKS;
        boolean braking = false;
        if (falling) {
            // predict the fall between the server's updates
            double target = descentSpeed(groundDistance());
            braking = target < TERMINAL * 0.95;
            setDeltaMovement(0, Mth.lerp(0.15, getDeltaMovement().y, -target), 0);
            move(MoverType.SELF, getDeltaMovement());
        }
        if (falling || rising) {
            for (int i = 0; i < (braking ? 8 : 4); i++) {
                double side = (i & 1) == 0 ? 0.5 : -0.5;
                double front = (i & 2) == 0 ? 0.5 : -0.5;
                // the exhaust goes down, hard when the thrusters brake
                double jet = braking || rising ? -0.55 : -0.3;
                level().addParticle(ParticleTypes.FLAME, getX() + side, getY() + 0.05, getZ() + front,
                        random.nextGaussian() * 0.02, jet, random.nextGaussian() * 0.02);
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + side, getY() + 0.05, getZ() + front,
                        random.nextGaussian() * 0.03, jet * 0.8, random.nextGaussian() * 0.03);
            }
        } else if (ticksSinceLanding() < 40 && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + random.nextGaussian() * 0.5,
                    getY() + 0.2, getZ() + random.nextGaussian() * 0.5, 0, 0.04, 0);
        }
    }
}
