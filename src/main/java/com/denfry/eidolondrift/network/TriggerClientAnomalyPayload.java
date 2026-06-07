package com.denfry.eidolondrift.network;

import com.denfry.eidolondrift.EidolonDrift;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: "render this self-expiring illusion" (GDD §10). The server never tracks what the
 * client draws — it only names a {@link #type}, an {@link #origin} to hang it on, a
 * {@link #durationTicks} after which the client drops it, and a {@link #variant} seed for
 * per-instance variety. Always sent <b>unicast</b> (invariant §2); never broadcast.
 *
 * <p>Introduced for M3's home visuals (shadow, desaturate); M4 reuses the same channel for
 * the wider false-world / HUD anomaly set.
 */
public record TriggerClientAnomalyPayload(ResourceLocation anomalyType, BlockPos origin,
                                          int durationTicks, int variant)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TriggerClientAnomalyPayload> TYPE =
            new CustomPacketPayload.Type<>(EidolonDrift.RL("trigger_client_anomaly"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerClientAnomalyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, TriggerClientAnomalyPayload::anomalyType,
                    BlockPos.STREAM_CODEC, TriggerClientAnomalyPayload::origin,
                    ByteBufCodecs.VAR_INT, TriggerClientAnomalyPayload::durationTicks,
                    ByteBufCodecs.VAR_INT, TriggerClientAnomalyPayload::variant,
                    TriggerClientAnomalyPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
