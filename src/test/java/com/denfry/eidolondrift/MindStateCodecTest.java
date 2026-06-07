package com.denfry.eidolondrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.denfry.eidolondrift.mind.MindState;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

/**
 * MindState must survive serialization round-trips (DoD: "survives relog serialization").
 * Uses JsonOps so the test needs no Minecraft bootstrap — the CODEC is pure DFU.
 */
class MindStateCodecTest {

    @Test
    void roundTripPreservesAllFields() {
        MindState in = new MindState(
                11f, 22f, 33f, 44f, 55f, 66f, 77f, 88f, 99f, 12.5f, 3.5f, 7);

        var encoded = MindState.CODEC.encodeStart(JsonOps.INSTANCE, in);
        assertTrue(encoded.result().isPresent(), "encode should succeed");

        MindState out = MindState.CODEC.parse(JsonOps.INSTANCE, encoded.result().get())
                .result().orElseThrow();

        assertEquals(in.dread, out.dread);
        assertEquals(in.suspicion, out.suspicion);
        assertEquals(in.attachment, out.attachment);
        assertEquals(in.distortion, out.distortion);
        assertEquals(in.isolation, out.isolation);
        assertEquals(in.routine, out.routine);
        assertEquals(in.memoryPressure, out.memoryPressure);
        assertEquals(in.homeCorruption, out.homeCorruption);
        assertEquals(in.caveResonance, out.caveResonance);
        assertEquals(in.sleepDebt, out.sleepDebt);
        assertEquals(in.echoDensity, out.echoDensity);
        assertEquals(in.progressionStage, out.progressionStage);
    }

    @Test
    void defaultsRoundTrip() {
        MindState in = new MindState();
        var json = MindState.CODEC.encodeStart(JsonOps.INSTANCE, in).result().orElseThrow();
        MindState out = MindState.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
        assertEquals(0f, out.dread);
        assertEquals(0, out.progressionStage);
    }

    @Test
    void distortionIsDerivedFromAcuteParamsAndStage() {
        MindState ms = new MindState();
        ms.attachment = 50f;   // * 0.004 = 0.2
        ms.dread = 100f;       // * 0.003 = 0.3
        ms.progressionStage = 3; // * 10 = 30
        ms.recomputeDistortion();
        assertEquals(30.5f, ms.distortion, 1e-4);
    }
}
