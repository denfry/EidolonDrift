package com.denfry.eidolondrift.anomaly.sound;

import com.denfry.eidolondrift.anomaly.AbstractAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.world.phys.Vec3;

/**
 * The house settling — a slow structural creak from above or the next room. Anchors the
 * "the house is learning" arc (GDD §7); weighted up for home-fearful players.
 */
public final class HouseCreakAnomaly extends AbstractAnomaly {

    public HouseCreakAnomaly() {
        super("house_creak", AnomalyCategory.SOUND);
    }

    @Override public int cooldownTicks() { return 6000; } // 5 min
    @Override public int pressureCost() { return 1; }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.isNearHome() && ctx.progressionStage() >= 1;
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        float w = 1.6f + ctx.mindState().homeCorruption * 0.02f;
        if (ctx.fearIsHome()) w *= 1.6f;
        return w;
    }

    @Override
    public void execute(AnomalyContext ctx) {
        var player = ctx.player();
        // Above and slightly to one side — like a floorboard overhead.
        Vec3 above = player.position().add(
                (player.getRandom().nextDouble() - 0.5) * 4.0,
                3.0 + player.getRandom().nextInt(2),
                (player.getRandom().nextDouble() - 0.5) * 4.0);
        SoundScheduler.playOnce(player, ModSounds.HOUSE_CREAK, above, 0.8f, 0.9f);
    }
}
