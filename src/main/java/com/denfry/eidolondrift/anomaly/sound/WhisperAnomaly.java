package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.world.phys.Vec3;

/**
 * A close, wordless whisper just off the player's shoulder. Fires when the player is
 * alone and a little frayed — the world acknowledging it is listening.
 */
public final class WhisperAnomaly extends AbstractAnomaly {

    public WhisperAnomaly() {
        super("whisper", AnomalyCategory.SOUND);
    }

    @Override public int cooldownTicks() { return 7200; } // 6 min
    @Override public int pressureCost() { return 1; }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.mindState().isolation > 30f
                && ctx.progressionStage() >= 1;
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        float w = 1.5f + ctx.mindState().isolation * 0.02f + ctx.mindState().dread * 0.01f;
        if (ctx.isNight()) w *= 1.3f;
        return w;
    }

    @Override
    public void execute(AnomalyContext ctx) {
        var player = ctx.player();
        // 1–3 blocks behind, roughly at head height.
        Vec3 near = player.getLookAngle().scale(-(1 + player.getRandom().nextInt(3)))
                .add(player.getEyePosition());
        SoundScheduler.playOnce(player, ModSounds.WHISPER, near, 0.6f,
                0.95f + player.getRandom().nextFloat() * 0.1f);
    }
}
