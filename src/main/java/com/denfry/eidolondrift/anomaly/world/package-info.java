/**
 * Home-Horror "world" anomalies (GDD §7, Stages 2–4). Functional events (bed refuses sleep)
 * and config-gated, always-revertible physical changes (door, furnace, torch, sign) — every
 * physical mutation lives behind {@code world_mutation_allowed} (invariant §1) and never
 * destroys player property (invariant §8).
 */
package com.denfry.eidolondrift.anomaly.world;
