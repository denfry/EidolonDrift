package com.denfry.eidolondrift.anomaly;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.sound.CaveResonanceAnomaly;
import com.denfry.eidolondrift.anomaly.sound.DistantVoiceAnomaly;
import com.denfry.eidolondrift.anomaly.sound.HouseCreakAnomaly;
import com.denfry.eidolondrift.anomaly.sound.PhantomFootstepsAnomaly;
import com.denfry.eidolondrift.anomaly.sound.WhisperAnomaly;
import com.denfry.eidolondrift.director.AnomalyRegistry;

/**
 * The one place that knows the concrete anomaly classes. Called once at common setup so
 * anomalies self-register into {@link AnomalyRegistry}; the Director never imports them
 * (invariant). Each new anomaly adds a single line here.
 */
public final class ModAnomalies {

    private ModAnomalies() {}

    public static void bootstrap() {
        // Layer 4 — sound (M1).
        AnomalyRegistry.register(new PhantomFootstepsAnomaly());
        AnomalyRegistry.register(new WhisperAnomaly());
        AnomalyRegistry.register(new DistantVoiceAnomaly());
        AnomalyRegistry.register(new HouseCreakAnomaly());
        AnomalyRegistry.register(new CaveResonanceAnomaly());

        EidolonDrift.LOGGER.info("Registered {} anomalies.", AnomalyRegistry.size());
    }
}
