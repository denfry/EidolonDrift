package com.denfry.eidolondrift.util;

import net.minecraft.server.level.ServerPlayer;

/**
 * Small server-side helpers shared across systems.
 */
public final class ServerHelper {

    private ServerHelper() {}

    /**
     * True if no <i>other</i> player is within {@code radius} blocks in the same level.
     * Used by {@link com.denfry.eidolondrift.mind.MindStateManager} for isolation (GDD §4).
     */
    public static boolean noPlayersNearby(ServerPlayer player, double radius) {
        double r2 = radius * radius;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player) continue;
            if (other.distanceToSqr(player) <= r2) return false;
        }
        return true;
    }
}
