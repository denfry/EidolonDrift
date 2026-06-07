package com.denfry.eidolondrift.client;

import com.denfry.eidolondrift.network.CancelClientAnomalyPayload;
import com.denfry.eidolondrift.network.TriggerClientAnomalyPayload;

/**
 * Client-thread entry point for the S2C deception channel. Referenced only from the network
 * registrar's S2C lambda, so it is classloaded on the physical client alone — a dedicated
 * server never touches it (invariant §3: client owns perception).
 */
public final class ClientAnomalyHandler {

    private ClientAnomalyHandler() {}

    public static void trigger(TriggerClientAnomalyPayload payload) {
        ClientAnomalyManager.add(new ClientAnomalyInstance(
                payload.anomalyType(), payload.origin(), payload.durationTicks(), payload.variant()));
    }

    public static void cancel(CancelClientAnomalyPayload payload) {
        if (payload.anomalyType().equals(CancelClientAnomalyPayload.ALL)) {
            ClientAnomalyManager.clearAll();
        } else {
            ClientAnomalyManager.clear(payload.anomalyType());
        }
    }
}
