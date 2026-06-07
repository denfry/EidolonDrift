package com.denfry.eidolondrift.anomaly;

import net.minecraft.resources.ResourceLocation;

/**
 * One discrete horror event (GDD §5/§19). Concrete anomalies <b>self-register</b> into
 * the registry — the Director must never hard-depend on a concrete anomaly (invariant).
 *
 * <p>By default anomalies are illusions and must not mutate real world state
 * (invariant §1). Anything physical lives behind the {@code world_mutation_allowed} gate.
 */
public interface Anomaly {

    /** Stable id, namespaced {@code eidolon_drift:}. Also the cooldown/history key. */
    ResourceLocation id();

    AnomalyCategory category();

    /** Relative selection weight for this context (already folds in fear-profile bias). */
    float baseWeight(AnomalyContext ctx);

    /** Hard gate: may this anomaly fire at all right now? Cheap checks only. */
    boolean canFire(AnomalyContext ctx);

    /** Minimum ticks before this same anomaly may fire again for the player. */
    int cooldownTicks();

    /** Cost charged against the wave's pressure budget (GDD §5). */
    int pressureCost();

    /** Produce the effect. Runs on the server; client illusions go out as unicast packets. */
    void execute(AnomalyContext ctx);

    /** True if the visible effect is client-side only (no server world change). */
    default boolean isClientSide() {
        return false;
    }

    default String getDebugLabel() {
        return id().toString();
    }
}
