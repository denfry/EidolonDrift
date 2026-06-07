package com.denfry.eidolondrift.director;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.Anomaly;

import net.minecraft.resources.ResourceLocation;

/**
 * Central registry of all anomalies. Concrete anomalies self-register here at setup;
 * the {@link AnomalyDirector} only ever reads it, so it never hard-depends on any
 * concrete anomaly (invariant).
 */
public final class AnomalyRegistry {

    private static final Map<ResourceLocation, Anomaly> REGISTRY = new LinkedHashMap<>();

    private AnomalyRegistry() {}

    public static void register(Anomaly anomaly) {
        Anomaly prev = REGISTRY.putIfAbsent(anomaly.id(), anomaly);
        if (prev != null) {
            EidolonDrift.LOGGER.warn("Duplicate anomaly id ignored: {}", anomaly.id());
        }
    }

    public static Collection<Anomaly> all() {
        return REGISTRY.values();
    }

    public static Optional<Anomaly> byId(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static int size() {
        return REGISTRY.size();
    }
}
