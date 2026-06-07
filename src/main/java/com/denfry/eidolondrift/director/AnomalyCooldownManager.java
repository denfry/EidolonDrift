package com.denfry.eidolondrift.director;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.denfry.eidolondrift.anomaly.Anomaly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks per-player, per-anomaly cooldowns (GDD §5). An anomaly that fired stays on
 * cooldown for its {@link Anomaly#cooldownTicks()} before it can be selected again.
 */
public final class AnomalyCooldownManager {

    /** player -> (anomaly id -> game tick when cooldown ends). */
    private final Map<UUID, Map<ResourceLocation, Long>> until = new HashMap<>();

    public boolean isOnCooldown(ServerPlayer player, Anomaly anomaly) {
        Map<ResourceLocation, Long> m = until.get(player.getUUID());
        if (m == null) return false;
        Long end = m.get(anomaly.id());
        return end != null && player.level().getGameTime() < end;
    }

    public void apply(ServerPlayer player, Anomaly anomaly) {
        until.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(anomaly.id(), player.level().getGameTime() + anomaly.cooldownTicks());
    }
}
