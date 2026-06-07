package com.denfry.eidolondrift.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The invisible, multi-dimensional fear state that replaces a classic "sanity bar"
 * (GDD §4). The player never sees these numbers — only symptoms. Server-side truth.
 *
 * <p>Field set follows the GDD §19 skeleton. {@link #distortion} is <b>derived</b>
 * (see {@link #recomputeDistortion()}) and must never be set directly by gameplay
 * code — but it is still serialized so state survives relog and is inspectable.
 * Every field is in {@code [0, 100]} except {@link #progressionStage} ({@code [0, 9]}).
 */
public class MindState {

    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 9;

    // Acute / volatile
    public float dread;
    public float suspicion;
    public float attachment;
    public float distortion;     // DERIVED — do not set directly

    // Behavioural
    public float isolation;
    public float routine;
    public float memoryPressure;

    // Place-bound
    public float homeCorruption;
    public float caveResonance;
    public float sleepDebt;
    public float echoDensity;

    public int progressionStage;

    /** Full Codec — serializes <b>all</b> fields (CLAUDE.md invariant), including derived distortion. */
    public static final Codec<MindState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("dread", 0f).forGetter(m -> m.dread),
            Codec.FLOAT.optionalFieldOf("suspicion", 0f).forGetter(m -> m.suspicion),
            Codec.FLOAT.optionalFieldOf("attachment", 0f).forGetter(m -> m.attachment),
            Codec.FLOAT.optionalFieldOf("distortion", 0f).forGetter(m -> m.distortion),
            Codec.FLOAT.optionalFieldOf("isolation", 0f).forGetter(m -> m.isolation),
            Codec.FLOAT.optionalFieldOf("routine", 0f).forGetter(m -> m.routine),
            Codec.FLOAT.optionalFieldOf("memoryPressure", 0f).forGetter(m -> m.memoryPressure),
            Codec.FLOAT.optionalFieldOf("homeCorruption", 0f).forGetter(m -> m.homeCorruption),
            Codec.FLOAT.optionalFieldOf("caveResonance", 0f).forGetter(m -> m.caveResonance),
            Codec.FLOAT.optionalFieldOf("sleepDebt", 0f).forGetter(m -> m.sleepDebt),
            Codec.FLOAT.optionalFieldOf("echoDensity", 0f).forGetter(m -> m.echoDensity),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(m -> m.progressionStage)
    ).apply(inst, MindState::new));

    /** Default attachment value: a calm, Stage-0 mind. */
    public MindState() {}

    public MindState(float dread, float suspicion, float attachment, float distortion,
                     float isolation, float routine, float memoryPressure,
                     float homeCorruption, float caveResonance, float sleepDebt,
                     float echoDensity, int progressionStage) {
        this.dread = dread;
        this.suspicion = suspicion;
        this.attachment = attachment;
        this.distortion = distortion;
        this.isolation = isolation;
        this.routine = routine;
        this.memoryPressure = memoryPressure;
        this.homeCorruption = homeCorruption;
        this.caveResonance = caveResonance;
        this.sleepDebt = sleepDebt;
        this.echoDensity = echoDensity;
        this.progressionStage = progressionStage;
    }

    /** Clamp a parameter to its valid {@code [0, 100]} range. */
    public static float clamp(float v) {
        return v < 0f ? 0f : (v > 100f ? 100f : v);
    }

    /**
     * Recompute the derived {@link #distortion} from the acute params + stage (GDD §4).
     * Called by {@link MindStateManager} every tick; gameplay code must not set distortion.
     */
    public void recomputeDistortion() {
        this.distortion = clamp(attachment * 0.004f + dread * 0.003f + progressionStage * 10f);
    }

    /** Clamp every parameter back into range (call after mutating). */
    public void clampAll() {
        dread = clamp(dread);
        suspicion = clamp(suspicion);
        attachment = clamp(attachment);
        distortion = clamp(distortion);
        isolation = clamp(isolation);
        routine = clamp(routine);
        memoryPressure = clamp(memoryPressure);
        homeCorruption = clamp(homeCorruption);
        caveResonance = clamp(caveResonance);
        sleepDebt = clamp(sleepDebt);
        echoDensity = clamp(echoDensity);
        if (progressionStage < MIN_STAGE) progressionStage = MIN_STAGE;
        if (progressionStage > MAX_STAGE) progressionStage = MAX_STAGE;
    }

    public void addDread(float amount) {
        dread = clamp(dread + amount);
    }

    public void addSuspicion(float amount) {
        suspicion = clamp(suspicion + amount);
    }
}
