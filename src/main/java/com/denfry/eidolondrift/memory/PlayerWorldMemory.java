package com.denfry.eidolondrift.memory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Per-player record of real habits (GDD §6) — the spine of all personalisation.
 * Minimal M1 slice: known chunks, primary bed, sleep history, home corruption.
 * Records but <b>never destroys</b> player property (invariant §8).
 *
 * <p>Stored as a Data Attachment on the player (persists across sessions, copies on
 * death). The richer route/mining/death structures land in later layers; the global
 * {@code WorldMemorySavedData} arrives when shared-home memory is needed (M7).
 */
public class PlayerWorldMemory {

    /** Cap on remembered chunks (ring-eviction); see GDD §24 performance budget. */
    public static final int MAX_KNOWN_CHUNKS = 512;

    /** Packed {@link ChunkPos#toLong()} of chunks the player has spent time in. */
    public final Set<Long> knownChunks = new LinkedHashSet<>();

    public Optional<BlockPos> primaryBedPos = Optional.empty();
    public long timesSlept = 0L;
    /** World day number on which the player last slept; {@code -1} = never. */
    public long lastSleptDay = -1L;
    public float homeCorruptionLevel = 0f;

    /** Derived, not serialized — rebuilt on login and periodically. */
    public transient FearProfile fearProfile = new FearProfile();

    public static final Codec<PlayerWorldMemory> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.listOf().optionalFieldOf("knownChunks", List.of()).forGetter(m -> new ArrayList<>(m.knownChunks)),
            BlockPos.CODEC.optionalFieldOf("primaryBedPos").forGetter(m -> m.primaryBedPos),
            Codec.LONG.optionalFieldOf("timesSlept", 0L).forGetter(m -> m.timesSlept),
            Codec.LONG.optionalFieldOf("lastSleptDay", -1L).forGetter(m -> m.lastSleptDay),
            Codec.FLOAT.optionalFieldOf("homeCorruption", 0f).forGetter(m -> m.homeCorruptionLevel)
    ).apply(inst, PlayerWorldMemory::fromCodec));

    public PlayerWorldMemory() {}

    private static PlayerWorldMemory fromCodec(List<Long> chunks, Optional<BlockPos> bed,
                                               long timesSlept, long lastSleptDay, float homeCorruption) {
        PlayerWorldMemory m = new PlayerWorldMemory();
        m.knownChunks.addAll(chunks);
        m.primaryBedPos = bed;
        m.timesSlept = timesSlept;
        m.lastSleptDay = lastSleptDay;
        m.homeCorruptionLevel = homeCorruption;
        return m;
    }

    // ── habits ────────────────────────────────────────────────────────────────

    public boolean isKnownLocation(BlockPos pos) {
        return knownChunks.contains(new ChunkPos(pos).toLong());
    }

    /**
     * Derived view of the home zone (GDD §7). Anchored on {@link #primaryBedPos}, so it is
     * rebuilt on demand and never serialized separately — corruption lives on the MindState.
     */
    public HomeMemory home() {
        return new HomeMemory(primaryBedPos);
    }

    /** Mark the chunk at {@code pos} as familiar, evicting the oldest if over cap. */
    public void markVisited(BlockPos pos) {
        long key = new ChunkPos(pos).toLong();
        if (knownChunks.contains(key)) return;
        if (knownChunks.size() >= MAX_KNOWN_CHUNKS) {
            var it = knownChunks.iterator();
            it.next();
            it.remove();
        }
        knownChunks.add(key);
    }

    public void recordSleep(Level level, BlockPos bedPos) {
        timesSlept++;
        lastSleptDay = level.getDayTime() / 24000L;
        if (primaryBedPos.isEmpty()) primaryBedPos = Optional.of(bedPos.immutable());
    }

    /** Did the player sleep on the current or previous in-game day? */
    public boolean sleptLastNight(Level level) {
        if (lastSleptDay < 0) return false;
        long today = level.getDayTime() / 24000L;
        return today - lastSleptDay <= 1L;
    }
}
