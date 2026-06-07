package com.denfry.eidolondrift.observer;

import com.denfry.eidolondrift.mind.MindState;

/**
 * The Observer's life-cycle (GDD §8). Not a difficulty curve — a slow confirmation that
 * "something is paying attention". Each phase has a transition condition keyed off the
 * player's {@link MindState} (stage + a single acute/place parameter), and the phases above
 * {@link #INVITED} are reached only via ritual/ending paths, never auto-derived.
 *
 * <p>M2 ships the world-facing arc through {@link #SEEN} (see {@link #MAX_IMPLEMENTED}); the
 * later constants exist so persistence, rendering and the spawn manager are forward-compatible.
 */
public enum ObserverPhase {
    /** Does not exist (Stage 0–1). */
    ABSENT,
    /** Sound only, no entity — faint tones from the direction of a future spawn. */
    IMPLIED,
    /** Spawns beyond render range, drifts to the edge of vision; glimpsed and gone. */
    PERIPHERAL,
    /** Stands still and watches from a distance; vanishes if approached or looked at. */
    SEEN,
    /** Moves at night, follows at distance (M4+). */
    NEAR,
    /** Enters the home zone for the first time via Echo Door / ritual (M3+). */
    INVITED,
    /** Freely roams the home zone at night (M3+). */
    INSIDE,
    /** Copies the player on the Mirror path (M8). */
    MIRRORING,
    /** Confrontation for the endings (M8). */
    FINAL;

    /** Highest phase implemented this milestone; the spawn manager never exceeds it. */
    public static final ObserverPhase MAX_IMPLEMENTED = SEEN;

    private static final ObserverPhase[] VALUES = values();

    public static ObserverPhase byOrdinal(int ordinal) {
        if (ordinal < 0) return ABSENT;
        return ordinal >= VALUES.length ? FINAL : VALUES[ordinal];
    }

    /** A real, server-tracked entity exists from {@link #PERIPHERAL} upward. */
    public boolean hasEntity() {
        return ordinal() >= PERIPHERAL.ordinal();
    }

    /** Before being {@link #INVITED} in, bright light (&gt; 11) forces a temporary retreat (GDD §8). */
    public boolean retreatsFromLight() {
        return ordinal() < INVITED.ordinal();
    }

    /** Clamp this phase down to what the current milestone actually drives. */
    public ObserverPhase cappedToImplemented() {
        return ordinal() > MAX_IMPLEMENTED.ordinal() ? MAX_IMPLEMENTED : this;
    }

    /**
     * The phase the world is currently "owed" purely from {@link MindState}, following the GDD §8
     * transition table. {@code observerAggression} (config) softens the parameter thresholds — a
     * shier Observer (&lt; 1.0) needs more, a relentless one (&gt; 1.0) less — but never the hard
     * progression-stage floor. Returns the <i>auto-derivable</i> phases only (ABSENT…INSIDE);
     * {@link #INVITED}/{@link #MIRRORING}/{@link #FINAL} are path-driven and excluded.
     */
    public static ObserverPhase autoFor(MindState ms, double observerAggression) {
        double aggro = clampAggro(observerAggression);
        if (qualifies(ms, 6, ms.homeCorruption, 70f, aggro)) return INSIDE;
        if (qualifies(ms, 4, ms.homeCorruption, 30f, aggro)) return NEAR;
        if (qualifies(ms, 3, ms.routine, 60f, aggro)) return SEEN;
        if (qualifies(ms, 2, ms.attachment, 40f, aggro)) return PERIPHERAL;
        if (qualifies(ms, 1, ms.distortion, 10f, aggro)) return IMPLIED;
        return ABSENT;
    }

    private static boolean qualifies(MindState ms, int minStage, float value,
                                     float baseThreshold, double aggro) {
        if (ms.progressionStage < minStage) return false;        // hard stage floor (GDD §8)
        return value >= baseThreshold / aggro;                   // aggression softens the parameter
    }

    private static double clampAggro(double a) {
        if (Double.isNaN(a) || a < 0.1) return 0.1;
        return Math.min(a, 10.0);
    }
}
