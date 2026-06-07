/**
 * Layer 3 — Director core (GDD §5). The "horror filmmaker": weighted-random selection
 * over {@code canFire} candidates within a pressure budget, honouring cooldowns,
 * {@code firedRecently}, MIN_GAP (5 min), session budget (≤3), and protected windows.
 * Ticks every 400 ticks — never per-frame. Must not hard-depend on concrete anomalies;
 * they self-register. Keep deterministic-testable (inject RNG). Empty until M1.
 */
package com.denfry.eidolondrift.director;
