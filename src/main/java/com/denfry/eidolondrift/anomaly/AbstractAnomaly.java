package com.denfry.eidolondrift.anomaly;

import com.denfry.eidolondrift.EidolonDrift;

import net.minecraft.resources.ResourceLocation;

/**
 * Convenience base for concrete anomalies: caches the id and category and supplies a
 * sensible debug label. Subclasses still implement {@link #canFire}/{@link #execute}.
 */
public abstract class AbstractAnomaly implements Anomaly {

    private final ResourceLocation id;
    private final AnomalyCategory category;

    protected AbstractAnomaly(String path, AnomalyCategory category) {
        this.id = EidolonDrift.RL(path);
        this.category = category;
    }

    @Override
    public final ResourceLocation id() {
        return id;
    }

    @Override
    public final AnomalyCategory category() {
        return category;
    }
}
