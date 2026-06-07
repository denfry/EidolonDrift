/**
 * Client-only layer (render/hud/audio subpackages). Receives unicast trigger packets and
 * builds self-expiring {@code ClientAnomalyInstance}s — the server never knows what is
 * rendered. Anomalies auto-expire via tick counter and cancel on &gt;30 blocks; nothing
 * restored on relog. Accessibility toggles live here: {@code disable_flashing_effects}
 * must kill all flash content (invariant §6). Empty until M4.
 */
package com.denfry.eidolondrift.client;
