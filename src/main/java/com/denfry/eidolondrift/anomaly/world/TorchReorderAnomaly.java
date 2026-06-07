package com.denfry.eidolondrift.anomaly.world;

import java.util.List;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.util.BlockRevertScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A torch turns up where it wasn't — the lighting "order" near the entrance quietly shifts
 * (catalogue #5/#15, Stage 4). Implemented additively (no player torch is ever removed): a
 * ghost torch is placed in a valid empty spot and later cleared back to air, so nothing the
 * player built is touched (invariant §8). Gated behind {@code world_mutation_allowed}.
 */
public final class TorchReorderAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 2400; // ~2 min before it vanishes again

    public TorchReorderAnomaly() {
        super("torch_reorder", AnomalyCategory.WORLD);
    }

    @Override protected int minStage() { return 4; }
    @Override public int cooldownTicks() { return 12000; } // 10 min
    @Override public int pressureCost() { return 1; }
    @Override protected boolean extraGate(AnomalyContext ctx) { return ModConfig.WORLD_MUTATION_ALLOWED.get(); }

    @Override
    public void execute(AnomalyContext ctx) {
        if (!ModConfig.WORLD_MUTATION_ALLOWED.get()) {
            EidolonDrift.LOGGER.debug("torch_reorder skipped: world_mutation_allowed is off");
            return;
        }
        ServerPlayer player = ctx.player();
        ServerLevel level = player.serverLevel();
        BlockState torch = Blocks.TORCH.defaultBlockState();

        List<BlockPos> airSpots = ctx.worldMemory().home().findBlocks(
                level, player.blockPosition(), 10, s -> s.isAir(), 64);
        for (int i = airSpots.size() - 1; i > 0; i--) { // shuffle in place
            int j = player.getRandom().nextInt(i + 1);
            BlockPos tmp = airSpots.get(i); airSpots.set(i, airSpots.get(j)); airSpots.set(j, tmp);
        }
        for (BlockPos pos : airSpots) {
            if (!torch.canSurvive(level, pos)) continue;
            BlockState original = level.getBlockState(pos); // air
            level.setBlock(pos, torch, Block.UPDATE_ALL);
            BlockRevertScheduler.schedule(level, pos, original, null, DURATION_TICKS);
            return;
        }
    }
}
