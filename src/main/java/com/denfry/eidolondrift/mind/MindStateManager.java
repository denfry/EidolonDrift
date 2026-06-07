package com.denfry.eidolondrift.mind;

import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.registry.ModDataAttachments;
import com.denfry.eidolondrift.util.ServerHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side decay/growth of every player's {@link MindState} (GDD §4). Runs every
 * {@link #TICK_PERIOD} ticks, never per-frame. Mutates only server-owned state;
 * symptoms (sound/visual lies) are produced elsewhere. Registered on the game event bus.
 */
public final class MindStateManager {

    /** GDD §4: tick logic runs every 100 ticks (5 seconds). */
    public static final int TICK_PERIOD = 100;

    private static int counter = 0;

    private MindStateManager() {}

    // ── convenience accessors ───────────────────────────────────────────────────

    public static MindState get(ServerPlayer player) {
        return player.getData(ModDataAttachments.MIND_STATE.get());
    }

    public static void set(ServerPlayer player, MindState state) {
        player.setData(ModDataAttachments.MIND_STATE.get(), state);
    }

    public static PlayerWorldMemory memory(ServerPlayer player) {
        return player.getData(ModDataAttachments.WORLD_MEMORY.get());
    }

    public static void addDread(ServerPlayer player, float amount) {
        MindState ms = get(player);
        ms.addDread(amount);
        set(player, ms);
    }

    public static void addSuspicion(ServerPlayer player, float amount) {
        MindState ms = get(player);
        ms.addSuspicion(amount);
        set(player, ms);
    }

    // ── events ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (!ModConfig.ENABLED.get()) return;
        if (++counter < TICK_PERIOD) return;
        counter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerWorldMemory mem = memory(player);
        mem.fearProfile.recalculate(mem, get(player));
    }

    /** One decay/growth pass for a single player. Mirrors GDD §4 {@code serverTick()}. */
    private static void tickPlayer(ServerPlayer player) {
        MindState ms = get(player);
        PlayerWorldMemory mem = memory(player);
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        // 0. Record that this chunk is now familiar (drives routine + known-location).
        mem.markVisited(pos);

        // 1. Dread slowly fades.
        ms.dread = MindState.clamp(ms.dread - 0.1f);

        // 2. Routine accumulates in familiar places.
        if (mem.isKnownLocation(pos)) {
            ms.routine = MindState.clamp(ms.routine + 0.05f);
        }

        // 3. Isolation rises when truly alone, fades in company.
        boolean alone = ServerHelper.noPlayersNearby(player, 64);
        if (alone) {
            ms.isolation = MindState.clamp(ms.isolation + 0.02f);
        } else {
            ms.isolation = MindState.clamp(ms.isolation - 0.08f);
        }

        // 4. Cave resonance deep underground in open-sky dimensions.
        if (pos.getY() < -16 && !level.dimensionType().hasCeiling()) {
            ms.caveResonance = MindState.clamp(ms.caveResonance + 0.03f);
        } else {
            ms.caveResonance = MindState.clamp(ms.caveResonance - 0.01f);
        }

        // 5. Sleep debt builds across un-slept nights.
        long time = level.getDayTime() % 24000L;
        if (time > 12542 && time < 23460 && !mem.sleptLastNight(level)) {
            ms.sleepDebt = MindState.clamp(ms.sleepDebt + 1.5f);
        }

        // 6. Distortion is DERIVED — recompute, never set directly.
        ms.recomputeDistortion();

        ms.clampAll();
        set(player, ms);
        player.setData(ModDataAttachments.WORLD_MEMORY.get(), mem);
    }
}
