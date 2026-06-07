package com.denfry.eidolondrift.anomaly.client;

import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.network.EidolonNetworking;
import com.denfry.eidolondrift.network.TriggerClientAnomalyPayload;

import net.minecraft.server.level.ServerPlayer;

/**
 * The room briefly drains of colour (catalogue #21 — gated a stage early at 4 as a feasible
 * client visual for M3). Pure client illusion: a unicast trigger, a soft grey wash that eases
 * in and out, then it's gone. The client honours {@code disable_screen_distortion}
 * (invariant §6) and the wash never flashes.
 */
public final class RoomDesaturateAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 60; // ~3 s (catalogue #21)

    public RoomDesaturateAnomaly() {
        super("room_desaturate", AnomalyCategory.VISUAL);
    }

    @Override protected int minStage() { return 4; }
    @Override protected float baseHomeWeight(AnomalyContext ctx) { return 1.1f; }
    @Override public int cooldownTicks() { return 12000; } // 10 min
    @Override public int pressureCost() { return 1; }
    @Override public boolean isClientSide() { return true; }

    @Override
    public void execute(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        EidolonNetworking.sendToPlayer(player, new TriggerClientAnomalyPayload(
                ClientAnomalyTypes.ROOM_DESATURATE, player.blockPosition(),
                DURATION_TICKS, player.getRandom().nextInt()));
    }
}
