package com.denfry.eidolondrift.anomaly.world;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.util.BlockRevertScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sign briefly reads your own name, spelled backwards (catalogue #6/#18, Stage 3). Captures
 * the sign's full block-entity data, rewrites the front face, and reverts the exact original
 * text via {@link BlockRevertScheduler} — your words come back untouched (invariant §8). Gated
 * behind {@code world_mutation_allowed} (invariant §1).
 */
public final class SignTextAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 600; // ~30 s then reverts (catalogue #6)

    public SignTextAnomaly() {
        super("sign_text", AnomalyCategory.WORLD);
    }

    @Override protected int minStage() { return 3; }
    @Override public int cooldownTicks() { return 12000; } // 10 min
    @Override public int pressureCost() { return 1; }
    @Override protected boolean extraGate(AnomalyContext ctx) { return ModConfig.WORLD_MUTATION_ALLOWED.get(); }

    @Override
    public void execute(AnomalyContext ctx) {
        if (!ModConfig.WORLD_MUTATION_ALLOWED.get()) {
            EidolonDrift.LOGGER.debug("sign_text skipped: world_mutation_allowed is off");
            return;
        }
        ServerPlayer player = ctx.player();
        ServerLevel level = player.serverLevel();
        var target = ctx.worldMemory().home().findRandomBlock(level, player.blockPosition(), 12,
                s -> s.getBlock() instanceof SignBlock, player.getRandom());
        if (target.isEmpty()) return;

        BlockPos pos = target.get();
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;

        CompoundTag original = sign.saveWithFullMetadata(level.registryAccess());
        String reversed = new StringBuilder(player.getGameProfile().getName()).reverse().toString();

        SignText txt = sign.getFrontText()
                .setMessage(0, Component.empty())
                .setMessage(1, Component.literal(reversed))
                .setMessage(2, Component.empty())
                .setMessage(3, Component.empty());
        sign.setText(txt, true);
        sign.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

        BlockRevertScheduler.schedule(level, pos, state, original, DURATION_TICKS);
    }
}
