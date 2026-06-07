package com.denfry.eidolondrift.util;

import java.util.List;
import java.util.function.ToDoubleFunction;

import net.minecraft.util.RandomSource;

/**
 * Deterministic weighted selection. The {@link RandomSource} is injected so Director
 * choices are reproducible under a seeded RNG in tests (PLAN §7).
 */
public final class WeightedRandom {

    private WeightedRandom() {}

    /**
     * Pick one item with probability proportional to {@code weigher}. Non-positive
     * weights are skipped. Returns {@code null} only for an empty list or all-zero weights.
     */
    public static <T> T pick(List<T> items, ToDoubleFunction<T> weigher, RandomSource rng) {
        if (items.isEmpty()) return null;

        double total = 0.0;
        for (T item : items) {
            double w = weigher.applyAsDouble(item);
            if (w > 0.0) total += w;
        }
        if (total <= 0.0) return null;

        double roll = rng.nextDouble() * total;
        for (T item : items) {
            double w = weigher.applyAsDouble(item);
            if (w <= 0.0) continue;
            roll -= w;
            if (roll < 0.0) return item;
        }
        // Floating-point fall-through: return the last positive-weight item.
        for (int i = items.size() - 1; i >= 0; i--) {
            if (weigher.applyAsDouble(items.get(i)) > 0.0) return items.get(i);
        }
        return null;
    }
}
