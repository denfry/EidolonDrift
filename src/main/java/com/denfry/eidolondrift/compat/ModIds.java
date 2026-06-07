package com.denfry.eidolondrift.compat;

/**
 * Mod-id constants for every partner mod Eidolon Drift can integrate with.
 *
 * <p>These are the actual {@code modId}s used by {@link net.neoforged.fml.ModList#isLoaded(String)}
 * — NOT Modrinth/CurseForge slugs. A wrong value here makes an integration silently never fire,
 * so this table is the single place to correct them. Verified against the mods' published metadata
 * for Minecraft 1.21.1 / NeoForge where possible; entries flagged {@code VERIFY} should be
 * double-checked against the partner mod's {@code neoforge.mods.toml} if integration won't engage.
 */
public final class ModIds {

    private ModIds() {}

    /** Sound Physics Remastered — realistic reverb / occlusion for positional sounds. */
    public static final String SOUND_PHYSICS_REMASTERED = "sound_physics_remastered";

    /** AmbientSounds (CreativeMD) — biome/situation ambient soundscape. */
    public static final String AMBIENTSOUNDS = "ambientsounds";

    /** Dynamic Surroundings (OreCruncher) — ambient sound + reverb. VERIFY modid for 1.21.1. */
    public static final String DYNAMIC_SURROUNDINGS = "dsurround";

    /** Simple Voice Chat — proximity voice; whisper-through-voice integration target. */
    public static final String VOICECHAT = "voicechat";

    /** Distant Horizons — LOD render distance; amplifies isolation / agoraphobia. */
    public static final String DISTANT_HORIZONS = "distanthorizons";

    /** Terralith — atmospheric overworld worldgen (Stardust Labs). */
    public static final String TERRALITH = "terralith";

    /** Tectonic — large-scale terrain shaping (Stardust Labs). */
    public static final String TECTONIC = "tectonic";
}
