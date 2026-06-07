package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound-event registry. Empty for M0 — sound anomalies land in Layer 4 (M1).
 * All events use the {@code eidolon_drift} namespace and require distinct subtitles.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, EidolonDrift.MODID);

    private ModSounds() {}
}
