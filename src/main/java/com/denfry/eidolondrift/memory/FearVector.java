package com.denfry.eidolondrift.memory;

/**
 * The dominant axis of a player's fear, derived from their real habits (GDD §6).
 * Drives which anomaly categories the Director weights highest for this player.
 */
public enum FearVector {
    /** Darkness / low light. */
    DARK,
    /** Deep underground, enclosed spaces. */
    CAVE,
    /** Being around (or watched by) others; or, inverted, isolation. */
    SOCIAL,
    /** Attachment to and safety of the home. */
    HOME,
    /** Wide open exposure, the surface at night. */
    OPEN,
    /** Not enough data yet. */
    NONE
}
