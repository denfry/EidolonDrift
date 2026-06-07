/**
 * Layer 5 — The Observer (GDD §8). Not a boss: no loot, no HP bar. A phase machine
 * ({@link com.denfry.eidolondrift.observer.ObserverPhase ABSENT→…→FINAL}) that reacts to being
 * looked at and retreats from light &gt; 11 before INVITED. Cannot break blocks (no grief,
 * invariant §8) and carries no goal AI, so there is no pathfinding to exploit.
 *
 * <p>M2 ships the arc through {@code SEEN}. The Observer is a <b>server-real</b>
 * {@link com.denfry.eidolondrift.observer.ObserverEntity} that is rendered for exactly one player
 * — {@link com.denfry.eidolondrift.observer.ObserverEntity#broadcastToPlayer} keeps it off every
 * other client (and their minimaps), satisfying invariant §2 without the M4 client-anomaly
 * channel. {@link com.denfry.eidolondrift.observer.ObserverSpawnManager} owns its life-cycle,
 * deriving the owed phase from {@link com.denfry.eidolondrift.mind.MindState}.
 */
package com.denfry.eidolondrift.observer;
