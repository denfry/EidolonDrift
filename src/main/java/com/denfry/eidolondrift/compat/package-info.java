/**
 * Optional mod-integration layer (soft dependencies only). Eidolon Drift never hard-depends
 * on another mod: every integration is detected at runtime via {@link net.neoforged.fml.ModList}
 * and gated behind the {@code [integrations]} config section. If a partner mod is absent or the
 * toggle is off, the integration is a no-op and the mod behaves exactly as standalone.
 *
 * <p>This is the foundation only — {@link com.denfry.eidolondrift.compat.Integration} enumerates
 * the supported partner mods and {@link com.denfry.eidolondrift.compat.ModIntegrations} answers
 * "is this integration active?". Concrete behaviour (reverb-aware sounds, ambient ducking,
 * voice-chat whispers, worldgen-driven dread) plugs in here in later milestones.
 *
 * <p>Layering: {@code compat} may depend on {@code config} and the foundation only; partner-mod
 * behaviour must remain class-load-safe when the partner is absent (no eager imports of their API).
 */
package com.denfry.eidolondrift.compat;
