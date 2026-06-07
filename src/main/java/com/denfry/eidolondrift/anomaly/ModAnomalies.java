package com.denfry.eidolondrift.anomaly;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.client.RoomDesaturateAnomaly;
import com.denfry.eidolondrift.anomaly.client.ShadowInCornerAnomaly;
import com.denfry.eidolondrift.anomaly.sound.CaveResonanceAnomaly;
import com.denfry.eidolondrift.anomaly.sound.ChestClickNoPlayerAnomaly;
import com.denfry.eidolondrift.anomaly.sound.DistantVoiceAnomaly;
import com.denfry.eidolondrift.anomaly.sound.FootstepsAboveAnomaly;
import com.denfry.eidolondrift.anomaly.sound.FootstepsOutsideAnomaly;
import com.denfry.eidolondrift.anomaly.sound.HouseCreakAnomaly;
import com.denfry.eidolondrift.anomaly.sound.PhantomFootstepsAnomaly;
import com.denfry.eidolondrift.anomaly.sound.WhisperAnomaly;
import com.denfry.eidolondrift.anomaly.world.BedRefusesSleepAnomaly;
import com.denfry.eidolondrift.anomaly.world.DoorShiftAnomaly;
import com.denfry.eidolondrift.anomaly.world.FurnaceLitAnomaly;
import com.denfry.eidolondrift.anomaly.world.SignTextAnomaly;
import com.denfry.eidolondrift.anomaly.world.TorchReorderAnomaly;
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

        // Layer 6 — Home Horror, Stages 2–4 (M3).
        AnomalyRegistry.register(new ChestClickNoPlayerAnomaly());   // #2  sound
        AnomalyRegistry.register(new FootstepsOutsideAnomaly());     // "Heard Outside" sound
        AnomalyRegistry.register(new FootstepsAboveAnomaly());       // #10 sound
        AnomalyRegistry.register(new BedRefusesSleepAnomaly());      // #3  functional
        AnomalyRegistry.register(new ShadowInCornerAnomaly());       // #11 client visual
        AnomalyRegistry.register(new RoomDesaturateAnomaly());       // #21 client visual
        AnomalyRegistry.register(new DoorShiftAnomaly());            // #1/#14 gated mutation
        AnomalyRegistry.register(new FurnaceLitAnomaly());           // #7  gated mutation
        AnomalyRegistry.register(new SignTextAnomaly());             // #6/#18 gated mutation
        AnomalyRegistry.register(new TorchReorderAnomaly());         // #5/#15 gated mutation

        EidolonDrift.LOGGER.info("Registered {} anomalies.", AnomalyRegistry.size());
    }
}
