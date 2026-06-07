/**
 * Layer 8 — Player Echo (GDD §9). A ghost of the player's own past behaviour replayed
 * from an {@code EchoSnapshot} ring buffer (≤512, captured every 60 ticks / on action).
 * MEMORY + ROUTINE types for 1.0; deals no damage; visible only to the target player;
 * capped at ≤3 per player. Empty until M5.
 */
package com.denfry.eidolondrift.echo;
