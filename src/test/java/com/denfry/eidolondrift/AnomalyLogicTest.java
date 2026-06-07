package com.denfry.eidolondrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.anomaly.sound.CaveResonanceAnomaly;
import com.denfry.eidolondrift.anomaly.sound.PhantomFootstepsAnomaly;
import com.denfry.eidolondrift.anomaly.sound.WhisperAnomaly;
import com.denfry.eidolondrift.director.AnomalyMemory;
import com.denfry.eidolondrift.director.EscalationLadder;
import com.denfry.eidolondrift.memory.FearVector;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.mind.MindState;

import org.junit.jupiter.api.Test;

/** Pure gating/weighting logic for the Director + sound anomalies (no live player). */
class AnomalyLogicTest {

    private static AnomalyContext ctx(MindState ms, boolean nearHome, boolean underground,
                                      PlayerWorldMemory mem) {
        return new AnomalyContext(null, ms, ms.progressionStage, null, 0L, 0,
                underground, false, nearHome, nearHome ? 10.0 : 1000.0, mem, new AnomalyMemory());
    }

    private static MindState mind(int stage, float isolation, float cave) {
        MindState ms = new MindState();
        ms.progressionStage = stage;
        ms.isolation = isolation;
        ms.caveResonance = cave;
        return ms;
    }

    @Test
    void phantomFootstepsGating() {
        var a = new PhantomFootstepsAnomaly();
        var mem = new PlayerWorldMemory();
        assertTrue(a.canFire(ctx(mind(1, 50, 0), true, false, mem)), "isolation>40 & stage>=1 & nearHome");
        assertFalse(a.canFire(ctx(mind(1, 50, 0), false, false, mem)), "not near home");
        assertFalse(a.canFire(ctx(mind(1, 30, 0), true, false, mem)), "isolation too low");
        assertFalse(a.canFire(ctx(mind(0, 50, 0), true, false, mem)), "stage 0 protected");
    }

    @Test
    void whisperGating() {
        var a = new WhisperAnomaly();
        var mem = new PlayerWorldMemory();
        assertTrue(a.canFire(ctx(mind(1, 31, 0), false, false, mem)));
        assertFalse(a.canFire(ctx(mind(1, 20, 0), false, false, mem)));
        assertFalse(a.canFire(ctx(mind(0, 90, 0), false, false, mem)));
    }

    @Test
    void caveResonanceNeedsUndergroundAndResonance() {
        var a = new CaveResonanceAnomaly();
        var mem = new PlayerWorldMemory();
        assertTrue(a.canFire(ctx(mind(1, 0, 40), false, true, mem)));
        assertFalse(a.canFire(ctx(mind(1, 0, 40), false, false, mem)), "must be underground");
        assertFalse(a.canFire(ctx(mind(1, 0, 10), false, true, mem)), "resonance too low");
    }

    @Test
    void homeFearBoostsFootstepWeight() {
        var a = new PhantomFootstepsAnomaly();
        MindState ms = mind(2, 60, 0);

        var neutral = new PlayerWorldMemory();
        neutral.fearProfile.primaryFear = FearVector.NONE;
        float base = a.baseWeight(ctx(ms, true, false, neutral));

        var homeFear = new PlayerWorldMemory();
        homeFear.fearProfile.primaryFear = FearVector.HOME;
        float boosted = a.baseWeight(ctx(ms, true, false, homeFear));

        assertTrue(boosted > base, "HOME fear must raise footstep weight (" + boosted + " > " + base + ")");
        assertEquals(base * 1.8f, boosted, 1e-4);
    }

    @Test
    void escalationBudgetGrowsWithWave() {
        MindState calm = new MindState();
        assertEquals(1, EscalationLadder.pressureBudgetForWave(0, calm));
        assertTrue(EscalationLadder.pressureBudgetForWave(6, calm) >= EscalationLadder.pressureBudgetForWave(0, calm));

        MindState s5 = new MindState();
        s5.progressionStage = 5;
        s5.distortion = 70f;
        assertEquals(6, EscalationLadder.currentWave(ctx(s5, false, false, new PlayerWorldMemory())),
                "stage 5 + high distortion → wave 6");
    }
}
