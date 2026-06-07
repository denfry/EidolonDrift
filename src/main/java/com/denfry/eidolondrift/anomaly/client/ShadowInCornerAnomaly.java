package com.denfry.eidolondrift.anomaly.client;

import com.denfry.eidolondrift.anomaly.AbstractHomeAnomaly;
import com.denfry.eidolondrift.anomaly.AnomalyCategory;
import com.denfry.eidolondrift.anomaly.AnomalyContext;
import com.denfry.eidolondrift.network.EidolonNetworking;
import com.denfry.eidolondrift.network.TriggerClientAnomalyPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * A dark shape gathers in a corner of the room and thins away as you turn toward it
 * (catalogue #11, Stage 4). Pure client illusion (invariant §1): the server only sends a
 * unicast trigger; the client renders a brief shadow that self-expires and cancels if the
 * player walks off. Nothing in the world actually changes.
 */
public final class ShadowInCornerAnomaly extends AbstractHomeAnomaly {

    private static final int DURATION_TICKS = 100; // ~5 s (catalogue #11)

    public ShadowInCornerAnomaly() {
        super("shadow_in_corner", AnomalyCategory.VISUAL);
    }

    @Override protected int minStage() { return 4; }
    @Override public int cooldownTicks() { return 9600; } // 8 min
    @Override public int pressureCost() { return 1; }
    @Override public boolean isClientSide() { return true; }

    @Override
    public void execute(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        // A spot a few blocks off, at floor height — read as the corner of the room.
        int dx = 3 + player.getRandom().nextInt(3);
        int dz = 3 + player.getRandom().nextInt(3);
        if (player.getRandom().nextBoolean()) dx = -dx;
        if (player.getRandom().nextBoolean()) dz = -dz;
        BlockPos origin = player.blockPosition().offset(dx, 0, dz);

        EidolonNetworking.sendToPlayer(player, new TriggerClientAnomalyPayload(
                ClientAnomalyTypes.SHADOW_IN_CORNER, origin, DURATION_TICKS, player.getRandom().nextInt()));
    }
}
