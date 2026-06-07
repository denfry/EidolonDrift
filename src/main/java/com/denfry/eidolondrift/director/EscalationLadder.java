package com.denfry.eidolondrift.director;

import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.mind.MindState;

/**
 * Maps the current situation to a "pressure wave" and a budget for it (GDD §5). Higher
 * stage + higher distortion → bigger waves. Kept pure/deterministic so the Director's
 * selection is unit-testable.
 */
public final class EscalationLadder {

    private EscalationLadder() {}

    /** Current wave index (0..9), rising with progression stage and acute distortion. */
    public static int currentWave(AnomalyContext ctx) {
        int wave = ctx.progressionStage();
        if (ctx.mindState().distortion >= 60f) wave += 1;
        return Math.max(0, Math.min(9, wave));
    }

    /**
     * Pressure budget available this evaluation. Sound anomalies cost 1, so early stages
     * fire at most one cheap event; later waves can afford heavier combinations.
     */
    public static int pressureBudgetForWave(int wave, MindState ms) {
        int budget = 1 + wave / 2;
        if (ms.dread >= 70f) budget += 1; // acute dread opens the throttle slightly
        return Math.max(1, budget);
    }
}
