package com.denfry.eidolondrift.anomaly.client;

import com.denfry.eidolondrift.EidolonDrift;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared ids for the client-deception channel (GDD §10). Both the server-side anomaly that
 * sends the trigger and the client handler that renders it reference these, so the wire
 * {@code type} stays in lock-step. Each value equals the corresponding anomaly's {@code id()}.
 */
public final class ClientAnomalyTypes {

    /** A still, dark shape in a home corner that thins out as you approach (catalogue #11). */
    public static final ResourceLocation SHADOW_IN_CORNER = EidolonDrift.RL("shadow_in_corner");
    /** The room briefly drains of colour (catalogue #21, feasible client overlay). */
    public static final ResourceLocation ROOM_DESATURATE = EidolonDrift.RL("room_desaturate");

    private ClientAnomalyTypes() {}
}
