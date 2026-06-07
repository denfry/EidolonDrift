package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.world.phys.Vec3;

/**
 * A low drone resonating out of the rock when the player is deep underground — like the
 * cave breathing. Scales with accumulated cave resonance (GDD §4).
 */
public final class CaveResonanceAnomaly extends AbstractAnomaly {

    public CaveResonanceAnomaly() {
        super("cave_resonance", AnomalyCategory.SOUND);
    }

    @Override public int cooldownTicks() { return 8000; } // ~6.6 min
    @Override public int pressureCost() { return 1; }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.isUnderground()
                && ctx.mindState().caveResonance > 30f
                && ctx.progressionStage() >= 1;
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        return 1.4f + ctx.mindState().caveResonance * 0.02f;
    }

    @Override
    public void execute(AnomalyContext ctx) {
        var player = ctx.player();
        // From somewhere in the surrounding stone, slightly below.
        Vec3 from = player.position().add(
                (player.getRandom().nextDouble() - 0.5) * 8.0,
                -(2.0 + player.getRandom().nextInt(4)),
                (player.getRandom().nextDouble() - 0.5) * 8.0);
        SoundScheduler.playOnce(player, ModSounds.CAVE_RESONANCE, from, 0.7f,
                0.9f + player.getRandom().nextFloat() * 0.15f);
    }
}
