package com.denfry.eidolondrift.memory;

import com.denfry.eidolondrift.mind.MindState;

/**
 * Personalisation engine output (GDD §6): the player's primary fear axis plus how
 * predictable ("habit strength") they are. Derived — recalculated on login and
 * periodically — so it is not serialized; it is rebuilt from {@link PlayerWorldMemory}
 * and the current {@link MindState}.
 */
public final class FearProfile {

    public FearVector primaryFear = FearVector.NONE;
    /** 0–1: how predictable / routine-bound this player is. */
    public float habitStrength;

    /**
     * Recompute from accumulated memory + current mind state. Intentionally simple
     * for M1 — it just needs to bias weighting; richer heuristics come with more
     * recorded signals (mining depth, biome avoidance, death clustering) in later layers.
     */
    public void recalculate(PlayerWorldMemory mem, MindState ms) {
        // Habit strength rises with how settled (routine) and home-bound the player is.
        habitStrength = MindState.clamp(ms.routine * 0.6f + Math.min(mem.timesSlept, 40L) * 1.0f) / 100f;

        // Pick the strongest pressure axis. Ties resolve by this priority order.
        float bestScore = 8f; // floor: below this we stay NONE (deniability early on)
        FearVector best = FearVector.NONE;

        float home = ms.homeCorruption + ms.attachment * 0.5f + mem.timesSlept;
        if (home > bestScore) { bestScore = home; best = FearVector.HOME; }

        float cave = ms.caveResonance;
        if (cave > bestScore) { bestScore = cave; best = FearVector.CAVE; }

        float social = ms.isolation;
        if (social > bestScore) { bestScore = social; best = FearVector.SOCIAL; }

        float dark = ms.sleepDebt * 0.7f + ms.dread * 0.3f;
        if (dark > bestScore) { bestScore = dark; best = FearVector.DARK; }

        this.primaryFear = best;
    }
}
