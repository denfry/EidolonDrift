package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.world.phys.Vec3;

/**
 * A faint voice far off in a random direction — almost a real villager or another
 * player, but there is no one there. Stronger pull for socially-fearful players.
 */
public final class DistantVoiceAnomaly extends AbstractAnomaly {

    public DistantVoiceAnomaly() {
        super("distant_voice", AnomalyCategory.SOUND);
    }

    @Override public int cooldownTicks() { return 12000; } // 10 min
    @Override public int pressureCost() { return 1; }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.progressionStage() >= 2
                && ctx.mindState().isolation > 25f;
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        return 1.2f + ctx.mindState().isolation * 0.015f;
    }

    @Override
    public void execute(AnomalyContext ctx) {
        var player = ctx.player();
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        double dist = 30 + player.getRandom().nextInt(20);
        Vec3 far = player.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        SoundScheduler.playOnce(player, ModSounds.DISTANT_VOICE, far, 0.7f, 1.0f);
    }
}
