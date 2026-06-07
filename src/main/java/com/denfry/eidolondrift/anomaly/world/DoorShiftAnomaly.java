package com.denfry.eidolondrift.anomaly.world;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.util.BlockRevertScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A door drifts open (or shut) on its own, then settles back (catalogue #1/#14, Stage 2).
 *
 * <p>This is a <i>real</i> block change, so it is gated behind {@code world_mutation_allowed}
 * (OFF by default, invariant §1) and is always reverted by {@link BlockRevertScheduler} — the
 * door returns to exactly how you left it (invariant §8). When the gate is off, M4's client
 * channel will carry this as a per-player illusion instead.
 */
public final class DoorShiftAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 200; // ~10 s, then it swings back

    public DoorShiftAnomaly() {
        super("door_shift", AnomalyCategory.WORLD);
    }

    @Override protected int minStage() { return 2; }
    @Override public int cooldownTicks() { return 9600; } // 8 min
    @Override public int pressureCost() { return 2; }
    @Override protected boolean extraGate(AnomalyContext ctx) { return ModConfig.WORLD_MUTATION_ALLOWED.get(); }

    @Override
    public void execute(AnomalyContext ctx) {
        if (!ModConfig.WORLD_MUTATION_ALLOWED.get()) {
            EidolonDrift.LOGGER.debug("door_shift skipped: world_mutation_allowed is off");
            return;
        }
        ServerPlayer player = ctx.player();
        ServerLevel level = player.serverLevel();
        var target = ctx.worldMemory().home().findRandomBlock(level, player.blockPosition(), 12,
                s -> s.getBlock() instanceof DoorBlock
                        && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER,
                player.getRandom());
        if (target.isEmpty()) return;

        BlockPos lower = target.get();
        BlockPos upper = lower.above();
        BlockState lowerState = level.getBlockState(lower);
        BlockState upperState = level.getBlockState(upper);
        if (!(lowerState.getBlock() instanceof DoorBlock door)) return;

        boolean open = lowerState.getValue(DoorBlock.OPEN);
        door.setOpen(player, level, lowerState, lower, !open);

        // Restore both halves to exactly their captured states.
        BlockRevertScheduler.schedule(level, lower, lowerState, null, DURATION_TICKS);
        BlockRevertScheduler.schedule(level, upper, upperState, null, DURATION_TICKS);
    }
}
