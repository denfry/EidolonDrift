package com.denfry.eidolondrift.director;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.denfry.eidolondrift.anomaly.Anomaly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player log of recently fired anomalies (GDD §5). Backs the Director's
 * {@code firedRecently} repetition guard, the session-budget counter, and the
 * "never repeat the same sound within 4 in-game hours" rule (GDD §12).
 */
public final class AnomalyMemory {

    private record Fire(ResourceLocation id, long tick) {}

    private static final int MAX_LOG = 32;

    private final Map<UUID, Deque<Fire>> log = new HashMap<>();
    private final Map<UUID, Integer> sessionCount = new HashMap<>();

    public void record(ServerPlayer player, Anomaly anomaly) {
        Deque<Fire> d = log.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        d.addFirst(new Fire(anomaly.id(), player.level().getGameTime()));
        while (d.size() > MAX_LOG) d.removeLast();
        sessionCount.merge(player.getUUID(), 1, Integer::sum);
    }

    /** True if {@code anomaly} appears among the player's last {@code lastN} fires. */
    public boolean firedRecently(ServerPlayer player, Anomaly anomaly, int lastN) {
        Deque<Fire> d = log.get(player.getUUID());
        if (d == null) return false;
        int i = 0;
        for (Fire f : d) {
            if (i++ >= lastN) break;
            if (f.id().equals(anomaly.id())) return true;
        }
        return false;
    }

    /** Game-ticks since this player's most recent anomaly, or {@link Long#MAX_VALUE}. */
    public long timeSinceLast(ServerPlayer player) {
        Deque<Fire> d = log.get(player.getUUID());
        if (d == null || d.isEmpty()) return Long.MAX_VALUE;
        return player.level().getGameTime() - d.peekFirst().tick();
    }

    /** Ticks since this exact anomaly last fired for the player, or {@link Long#MAX_VALUE}. */
    public long timeSince(ServerPlayer player, Anomaly anomaly) {
        Deque<Fire> d = log.get(player.getUUID());
        if (d == null) return Long.MAX_VALUE;
        for (Fire f : d) {
            if (f.id().equals(anomaly.id())) {
                return player.level().getGameTime() - f.tick();
            }
        }
        return Long.MAX_VALUE;
    }

    public int sessionCount(ServerPlayer player) {
        return sessionCount.getOrDefault(player.getUUID(), 0);
    }

    public void resetSession(ServerPlayer player) {
        sessionCount.put(player.getUUID(), 0);
    }
}
