package com.denfry.eidolondrift.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * One live client-side illusion (GDD §10). Purely client state: it ages each client tick
 * and removes itself once {@link #expired()} — nothing is ever persisted or restored on
 * relog (invariant §3). The server has no idea this exists.
 */
public final class ClientAnomalyInstance {

    public final ResourceLocation type;
    public final BlockPos origin;
    public final int durationTicks;
    public final int variant;

    private int age;

    public ClientAnomalyInstance(ResourceLocation type, BlockPos origin, int durationTicks, int variant) {
        this.type = type;
        this.origin = origin;
        this.durationTicks = durationTicks;
        this.variant = variant;
    }

    public void tick() {
        age++;
    }

    public int age() {
        return age;
    }

    public boolean expired() {
        return age >= durationTicks;
    }

    /** Eased 0→1→0 intensity (fade in, hold, fade out) for smooth, non-flashing visuals. */
    public float intensity() {
        if (durationTicks <= 0) return 0f;
        float p = Math.min(1f, (float) age / durationTicks);
        return (float) Math.sin(p * Math.PI);
    }
}
