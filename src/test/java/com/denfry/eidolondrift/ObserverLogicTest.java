package com.denfry.eidolondrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.denfry.eidolondrift.mind.MindState;
import com.denfry.eidolondrift.observer.ObserverPhase;
import com.denfry.eidolondrift.util.PlayerLookUtil;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Pure Observer logic (GDD §8): phase transitions + look-detection maths, no live entity. */
class ObserverLogicTest {

    private static MindState mind(int stage, float attachment, float routine,
                                  float distortion, float homeCorruption) {
        MindState ms = new MindState();
        ms.progressionStage = stage;
        ms.attachment = attachment;
        ms.routine = routine;
        ms.distortion = distortion;     // set directly here; derived in-game, but this is a pure test
        ms.homeCorruption = homeCorruption;
        return ms;
    }

    // ── phase transition table (GDD §8) ──────────────────────────────────────────

    @Test
    void hardStageFloorGatesEveryPhase() {
        // All parameters maxed, but Stage 0 → still ABSENT (stage is a hard floor).
        assertEquals(ObserverPhase.ABSENT,
                ObserverPhase.autoFor(mind(0, 100, 100, 100, 100), 1.0));
    }

    @Test
    void autoForFollowsTheTable() {
        assertEquals(ObserverPhase.IMPLIED, ObserverPhase.autoFor(mind(1, 0, 0, 10, 0), 1.0));
        assertEquals(ObserverPhase.PERIPHERAL, ObserverPhase.autoFor(mind(2, 40, 0, 25, 0), 1.0));
        assertEquals(ObserverPhase.SEEN, ObserverPhase.autoFor(mind(3, 50, 60, 35, 0), 1.0));
        // Just under each threshold collapses to the phase below.
        assertEquals(ObserverPhase.ABSENT, ObserverPhase.autoFor(mind(1, 0, 0, 9, 0), 1.0));
        assertEquals(ObserverPhase.IMPLIED, ObserverPhase.autoFor(mind(2, 39, 0, 12, 0), 1.0));
    }

    @Test
    void aggressionSoftensParameterThresholdsButNotStage() {
        // attachment 20 normally fails PERIPHERAL (needs 40); aggression 2.0 halves the bar.
        assertEquals(ObserverPhase.PERIPHERAL, ObserverPhase.autoFor(mind(2, 20, 0, 12, 0), 2.0));
        // …but a stage-1 mind can never reach an entity phase no matter how aggressive.
        assertEquals(ObserverPhase.IMPLIED, ObserverPhase.autoFor(mind(1, 100, 100, 100, 0), 10.0));
    }

    @Test
    void milestoneCapNeverExceedsSeen() {
        // Stage 4 + homeCorruption 30 derives NEAR, but M2 only drives through SEEN.
        ObserverPhase near = ObserverPhase.autoFor(mind(4, 50, 70, 45, 30), 1.0);
        assertEquals(ObserverPhase.NEAR, near);
        assertEquals(ObserverPhase.SEEN, near.cappedToImplemented());
        assertEquals(ObserverPhase.SEEN, ObserverPhase.SEEN.cappedToImplemented());
    }

    @Test
    void phaseCapabilityFlags() {
        assertFalse(ObserverPhase.IMPLIED.hasEntity());
        assertTrue(ObserverPhase.PERIPHERAL.hasEntity());
        assertTrue(ObserverPhase.SEEN.hasEntity());
        // Retreats from light only before being INVITED inside (GDD §8).
        assertTrue(ObserverPhase.SEEN.retreatsFromLight());
        assertTrue(ObserverPhase.NEAR.retreatsFromLight());
        assertFalse(ObserverPhase.INVITED.retreatsFromLight());
        assertFalse(ObserverPhase.INSIDE.retreatsFromLight());
    }

    // ── look detection (PlayerLookUtil) ──────────────────────────────────────────

    @Test
    void staringDownTheAxisIsSeen() {
        Vec3 eye = new Vec3(0, 1.6, 0);
        Vec3 lookPlusX = new Vec3(1, 0, 0);
        assertTrue(PlayerLookUtil.isLookingToward(eye, lookPlusX, new Vec3(10, 1.6, 0), 0.12),
                "looking straight at a target 10 blocks ahead");
        assertFalse(PlayerLookUtil.isLookingToward(eye, lookPlusX, new Vec3(0, 1.6, 10), 0.12),
                "target 90° to the side is not 'looked at'");
    }

    @Test
    void toleranceWidensWhenClose() {
        Vec3 eye = new Vec3(0, 1.6, 0);
        Vec3 look = new Vec3(1, 0, 0);
        // A small lateral offset at 3 blocks: forgiving. The same angle at 60 blocks: not.
        Vec3 near = new Vec3(3, 1.6, 0.25);
        Vec3 far = new Vec3(60, 1.6, 5.0);
        assertTrue(PlayerLookUtil.isLookingToward(eye, look, near, 0.12));
        assertFalse(PlayerLookUtil.isLookingToward(eye, look, far, 0.12));
    }
}
