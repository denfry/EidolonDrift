package com.denfry.eidolondrift.config;

import java.util.List;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration — mirrors {@code eidolon_drift-common.toml} from GDD §17.
 *
 * <p>Registered as a COMMON config so it loads on both client and dedicated server.
 * Accessibility toggles ({@link #DISABLE_FLASHING_EFFECTS}, etc.) are safety-critical:
 * {@code disable_flashing_effects} MUST kill all flash/strobe content (invariant §6).
 *
 * <p>For M0 the spec only loads cleanly; later layers read these values.
 */
public final class ModConfig {

    public static final ModConfigSpec SPEC;

    // ── general ─────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<String> HORROR_MODE;
    public static final ModConfigSpec.BooleanValue REQUIRE_OPT_IN;

    // ── horror_intensity ────────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue ANOMALY_FREQUENCY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STAGE_PROGRESSION_SPEED;
    public static final ModConfigSpec.DoubleValue OBSERVER_AGGRESSION;
    public static final ModConfigSpec.DoubleValue HOME_CORRUPTION_RATE;
    public static final ModConfigSpec.DoubleValue ECHO_SPAWN_CHANCE;

    // ── accessibility (safety-critical) ─────────────────────────────────────
    public static final ModConfigSpec.BooleanValue DISABLE_SCREEN_DISTORTION;
    public static final ModConfigSpec.BooleanValue DISABLE_FLASHING_EFFECTS;
    public static final ModConfigSpec.BooleanValue DISABLE_HUD_ANOMALIES;
    public static final ModConfigSpec.BooleanValue DISABLE_FAKE_SUBTITLES;
    public static final ModConfigSpec.BooleanValue REDUCE_AUDIO_HORROR;

    // ── observer ────────────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue OBSERVER_ENABLED;
    public static final ModConfigSpec.BooleanValue OBSERVER_CAN_ENTER_HOMES;
    public static final ModConfigSpec.BooleanValue OBSERVER_ATTACK_ON_HIT;
    public static final ModConfigSpec.IntValue OBSERVER_MIN_STAGE;
    public static final ModConfigSpec.IntValue OBSERVER_MAX_CONCURRENT;

    // ── world_memory ────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue MEMORY_DECAY_DAYS;
    public static final ModConfigSpec.IntValue MAX_ROUTE_SNAPSHOTS;
    public static final ModConfigSpec.IntValue MAX_DEATH_RECORDS;

    // ── home_corruption ─────────────────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue WORLD_MUTATION_ALLOWED;
    public static final ModConfigSpec.IntValue MAX_VISUAL_ANOMALY_RADIUS;

    // ── performance ─────────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue MAX_ECHOES_PER_PLAYER;
    public static final ModConfigSpec.IntValue MAX_CLIENT_ANOMALIES_PER_PLAYER;
    public static final ModConfigSpec.IntValue DIRECTOR_TICK_INTERVAL;

    // ── dimensions ──────────────────────────────────────────────────────────
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("general");
        ENABLED = b.comment("Master switch for the entire mod.").define("enabled", true);
        HORROR_MODE = b.comment("One of: soft_horror, default, hardcore, content_creator")
                .define("horror_mode", "default",
                        o -> o instanceof String s && List.of("soft_horror", "default", "hardcore", "content_creator").contains(s));
        REQUIRE_OPT_IN = b.comment("If true, players must run /eidolon join to be affected.")
                .define("require_opt_in", false);
        b.pop();

        b.push("horror_intensity");
        ANOMALY_FREQUENCY_MULTIPLIER = b.comment("0.0 = off; 2.0 = double speed.")
                .defineInRange("anomaly_frequency_multiplier", 1.0, 0.0, 10.0);
        STAGE_PROGRESSION_SPEED = b.defineInRange("stage_progression_speed", 1.0, 0.0, 10.0);
        OBSERVER_AGGRESSION = b.comment("0.5 = very shy; 2.0 = relentless.")
                .defineInRange("observer_aggression", 1.0, 0.0, 10.0);
        HOME_CORRUPTION_RATE = b.defineInRange("home_corruption_rate", 1.0, 0.0, 10.0);
        ECHO_SPAWN_CHANCE = b.defineInRange("echo_spawn_chance", 1.0, 0.0, 10.0);
        b.pop();

        b.push("accessibility");
        DISABLE_SCREEN_DISTORTION = b.comment("Disables shake, breathe, vignette.")
                .define("disable_screen_distortion", false);
        DISABLE_FLASHING_EFFECTS = b.comment("Epilepsy safety: disables ALL flash/strobe content.")
                .define("disable_flashing_effects", false);
        DISABLE_HUD_ANOMALIES = b.define("disable_hud_anomalies", false);
        DISABLE_FAKE_SUBTITLES = b.define("disable_fake_subtitles", false);
        REDUCE_AUDIO_HORROR = b.comment("Lowers all horror volumes by 60%.")
                .define("reduce_audio_horror", false);
        b.pop();

        b.push("observer");
        OBSERVER_ENABLED = b.define("enabled", true);
        OBSERVER_CAN_ENTER_HOMES = b.define("can_enter_homes", true);
        OBSERVER_ATTACK_ON_HIT = b.comment("If false, Observer only teleports away when hit.")
                .define("attack_on_hit", false);
        OBSERVER_MIN_STAGE = b.defineInRange("min_stage_to_spawn", 2, 0, 9);
        OBSERVER_MAX_CONCURRENT = b.defineInRange("max_concurrent_observers", 1, 0, 4);
        b.pop();

        b.push("world_memory");
        MEMORY_DECAY_DAYS = b.comment("Real-time days before old memory is pruned.")
                .defineInRange("memory_decay_days", 30, 1, 3650);
        MAX_ROUTE_SNAPSHOTS = b.defineInRange("max_route_snapshots", 512, 16, 4096);
        MAX_DEATH_RECORDS = b.defineInRange("max_death_records", 20, 0, 200);
        b.pop();

        b.push("home_corruption");
        WORLD_MUTATION_ALLOWED = b.comment("RECOMMENDED false: allow real physical block changes.")
                .define("allow_physical_block_changes", false);
        MAX_VISUAL_ANOMALY_RADIUS = b.defineInRange("max_visual_anomaly_radius", 16, 1, 64);
        b.pop();

        b.push("performance");
        MAX_ECHOES_PER_PLAYER = b.defineInRange("max_echoes_per_player", 3, 0, 16);
        MAX_CLIENT_ANOMALIES_PER_PLAYER = b.defineInRange("max_client_anomalies_per_player", 2, 0, 16);
        DIRECTOR_TICK_INTERVAL = b.comment("Ticks between director evaluations (never per-frame).")
                .defineInRange("director_tick_interval", 400, 100, 12000);
        b.pop();

        b.push("dimensions");
        DIMENSION_WHITELIST = b.comment("Dimensions where the mod is active (empty = all).")
                .defineListAllowEmpty("dimension_whitelist", List.of("minecraft:overworld"),
                        () -> "minecraft:overworld", o -> o instanceof String);
        DIMENSION_BLACKLIST = b.comment("Dimensions where the mod is fully disabled.")
                .defineListAllowEmpty("dimension_blacklist", List.of(),
                        () -> "minecraft:the_end", o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    private ModConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(Type.COMMON, SPEC);
    }
}
