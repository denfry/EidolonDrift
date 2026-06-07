package com.denfry.eidolondrift.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The home zone the house "remembers" (GDD §7, Stage 1 "Remembered"). A lightweight,
 * derived view built from {@link PlayerWorldMemory#primaryBedPos} — the bed is the home
 * anchor — so it needs no separate serialization; corruption itself lives on the
 * {@link com.denfry.eidolondrift.mind.MindState}.
 *
 * <p>The zone is a cuboid {@link AABB} centred on the anchor. It is <b>read-only for
 * destruction</b> (invariant §8): home anomalies may toggle/illusion blocks (revertibly,
 * and only behind {@code world_mutation_allowed}) but must never break player property.
 * Nothing here removes blocks; it only locates candidate targets for anomalies.
 */
public final class HomeMemory {

    /** Half-extent of the home cuboid on X/Z around the anchor. */
    public static final int HALF_EXTENT_XZ = 16;
    /** Half-extent on Y (rooms span a couple of floors). */
    public static final int HALF_EXTENT_Y = 8;

    /** The home anchor — the player's primary bed — or empty if they have no home yet. */
    public final Optional<BlockPos> anchor;

    public HomeMemory(Optional<BlockPos> anchor) {
        this.anchor = anchor;
    }

    /** True once the player has a logged home (i.e. has slept somewhere). */
    public boolean hasHome() {
        return anchor.isPresent();
    }

    /** The home cuboid centred on the anchor, or {@code null} if there is no home. */
    public AABB zone() {
        if (anchor.isEmpty()) return null;
        BlockPos a = anchor.get();
        return new AABB(
                a.getX() - HALF_EXTENT_XZ, a.getY() - HALF_EXTENT_Y, a.getZ() - HALF_EXTENT_XZ,
                a.getX() + HALF_EXTENT_XZ + 1, a.getY() + HALF_EXTENT_Y + 1, a.getZ() + HALF_EXTENT_XZ + 1);
    }

    /** Is {@code pos} inside the home zone? Always false when there is no home. */
    public boolean contains(BlockPos pos) {
        if (anchor.isEmpty()) return false;
        BlockPos a = anchor.get();
        return Math.abs(pos.getX() - a.getX()) <= HALF_EXTENT_XZ
                && Math.abs(pos.getY() - a.getY()) <= HALF_EXTENT_Y
                && Math.abs(pos.getZ() - a.getZ()) <= HALF_EXTENT_XZ;
    }

    /**
     * Collect up to {@code limit} block positions inside the home zone (and within
     * {@code scanRadius} of {@code focus}) whose state matches {@code match}. Bounded scan
     * — home anomalies are rare (≥5 min apart) so an occasional cuboid sweep is cheap
     * relative to the performance budget (invariant §5). Returns an empty list with no home.
     */
    public List<BlockPos> findBlocks(BlockGetter level, BlockPos focus, int scanRadius,
                                     Predicate<BlockState> match, int limit) {
        List<BlockPos> out = new ArrayList<>();
        if (anchor.isEmpty()) return out;
        BlockPos a = anchor.get();

        int minX = Math.max(a.getX() - HALF_EXTENT_XZ, focus.getX() - scanRadius);
        int maxX = Math.min(a.getX() + HALF_EXTENT_XZ, focus.getX() + scanRadius);
        int minY = Math.max(a.getY() - HALF_EXTENT_Y, focus.getY() - scanRadius);
        int maxY = Math.min(a.getY() + HALF_EXTENT_Y, focus.getY() + scanRadius);
        int minZ = Math.max(a.getZ() - HALF_EXTENT_XZ, focus.getZ() - scanRadius);
        int maxZ = Math.min(a.getZ() + HALF_EXTENT_XZ, focus.getZ() + scanRadius);

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    m.set(x, y, z);
                    if (match.test(level.getBlockState(m))) {
                        out.add(m.immutable());
                        if (out.size() >= limit) return out;
                    }
                }
            }
        }
        return out;
    }

    /** Pick one matching block at random inside the zone near {@code focus}, if any. */
    public Optional<BlockPos> findRandomBlock(BlockGetter level, BlockPos focus, int scanRadius,
                                              Predicate<BlockState> match, RandomSource rng) {
        List<BlockPos> found = findBlocks(level, focus, scanRadius, match, 64);
        if (found.isEmpty()) return Optional.empty();
        return Optional.of(found.get(rng.nextInt(found.size())));
    }
}
