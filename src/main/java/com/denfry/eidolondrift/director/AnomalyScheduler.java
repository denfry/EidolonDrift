package com.denfry.eidolondrift.director;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.denfry.eidolondrift.anomaly.Anomaly;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delays anomaly execution to a contextually appropriate moment (GDD §3/§5): the
 * Director picks <i>what</i>, the scheduler decides <i>when</i>. Entries fire once,
 * on the server thread, and are dropped if the target player has logged off.
 */
public final class AnomalyScheduler {

    private record Entry(UUID player, Anomaly anomaly, long fireTick) {}

    private final List<Entry> queue = new ArrayList<>();

    public void schedule(ServerPlayer player, Anomaly anomaly, int delayTicks) {
        queue.add(new Entry(player.getUUID(), anomaly, player.level().getGameTime() + delayTicks));
    }

    /** Fire all due entries via {@code fire}, which builds the context and executes. */
    public void tick(MinecraftServer server, BiConsumer<ServerPlayer, Anomaly> fire) {
        if (queue.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<Entry> it = queue.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now < e.fireTick()) continue;
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(e.player());
            if (player != null && player.isAlive()) {
                fire.accept(player, e.anomaly());
            }
        }
    }

    public int pending() {
        return queue.size();
    }
}
