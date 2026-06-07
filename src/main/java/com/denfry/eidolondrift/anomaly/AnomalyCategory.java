package com.denfry.eidolondrift.anomaly;

/**
 * Broad kind of an anomaly (GDD §5). Used for weighting, accessibility gating, and
 * (later) per-category config toggles.
 */
public enum AnomalyCategory {
    SOUND,
    VISUAL,
    WORLD,
    HUD,
    ENTITY
}
