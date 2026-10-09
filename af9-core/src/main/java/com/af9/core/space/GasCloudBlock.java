package com.af9.core.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The cloud of a gas giant (Zephyr, Kronos) and the plasma of the Sun (Helios): a translucent block the dimensions'
 * noise settings (data/af9/worldgen/noise_settings) fill the floating islands and the sea of cloud with. A cloud
 * catches a fall and throws the faller back up a little, as the Aether's clouds do; the plasma burns instead.
 */
public class GasCloudBlock extends HalfTransparentBlock {

    private final boolean burns;

    public GasCloudBlock(Properties properties, boolean burns) {
        super(properties);
        this.burns = burns;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // a cloud takes the fall (the plasma too: the burn is the harm there)
        entity.causeFallDamage(fallDistance, 0.0f, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (burns || motion.y >= 0 || entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        double bounce = entity instanceof LivingEntity ? 0.45 : 0.3;
        entity.setDeltaMovement(motion.x, -motion.y * bounce, motion.z);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (burns && !level.isClientSide && entity instanceof LivingEntity && !entity.fireImmune()) {
            entity.setSecondsOnFire(4);
        }
    }
}
