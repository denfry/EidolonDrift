package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound-event registry (GDD §12). All events use the {@code eidolon_drift} namespace and
 * each carries a subtitle distinct from any real sound. Mono + positional attenuation;
 * the Director never repeats the same event within 4 in-game hours per player.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, EidolonDrift.MODID);

    /** A single footstep, placed behind the player. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP_BEHIND = register("step_behind");
    /** Close, wordless whispering. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = register("whisper");
    /** A faint voice far away — almost a real villager/player, but not. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_VOICE = register("distant_voice");
    /** The house settling: a slow structural creak. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUSE_CREAK = register("house_creak");
    /** A low cave drone that should not be there. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CAVE_RESONANCE = register("cave_resonance");
    /** A faint directional tone from where the Observer is about to be (IMPLIED phase, GDD §8). */
    public static final DeferredHolder<SoundEvent, SoundEvent> OBSERVER_TONE = register("observer_tone");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(EidolonDrift.RL(name)));
    }
}
