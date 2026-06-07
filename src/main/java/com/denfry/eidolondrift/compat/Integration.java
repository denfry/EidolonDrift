package com.denfry.eidolondrift.compat;

import java.util.function.BooleanSupplier;

import com.denfry.eidolondrift.config.ModConfig;

/**
 * The catalogue of supported soft-dependency integrations. Each constant pairs a partner
 * {@link ModIds mod id} with the {@code [integrations]} config toggle that enables it and the
 * horror dimension it serves. Detection lives in {@link ModIntegrations}; this enum carries no
 * behaviour — concrete hooks are wired in later milestones (foundation only).
 *
 * <p>Config toggles are read through a {@link BooleanSupplier} so the value is fetched live
 * (honouring {@code /eidolon reload}) and never captured at class-init time.
 */
public enum Integration {

    /** Reverb/occlusion for whispers & footsteps — the cheapest, highest-ROI horror amplifier. */
    SOUND_PHYSICS("sound_physics", ModIds.SOUND_PHYSICS_REMASTERED, Category.SOUND,
            () -> ModConfig.INT_SOUND_PHYSICS.get()),

    /** Director can duck the ambient bed before an anomaly ("the silence before the horror"). */
    AMBIENT_SOUNDS("ambient_sounds", ModIds.AMBIENTSOUNDS, Category.SOUND,
            () -> ModConfig.INT_AMBIENT_SOUNDS.get()),

    /** Same ducking target as AmbientSounds, alternate provider. */
    DYNAMIC_SURROUNDINGS("dynamic_surroundings", ModIds.DYNAMIC_SURROUNDINGS, Category.SOUND,
            () -> ModConfig.INT_DYNAMIC_SURROUNDINGS.get()),

    /** Whisper-as-proximity-voice when the player is alone (needs SVC API in a later milestone). */
    SIMPLE_VOICE_CHAT("simple_voice_chat", ModIds.VOICECHAT, Category.VOICE,
            () -> ModConfig.INT_SIMPLE_VOICE_CHAT.get()),

    /** Far LOD horizons feed the isolation metric; client anomalies must not fight its rendering. */
    DISTANT_HORIZONS("distant_horizons", ModIds.DISTANT_HORIZONS, Category.VISUAL,
            () -> ModConfig.INT_DISTANT_HORIZONS.get()),

    /** Unfamiliar biomes raise dread; deep generation feeds caveResonance (worldgen-aware Director). */
    TERRALITH("terralith", ModIds.TERRALITH, Category.WORLDGEN,
            () -> ModConfig.INT_TERRALITH.get()),

    /** Large lonely terrain amplifies isolation; datapack-compatible weighting hints. */
    TECTONIC("tectonic", ModIds.TECTONIC, Category.WORLDGEN,
            () -> ModConfig.INT_TECTONIC.get());

    /** Which immersion dimension the integration serves (display/grouping only). */
    public enum Category { SOUND, VOICE, VISUAL, WORLDGEN }

    /** Stable key used both as the config option name and the {@code /eidolon integrations} label. */
    public final String key;
    /** Partner mod id to detect via {@link net.neoforged.fml.ModList}. */
    public final String modId;
    public final Category category;
    private final BooleanSupplier configEnabled;

    Integration(String key, String modId, Category category, BooleanSupplier configEnabled) {
        this.key = key;
        this.modId = modId;
        this.category = category;
        this.configEnabled = configEnabled;
    }

    /** Per-mod config toggle, read live. */
    boolean configEnabled() {
        return configEnabled.getAsBoolean();
    }
}
