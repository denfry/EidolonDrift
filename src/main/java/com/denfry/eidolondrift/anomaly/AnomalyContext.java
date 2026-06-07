package com.denfry.eidolondrift.anomaly;

import com.denfry.eidolondrift.director.AnomalyMemory;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.mind.MindState;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Immutable snapshot of everything an anomaly needs to decide and act (GDD §5).
 * Built once per Director evaluation so {@code canFire}/{@code baseWeight}/{@code execute}
 * all see a consistent world.
 */
public record AnomalyContext(
        ServerPlayer player,
        MindState mindState,
        int progressionStage,
        ResourceKey<Level> dimension,
        long gameTime,
        int lightLevel,
        boolean isUnderground,
        boolean isNight,
        boolean isNearHome,
        double distanceFromHome,
        PlayerWorldMemory worldMemory,
        AnomalyMemory anomalyHistory
) {
    /** Radius (blocks) within which the player counts as "near home". */
    public static final double HOME_RADIUS = 48.0;

    public static AnomalyContext build(ServerPlayer player, AnomalyMemory history) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        MindState ms = player.getData(com.denfry.eidolondrift.registry.ModDataAttachments.MIND_STATE.get());
        PlayerWorldMemory mem = player.getData(com.denfry.eidolondrift.registry.ModDataAttachments.WORLD_MEMORY.get());

        long time = level.getDayTime() % 24000L;
        boolean night = time > 12542 && time < 23460;
        boolean underground = !level.canSeeSky(pos) && !level.dimensionType().hasCeiling();

        double distHome = Double.MAX_VALUE;
        if (mem.primaryBedPos.isPresent()) {
            distHome = Math.sqrt(mem.primaryBedPos.get().distSqr(pos));
        }
        boolean nearHome = distHome <= HOME_RADIUS;

        return new AnomalyContext(
                player, ms, ms.progressionStage,
                level.dimension(), level.getGameTime(),
                level.getMaxLocalRawBrightness(pos),
                underground, night, nearHome, distHome,
                mem, history);
    }

    /** Sugar used heavily by {@code baseWeight}. */
    public boolean fearIsHome() {
        return worldMemory.fearProfile.primaryFear == com.denfry.eidolondrift.memory.FearVector.HOME;
    }
}
