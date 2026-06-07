package com.denfry.eidolondrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.anomaly.client.RoomDesaturateAnomaly;
import com.denfry.eidolondrift.anomaly.client.ShadowInCornerAnomaly;
import com.denfry.eidolondrift.anomaly.sound.ChestClickNoPlayerAnomaly;
import com.denfry.eidolondrift.anomaly.sound.FootstepsAboveAnomaly;
import com.denfry.eidolondrift.anomaly.sound.FootstepsOutsideAnomaly;
import com.denfry.eidolondrift.anomaly.world.BedRefusesSleepAnomaly;
import com.denfry.eidolondrift.director.AnomalyMemory;
import com.denfry.eidolondrift.memory.FearVector;
import com.denfry.eidolondrift.memory.HomeMemory;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.mind.MindState;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/** Home zone geometry + Home-Horror gating/weighting, with no live player (M3). */
class HomeLogicTest {

    private static AnomalyContext ctx(MindState ms, boolean nearHome, PlayerWorldMemory mem) {
        return new AnomalyContext(null, ms, ms.progressionStage, null, 0L, 0,
                false, false, nearHome, nearHome ? 10.0 : 1000.0, mem, new AnomalyMemory());
    }

    private static MindState stage(int s) {
        MindState ms = new MindState();
        ms.progressionStage = s;
        return ms;
    }

    /** A memory with a logged home anchor (i.e. the player has slept). */
    private static PlayerWorldMemory homed() {
        PlayerWorldMemory m = new PlayerWorldMemory();
        m.primaryBedPos = Optional.of(new BlockPos(8, 64, 8));
        return m;
    }

    @Test
    void homeZoneGeometry() {
        HomeMemory home = homed().home();
        assertTrue(home.hasHome());
        assertTrue(home.contains(new BlockPos(8, 64, 8)), "anchor itself");
        assertTrue(home.contains(new BlockPos(8 + HomeMemory.HALF_EXTENT_XZ, 64, 8)), "edge of XZ extent");
        assertFalse(home.contains(new BlockPos(8 + HomeMemory.HALF_EXTENT_XZ + 1, 64, 8)), "just past XZ");
        assertFalse(home.contains(new BlockPos(8, 64 + HomeMemory.HALF_EXTENT_Y + 1, 8)), "just past Y");

        HomeMemory none = new PlayerWorldMemory().home();
        assertFalse(none.hasHome());
        assertFalse(none.contains(BlockPos.ZERO), "no home contains nothing");
        assertEquals(null, none.zone());
    }

    @Test
    void chestClickGating() {
        var a = new ChestClickNoPlayerAnomaly();
        assertTrue(a.canFire(ctx(stage(2), true, homed())), "stage>=2 & near a logged home");
        assertFalse(a.canFire(ctx(stage(1), true, homed())), "stage too low");
        assertFalse(a.canFire(ctx(stage(2), false, homed())), "not near home");
        assertFalse(a.canFire(ctx(stage(2), true, new PlayerWorldMemory())), "no home logged yet");
    }

    @Test
    void stageThresholdsAcrossTheLadder() {
        assertTrue(new FootstepsOutsideAnomaly().canFire(ctx(stage(2), true, homed())), "heard-outside @ stage 2");
        assertFalse(new FootstepsAboveAnomaly().canFire(ctx(stage(3), true, homed())), "footsteps-above need stage 4");
        assertTrue(new FootstepsAboveAnomaly().canFire(ctx(stage(4), true, homed())));
        assertTrue(new BedRefusesSleepAnomaly().canFire(ctx(stage(3), true, homed())), "bed refusal @ stage 3");
        assertTrue(new ShadowInCornerAnomaly().canFire(ctx(stage(4), true, homed())));
        assertFalse(new ShadowInCornerAnomaly().canFire(ctx(stage(3), true, homed())), "shadow needs stage 4");
    }

    @Test
    void homeFearBoostsWeight() {
        var a = new ChestClickNoPlayerAnomaly();
        MindState ms = stage(2);
        ms.homeCorruption = 20f;

        PlayerWorldMemory neutral = homed();
        neutral.fearProfile.primaryFear = FearVector.NONE;
        float base = a.baseWeight(ctx(ms, true, neutral));

        PlayerWorldMemory homeFear = homed();
        homeFear.fearProfile.primaryFear = FearVector.HOME;
        float boosted = a.baseWeight(ctx(ms, true, homeFear));

        assertTrue(boosted > base, "HOME fear must raise home-anomaly weight");
        assertEquals(base * 1.6f, boosted, 1e-4);
    }

    @Test
    void clientVisualsAreClientSide() {
        assertTrue(new ShadowInCornerAnomaly().isClientSide());
        assertTrue(new RoomDesaturateAnomaly().isClientSide());
        assertFalse(new ChestClickNoPlayerAnomaly().isClientSide(), "sound anomalies run server-side");
    }
}
