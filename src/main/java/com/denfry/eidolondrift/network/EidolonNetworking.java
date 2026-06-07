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
        // No payloads yet. Future (S2C only):
        //   registrar.playToClient(TriggerClientAnomalyPayload.TYPE,
        //                          TriggerClientAnomalyPayload.STREAM_CODEC,
        //                          ClientAnomalyHandler::handle);
        EidolonDrift.LOGGER.debug("Eidolon Drift network channel '{}' ready (no payloads yet).",
                PROTOCOL_VERSION);
    }

    /**
     * Unicast a payload to exactly one player. The ONLY sanctioned way to deliver an
     * anomaly/MindState/echo packet — never broadcast (invariant §2).
     */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
