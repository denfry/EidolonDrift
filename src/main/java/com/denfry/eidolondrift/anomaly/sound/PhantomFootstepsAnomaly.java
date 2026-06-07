package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.world.phys.Vec3;

/**
 * Three slow footsteps behind the player, near home (GDD §19). The signature early
 * "is someone there?" beat — ambiguous enough to be deniable (invariant §7).
 */
public final class PhantomFootstepsAnomaly extends AbstractAnomaly {

    public PhantomFootstepsAnomaly() {
        super("phantom_footsteps", AnomalyCategory.SOUND);
    }

    @Override public int cooldownTicks() { return 9600; } // 8 min
    @Override public int pressureCost() { return 1; }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.mindState().isolation > 40f
                && ctx.progressionStage() >= 1
                && ctx.isNearHome();
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        float w = 2.0f;
        if (ctx.fearIsHome()) w *= 1.8f;
        return w;
    }

    @Override
    public void execute(AnomalyContext ctx) {
        var player = ctx.player();
        // 8–14 blocks behind the player's current facing.
        Vec3 behind = player.getLookAngle().scale(-(8 + player.getRandom().nextInt(7)))
                .add(player.position());
        SoundScheduler.playSeries(player, ModSounds.STEP_BEHIND,
                behind, 3, 20, 0.7f, 1.0f);
    }
}
