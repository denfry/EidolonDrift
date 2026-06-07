package com.denfry.eidolondrift.compat;

import java.util.EnumMap;
import java.util.Map;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.config.ModConfig;

import net.neoforged.fml.ModList;

/**
 * Runtime registry of which partner mods are installed, and whether each integration is active.
 *
 * <p><b>Presence</b> (is the partner mod loaded?) is detected once at {@code FMLCommonSetupEvent}
 * and cached — the mod list never changes after load. <b>Activeness</b> (presence + config) is
 * computed live so {@code /eidolon reload} takes effect without a restart.
 *
 * <p>Every callsite that wants to do something mod-specific MUST guard on {@link #isActive} so the
 * mod stays a no-op (and class-load-safe) when the partner is absent — invariant: standalone first.
 */
public final class ModIntegrations {

    private static final Map<Integration, Boolean> PRESENT = new EnumMap<>(Integration.class);
    private static boolean detected = false;

    private ModIntegrations() {}

    /**
     * Detect installed partner mods once, after all mods have constructed. Reads only the mod list
     * (no game/config state), so it is safe to call from common setup. Idempotent.
     */
    public static void detect() {
        ModList list = ModList.get();
        int found = 0;
        for (Integration in : Integration.values()) {
            boolean loaded = list.isLoaded(in.modId);
            PRESENT.put(in, loaded);
            if (loaded) {
                found++;
                EidolonDrift.LOGGER.info("Eidolon Drift: integration available — '{}' ({}).", in.key, in.modId);
            }
        }
        detected = true;
        EidolonDrift.LOGGER.info("Eidolon Drift: {} of {} optional integrations detected.",
                found, Integration.values().length);
    }

    /** True if the partner mod is installed, regardless of config. */
    public static boolean isPresent(Integration in) {
        return Boolean.TRUE.equals(PRESENT.get(in));
    }

    /**
     * True if the partner mod is installed AND enabled (master switch + per-mod toggle). Read live.
     * This is the gate every integration hook must check before touching partner-mod behaviour.
     */
    public static boolean isActive(Integration in) {
        return isPresent(in) && ModConfig.INTEGRATIONS_ENABLED.get() && in.configEnabled();
    }

    /** Whether {@link #detect()} has run yet (false very early in startup). */
    public static boolean isDetected() {
        return detected;
    }
}
