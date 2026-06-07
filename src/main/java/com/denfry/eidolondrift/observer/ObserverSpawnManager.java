package com.denfry.eidolondrift.observer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.mind.MindState;
import com.denfry.eidolondrift.mind.MindStateManager;
import com.denfry.eidolondrift.registry.ModEntities;
import com.denfry.eidolondrift.registry.ModSounds;
import com.denfry.eidolondrift.util.SoundScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side conductor for the Observer (GDD §8). Every {@link #CHECK_INTERVAL} ticks it reads
 * each player's {@link MindState}, decides the {@link ObserverPhase} they are "owed", and spawns,
 * re-phases, or clears their single Observer accordingly. It owns the entity's whole life-cycle;
 * the entity itself only reacts.
 *
 * <p>One Observer targets one player and is invisible to everyone else
 * ({@link ObserverEntity#broadcastToPlayer}). Honours the {@code observer} config block:
 * enable flag, {@code min_stage_to_spawn}, {@code max_concurrent_observers}, {@code aggression}.
 */
public final class ObserverSpawnManager {

    /** Re-evaluate phases every 2 s — cheap, never per-frame (invariant §5). */
    public static final int CHECK_INTERVAL = 40;
    /** Minimum gap between IMPLIED tones for one player (~30 s). */
    private static final int IMPLIED_TONE_GAP = 600;

    private static int tickAccumulator = 0;
    /** player UUID → live Observer entity id. */
    private static final Map<UUID, Integer> OBSERVERS = new HashMap<>();
    /** player UUID → game time of last IMPLIED tone. */
    private static final Map<UUID, Long> LAST_TONE = new HashMap<>();

    private ObserverSpawnManager() {}

    // ── tick ─────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (++tickAccumulator < CHECK_INTERVAL) return;
        tickAccumulator = 0;

        boolean enabled = ModConfig.ENABLED.get() && ModConfig.OBSERVER_ENABLED.get();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!enabled) {
                clear(player.serverLevel(), player.getUUID());
                continue;
            }
            evaluate(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player.serverLevel(), player.getUUID());
            LAST_TONE.remove(player.getUUID());
        }
    }

    // ── decision ─────────────────────────────────────────────────────────────────

    private static void evaluate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();

        if (player.isCreative() || player.isSpectator() || !dimensionAllowed(level)) {
            clear(level, id);
            return;
        }

        MindState ms = MindStateManager.get(player);
        ObserverPhase desired = ObserverPhase
                .autoFor(ms, ModConfig.OBSERVER_AGGRESSION.get())
                .cappedToImplemented();

        // Entity phases require the configured minimum progression stage; below it, at most a tone.
        if (desired.hasEntity() && ms.progressionStage < ModConfig.OBSERVER_MIN_STAGE.get()) {
            desired = ObserverPhase.IMPLIED;
        }

        if (desired == ObserverPhase.ABSENT) {
            clear(level, id);
            return;
        }
        if (desired == ObserverPhase.IMPLIED) {
            clear(level, id);
            maybePlayTone(player);
            return;
        }

        // PERIPHERAL / SEEN — keep exactly one Observer in the requested phase.
        ObserverEntity existing = current(level, id);
        if (existing != null) {
            existing.setPhase(desired);
            return;
        }
        if (OBSERVERS.size() >= ModConfig.OBSERVER_MAX_CONCURRENT.get()) return;
        spawn(player, level, desired);
    }

    private static void maybePlayTone(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = LAST_TONE.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now - last < IMPLIED_TONE_GAP) return;
        // Faint tone from a plausible future-spawn bearing (behind the player).
        float angle = player.getYRot() + 180f + (player.getRandom().nextFloat() - 0.5f) * 80f;
        double rad = Math.toRadians(angle);
        Vec3 from = player.position().add(-Math.sin(rad) * 24.0, 1.0, Math.cos(rad) * 24.0);
        SoundScheduler.playOnce(player, ModSounds.OBSERVER_TONE, from, 0.5f, 1.0f);
        LAST_TONE.put(player.getUUID(), now);
    }

    // ── spawning / clearing ──────────────────────────────────────────────────────

    private static void spawn(ServerPlayer player, ServerLevel level, ObserverPhase phase) {
        Optional<Vec3> spot = pickSpawnPos(player, level);
        if (spot.isEmpty()) return;

        ObserverEntity obs = ModEntities.OBSERVER.get().create(level);
        if (obs == null) return;
        obs.setTargetPlayer(player.getUUID());
        obs.setPhase(phase);
        Vec3 pos = spot.get();
        obs.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0f);
        level.addFreshEntity(obs);
        OBSERVERS.put(player.getUUID(), obs.getId());
        EidolonDrift.LOGGER.debug("Observer spawned for {} as {} at {}",
                player.getGameProfile().getName(), phase, pos);
    }

    /** Spawn at the far edge of sight (60–88 blocks), biased behind the player's view. */
    private static Optional<Vec3> pickSpawnPos(ServerPlayer player, ServerLevel level) {
        for (int attempt = 0; attempt < 6; attempt++) {
            float angle = player.getYRot() + 180f + (player.getRandom().nextFloat() - 0.5f) * 140f;
            double dist = 60.0 + player.getRandom().nextDouble() * 28.0;
            double rad = Math.toRadians(angle);
            double x = player.getX() - Math.sin(rad) * dist;
            double z = player.getZ() + Math.cos(rad) * dist;
            int bx = (int) Math.floor(x);
            int bz = (int) Math.floor(z);
            if (!level.isLoaded(new BlockPos(bx, level.getMinBuildHeight() + 1, bz))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            return Optional.of(new Vec3(bx + 0.5, y, bz + 0.5));
        }
        return Optional.empty();
    }

    private static ObserverEntity current(ServerLevel level, UUID playerId) {
        Integer entityId = OBSERVERS.get(playerId);
        if (entityId == null) return null;
        Entity e = level.getEntity(entityId);
        if (e instanceof ObserverEntity obs && obs.isAlive()) return obs;
        OBSERVERS.remove(playerId);                          // stale (unloaded / discarded)
        return null;
    }

    private static void clear(ServerLevel level, UUID playerId) {
        Integer entityId = OBSERVERS.remove(playerId);
        if (entityId == null) return;
        Entity e = level.getEntity(entityId);
        if (e instanceof ObserverEntity obs) obs.discard();
    }

    private static boolean dimensionAllowed(ServerLevel level) {
        String id = level.dimension().location().toString();
        List<? extends String> blacklist = ModConfig.DIMENSION_BLACKLIST.get();
        if (blacklist.contains(id)) return false;
        List<? extends String> whitelist = ModConfig.DIMENSION_WHITELIST.get();
        return whitelist.isEmpty() || whitelist.contains(id);
    }

    // ── admin / debug entry points (/eidolon observer …) ─────────────────────────

    /** Force-spawn (or re-phase) an Observer for the player at {@code phase}; returns the live phase. */
    public static ObserverPhase debugSpawn(ServerPlayer player, ObserverPhase phase) {
        ServerLevel level = player.serverLevel();
        ObserverPhase capped = phase.cappedToImplemented();
        if (!capped.hasEntity()) capped = ObserverPhase.PERIPHERAL;
        ObserverEntity existing = current(level, player.getUUID());
        if (existing != null) {
            existing.setPhase(capped);
        } else {
            spawn(player, level, capped);
        }
        return capped;
    }

    public static boolean debugClear(ServerPlayer player) {
        boolean had = current(player.serverLevel(), player.getUUID()) != null;
        clear(player.serverLevel(), player.getUUID());
        return had;
    }

    /** Human-readable state for {@code /eidolon observer info}. */
    public static String debugInfo(ServerPlayer player) {
        ObserverEntity obs = current(player.serverLevel(), player.getUUID());
        ObserverPhase auto = ObserverPhase.autoFor(
                MindStateManager.get(player), ModConfig.OBSERVER_AGGRESSION.get());
        if (obs == null) {
            return "none (owed: " + auto + ")";
        }
        double dist = Math.sqrt(obs.distanceToSqr(player));
        return String.format("%s @ %.1fm (owed: %s)", obs.getPhase(), dist, auto);
    }
}
