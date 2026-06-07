package com.denfry.eidolondrift.network;

import com.denfry.eidolondrift.EidolonDrift;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: drop active client illusions early (GDD §10). Sent unicast when the server decides
 * an illusion should stop before its natural expiry — e.g. an admin clear. The empty
 * {@link #type} is the sentinel for "clear all". Self-expiry and the >30-block distance
 * cancel are handled client-side; this is the explicit server-initiated path.
 */
public record CancelClientAnomalyPayload(ResourceLocation anomalyType) implements CustomPacketPayload {

    /** Sentinel meaning "cancel every active client anomaly for this player". */
    public static final ResourceLocation ALL = EidolonDrift.RL("all");

    public static final CustomPacketPayload.Type<CancelClientAnomalyPayload> TYPE =
            new CustomPacketPayload.Type<>(EidolonDrift.RL("cancel_client_anomaly"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelClientAnomalyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, CancelClientAnomalyPayload::anomalyType,
                    CancelClientAnomalyPayload::new);

    public static CancelClientAnomalyPayload all() {
        return new CancelClientAnomalyPayload(ALL);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
