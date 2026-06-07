package com.denfry.eidolondrift.director;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.Anomaly;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.util.WeightedRandom;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The "horror filmmaker" (GDD §5): a server-side conductor that decides <i>when</i>,
 * <i>what</i>, and (via the scheduler) <i>at what moment</i> anomalies fire. It enforces
 * scarcity — session budget, minimum gap, cooldowns, repetition guard — and the protected
 * windows (post-death, combat, first days, creative/spectator). Ticks every
 * {@code director_tick_interval} ticks per player, never per-frame.
 *
 * <p>Holds no hard reference to any concrete anomaly; it only reads {@link AnomalyRegistry}.
 */
public final class AnomalyDirector {

    public static final int SESSION_BUDGET = 3;       // max events per session (GDD §5)
    public static final int MIN_GAP_TICKS = 6000;     // 5 minutes between events
    public static final int FIRST_DAYS_PROTECTED = 3; // GDD §21
    public static final int POST_DEATH_TICKS = 2400;  // 2 minutes (GDD §21)
    public static final int COMBAT_TICKS = 200;       // 10 seconds (GDD §21)

    private static final AnomalyMemory MEMORY = new AnomalyMemory();
    private static final AnomalyCooldownManager COOLDOWNS = new AnomalyCooldownManager();
    private static final AnomalyScheduler SCHEDULER = new AnomalyScheduler();
    private static final Map<UUID, DirectorState> STATES = new HashMap<>();

    /** Injectable for deterministic tests (PLAN §7). */
    private static RandomSource rng = RandomSource.create();

    private AnomalyDirector() {}

    private static final class DirectorState {
        int tickAccumulator = 0;
        long lastCombatTick = Long.MIN_VALUE;
        long lastDeathTick = Long.MIN_VALUE;
    }

    private static DirectorState state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new DirectorState());
    }

    public static AnomalyMemory memory() {
        return MEMORY;
    }

    public static int pendingScheduled() {
        return SCHEDULER.pending();
    }

    public static void setRngForTesting(RandomSource source) {
        rng = source;
    }

    // ── events ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        // Always drain the scheduler so already-decided anomalies fire on time.
        SCHEDULER.tick(event.getServer(), AnomalyDirector::fire);

        if (!ModConfig.ENABLED.get()) return;
        double freq = ModConfig.ANOMALY_FREQUENCY_MULTIPLIER.get();
        if (freq <= 0.0) return;

        int interval = ModConfig.DIRECTOR_TICK_INTERVAL.get();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            DirectorState st = state(player);
            if (++st.tickAccumulator < interval) continue;
            st.tickAccumulator = 0;
            evaluate(player, st, freq);
        }
    }

    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MEMORY.resetSession(player);
            state(player).tickAccumulator = 0;
        }
    }

    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(final LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            state(player).lastCombatTick = player.level().getGameTime();
        }
    }

    @SubscribeEvent
    public static void onDeath(final LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            state(player).lastDeathTick = player.level().getGameTime();
        }
    }

    // ── core selection (GDD §5) ─────────────────────────────────────────────────

    private static void evaluate(ServerPlayer player, DirectorState st, double freq) {
        AnomalyContext ctx = AnomalyContext.build(player, MEMORY);
        if (!shouldConsider(player, st, ctx, freq)) return;

        int wave = EscalationLadder.currentWave(ctx);
        int budget = EscalationLadder.pressureBudgetForWave(wave, ctx.mindState());

        List<Anomaly> candidates = candidates(ctx, budget, player);
        if (candidates.isEmpty()) return;

        Anomaly chosen = WeightedRandom.pick(candidates, a -> a.baseWeight(ctx), rng);
        if (chosen == null) return;

        int delay = chooseDelay();
        SCHEDULER.schedule(player, chosen, delay);
        MEMORY.record(player, chosen);        // count at decision time (budget honesty)
        COOLDOWNS.apply(player, chosen);
        EidolonDrift.LOGGER.debug("Director scheduled '{}' for {} in {}t (wave {}, budget {})",
                chosen.id(), player.getGameProfile().getName(), delay, wave, budget);
    }

    /** Candidate filter — also usable by tests with a hand-built context. */
    public static List<Anomaly> candidates(AnomalyContext ctx, int budget, ServerPlayer player) {
        List<Anomaly> out = new ArrayList<>();
        for (Anomaly a : AnomalyRegistry.all()) {
            if (a.pressureCost() > budget) continue;
            if (!a.canFire(ctx)) continue;
            if (COOLDOWNS.isOnCooldown(player, a)) continue;
            if (MEMORY.firedRecently(player, a, 5)) continue;
            out.add(a);
        }
        return out;
    }

    private static boolean shouldConsider(ServerPlayer player, DirectorState st,
                                          AnomalyContext ctx, double freq) {
        if (player.isCreative() || player.isSpectator()) return false;
        if (!dimensionAllowed(ctx.dimension())) return false;

        Level level = player.level();
        long now = level.getGameTime();

        // Protected windows (GDD §21).
        if (level.getDayTime() / 24000L < FIRST_DAYS_PROTECTED) return false;
        if (now - st.lastDeathTick < POST_DEATH_TICKS) return false;
        if (now - st.lastCombatTick < COMBAT_TICKS) return false;

        // Scarcity.
        if (MEMORY.sessionCount(player) >= SESSION_BUDGET) return false;
        long minGap = (long) (MIN_GAP_TICKS / Math.max(0.05, freq));
        if (MEMORY.timeSinceLast(player) < minGap) return false;

        // Very low distortion at Stage 0 → mostly silence (deniability, GDD §2).
        if (ctx.mindState().distortion < 5f && ctx.progressionStage() == 0) {
            return rng.nextFloat() < 0.05f * (float) freq;
        }
        return true;
    }

    private static int chooseDelay() {
        // 1–6 s later, so the event lands a beat after the evaluation, not on the dot.
        return 20 + rng.nextInt(100);
    }

    private static void fire(ServerPlayer player, Anomaly anomaly) {
        try {
            anomaly.execute(AnomalyContext.build(player, MEMORY));
        } catch (Exception e) {
            EidolonDrift.LOGGER.error("Anomaly '{}' threw during execute", anomaly.id(), e);
        }
    }

    // ── admin / testing entry point ─────────────────────────────────────────────

    /** Force an anomaly to fire now, bypassing all gating (used by {@code /eidolon anomaly}). */
    public static boolean forceFire(ServerPlayer player, ResourceLocation id) {
        Anomaly anomaly = AnomalyRegistry.byId(id).orElse(null);
        if (anomaly == null) return false;
        MEMORY.record(player, anomaly);
        COOLDOWNS.apply(player, anomaly);
        fire(player, anomaly);
        return true;
    }

    private static boolean dimensionAllowed(ResourceKey<Level> dim) {
        String id = dim.location().toString();
        List<? extends String> blacklist = ModConfig.DIMENSION_BLACKLIST.get();
        if (blacklist.contains(id)) return false;
        List<? extends String> whitelist = ModConfig.DIMENSION_WHITELIST.get();
        return whitelist.isEmpty() || whitelist.contains(id);
    }
}
