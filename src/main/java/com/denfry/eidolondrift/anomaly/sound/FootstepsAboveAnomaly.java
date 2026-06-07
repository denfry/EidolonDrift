package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Slow footsteps cross the floor above — in a single-storey home (catalogue #10, Stage 4).
 * Three unhurried steps from the ceiling, drifting across, then nothing. Sound-only, unicast.
 */
public final class FootstepsAboveAnomaly extends AbstractHomeAnomaly {

    public FootstepsAboveAnomaly() {
        super("footsteps_above", AnomalyCategory.SOUND);
    }

    @Override protected int minStage() { return 4; }
    @Override protected float baseHomeWeight(AnomalyContext ctx) { return 1.3f; }
    @Override public int cooldownTicks() { return 9600; } // 8 min
    @Override public int pressureCost() { return 1; }

    @Override
    public void execute(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        // Start to one side of the ceiling and pace across as the steps land.
        double dx = (player.getRandom().nextDouble() - 0.5) * 5.0;
        double dz = (player.getRandom().nextDouble() - 0.5) * 5.0;
        Vec3 above = player.position().add(dx, 3.0 + player.getRandom().nextInt(2), dz);
        SoundScheduler.playSeries(player, ModSounds.STEP_BEHIND, above, 3, 11, 0.55f, 0.85f);
    }
}
