package com.denfry.eidolondrift.anomaly.world;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.mind.MindStateManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The bed refuses you (catalogue #3, Stage 3). When armed, the player's <i>next</i> sleep
 * attempt is rejected with the message "Something is watching." — then it disarms, so it
 * costs them exactly one night, never a permanent lockout (invariant §8, no grief).
 *
 * <p>Functional, server-side, and fully personal: the refusal only ever hits the armed
 * player. The static {@link CanPlayerSleepEvent} listener is registered on the game bus.
 */
public final class BedRefusesSleepAnomaly extends AbstractHomeAnomaly {

    /** Players whose next sleep attempt will be refused. */
    private static final Set<UUID> ARMED = new HashSet<>();

    public BedRefusesSleepAnomaly() {
        super("bed_refuses_sleep", AnomalyCategory.WORLD);
    }

    @Override protected int minStage() { return 3; }
    @Override protected float baseHomeWeight(AnomalyContext ctx) { return 1.4f; }
    @Override public int cooldownTicks() { return 12000; } // 10 min
    @Override public int pressureCost() { return 1; }

    @Override
    public void execute(AnomalyContext ctx) {
        ARMED.add(ctx.player().getUUID());
    }

    // ── game-bus listeners (registered via EidolonDrift) ─────────────────────────

    @SubscribeEvent
    public static void onCanSleep(final CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (!ARMED.remove(player.getUUID())) return;   // not armed → vanilla behaviour
        if (event.getProblem() != null) return;        // vanilla already refused; let it stand

        event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        player.displayClientMessage(
                Component.translatable("message.eidolon_drift.bed_refuses").withStyle(s -> s.withItalic(true)),
                true);
        MindStateManager.addDread(player, 6f);
        EidolonDrift.LOGGER.debug("Bed refused sleep for {}", player.getGameProfile().getName());
    }

    /** Disarm on logout so the refusal never carries silently into a later session. */
    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        ARMED.remove(event.getEntity().getUUID());
    }
}
