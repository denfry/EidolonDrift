package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Something paces <i>outside</i> the walls — the Stage-2 "Heard Outside" rung of the ladder
 * (GDD §7). A few footsteps at ground level a short way off, where no path runs. Sound-only,
 * unicast, and deliberately ambiguous (invariant §7): could be a wandering mob.
 */
public final class FootstepsOutsideAnomaly extends AbstractHomeAnomaly {

    public FootstepsOutsideAnomaly() {
        super("footsteps_outside", AnomalyCategory.SOUND);
    }

    @Override protected int minStage() { return 2; }
    @Override public int cooldownTicks() { return 7200; } // 6 min
    @Override public int pressureCost() { return 1; }

    @Override
    public void execute(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        // A lateral bearing a few blocks out, at the player's feet height — "past the wall".
        float angle = player.getRandom().nextFloat() * (float) (Math.PI * 2);
        double dist = 4.0 + player.getRandom().nextDouble() * 3.0;
        Vec3 outside = player.position().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
        SoundScheduler.playSeries(player, ModSounds.STEP_BEHIND, outside, 4, 9, 0.5f, 0.9f);
    }
}
