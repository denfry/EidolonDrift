package com.denfry.eidolondrift.anomaly.sound;

import java.util.Optional;
import java.util.Set;

import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A chest opens somewhere in the home with no one near it (catalogue #2/#12, Stage 2). Plays
 * the <i>real</i> vanilla chest sound — subtitle "Chest opens" — so it stays deniable
 * (invariant §7): a bug? a mob? or did something just open your storage? Sound-only, unicast.
 */
public final class ChestClickNoPlayerAnomaly extends AbstractHomeAnomaly {

    private static final Set<net.minecraft.world.level.block.Block> CONTAINERS = Set.of(
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST);

    public ChestClickNoPlayerAnomaly() {
        super("chest_click_no_player", AnomalyCategory.SOUND);
    }

    @Override protected int minStage() { return 2; }
    @Override public int cooldownTicks() { return 7200; } // 6 min
    @Override public int pressureCost() { return 1; }

    @Override
    public void execute(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        Level level = player.level();
        BlockPos at = ctx.worldMemory().home()
                .findRandomBlock(level, player.blockPosition(), 12,
                        s -> CONTAINERS.contains(s.getBlock()), player.getRandom())
                .orElseGet(() -> behindPlayer(player)); // no chest found → a click from the next room

        Vec3 pos = Vec3.atCenterOf(at);
        // The real vanilla chest sound (subtitle "Chest opens") keeps it deniable (invariant §7).
        Holder<SoundEvent> chest = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CHEST_OPEN);
        SoundScheduler.playOnce(player, chest, pos, 0.7f, 1.0f);
    }

    private static BlockPos behindPlayer(ServerPlayer player) {
        Vec3 p = player.getLookAngle().scale(-(3 + player.getRandom().nextInt(4))).add(player.position());
        return BlockPos.containing(p);
    }
}
