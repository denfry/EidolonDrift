package com.denfry.eidolondrift.network;

import com.denfry.eidolondrift.EidolonDrift;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network entry point. All gameplay packets are S2C and sent <b>per-player unicast</b>
 * (invariant §2): one player's illusions must never reach another client.
 *
 * <p>Skeleton for M0 — payload types register here from Layer 7 (client deception) on.
 */
public final class EidolonNetworking {

    /** Bump when wire formats change incompatibly. */
    private static final String PROTOCOL_VERSION = "1";

    private EidolonNetworking() {}

    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // S2C client-deception channel (M3 home visuals; reused by M4). The handler runs
        // only on the physical client — the lambda body classloads the client handler
        // lazily, so a dedicated server never touches client-only classes.
        registrar.playToClient(TriggerClientAnomalyPayload.TYPE,
                TriggerClientAnomalyPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.denfry.eidolondrift.client.ClientAnomalyHandler.trigger(payload)));
        registrar.playToClient(CancelClientAnomalyPayload.TYPE,
                CancelClientAnomalyPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.denfry.eidolondrift.client.ClientAnomalyHandler.cancel(payload)));

        EidolonDrift.LOGGER.debug("Eidolon Drift network channel '{}' ready ({} S2C payloads).",
                PROTOCOL_VERSION, 2);
    }

    /**
     * Unicast a payload to exactly one player. The ONLY sanctioned way to deliver an
     * anomaly/MindState/echo packet — never broadcast (invariant §2).
     */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
