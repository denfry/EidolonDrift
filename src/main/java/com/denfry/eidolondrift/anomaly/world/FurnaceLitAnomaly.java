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
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A furnace glows and crackles with no fuel in it, then goes cold (catalogue #7, Stage 3).
 * Flips only the visual {@code lit} blockstate — no items, no burn progress — and reverts it.
 * Gated behind {@code world_mutation_allowed} (invariant §1); always restored (invariant §8).
 */
public final class FurnaceLitAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 400; // ~20 s (catalogue #7)

    public FurnaceLitAnomaly() {
        super("furnace_lit", AnomalyCategory.WORLD);
    }

    @Override protected int minStage() { return 3; }
    @Override public int cooldownTicks() { return 9600; } // 8 min
    @Override public int pressureCost() { return 1; }
    @Override protected boolean extraGate(AnomalyContext ctx) { return ModConfig.WORLD_MUTATION_ALLOWED.get(); }

    @Override
    public void execute(AnomalyContext ctx) {
        if (!ModConfig.WORLD_MUTATION_ALLOWED.get()) {
            EidolonDrift.LOGGER.debug("furnace_lit skipped: world_mutation_allowed is off");
            return;
        }
        ServerPlayer player = ctx.player();
        ServerLevel level = player.serverLevel();
        var target = ctx.worldMemory().home().findRandomBlock(level, player.blockPosition(), 12,
                s -> s.getBlock() instanceof AbstractFurnaceBlock
                        && s.hasProperty(BlockStateProperties.LIT)
                        && !s.getValue(BlockStateProperties.LIT),
                player.getRandom());
        if (target.isEmpty()) return;

        BlockPos pos = target.get();
        BlockState original = level.getBlockState(pos);
        level.setBlock(pos, original.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
        BlockRevertScheduler.schedule(level, pos, original, null, DURATION_TICKS);
    }
}
