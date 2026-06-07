package com.denfry.eidolondrift.anomaly;

/**
 * Base for Home-Horror anomalies (GDD §7 — "The House Is Learning"). Shares the common gate
 * — the player must be near a <i>logged</i> home and past the anomaly's minimum stage — and a
 * weight that climbs with {@code homeCorruption} and the player's HOME fear bias.
 *
 * <p>Subclasses add their own {@link #extraGate} (e.g. a config flag for physical mutations)
 * and {@link #execute}. None of them ever destroys player property — the home zone is
 * read-only for destruction (invariant §8); physical changes are revertible and gated.
 */
public abstract class AbstractHomeAnomaly extends AbstractAnomaly {

    protected AbstractHomeAnomaly(String path, AnomalyCategory category) {
        super(path, category);
    }

    /** Lowest progression stage at which this home anomaly may appear (GDD §7 ladder). */
    protected abstract int minStage();

    /** Extra per-anomaly gate beyond the shared home checks. Default: always allowed. */
    protected boolean extraGate(AnomalyContext ctx) {
        return true;
    }

    /** Base weight before the corruption/fear scaling in {@link #baseWeight}. */
    protected float baseHomeWeight(AnomalyContext ctx) {
        return 1.5f;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.isNearHome()
                && ctx.worldMemory().home().hasHome()
                && ctx.progressionStage() >= minStage()
                && extraGate(ctx);
    }

    @Override
    public float baseWeight(AnomalyContext ctx) {
        float w = baseHomeWeight(ctx) + ctx.mindState().homeCorruption * 0.02f;
        if (ctx.fearIsHome()) w *= 1.6f;
        return w;
    }
}
