/**
 * Layer 2 — World Memory (GDD §6). Server {@code SavedData} keyed by player UUID plus
 * per-chunk/BE attachments record real habits (routes, homes, mining, deaths). Records
 * but never destroys property; home AABB read-only for destruction. {@code FearProfile}
 * recalculates on login + every 10 min and drives anomaly weighting. Empty until M1.
 */
package com.denfry.eidolondrift.memory;
