package com.denfry.eidolondrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.denfry.eidolondrift.util.WeightedRandom;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** Director selection must be reproducible under a seeded RNG (PLAN §7). */
class WeightedRandomTest {

    @Test
    void sameSeedSameSequence() {
        List<String> items = List.of("a", "b", "c");
        RandomSource r1 = RandomSource.create(1234L);
        RandomSource r2 = RandomSource.create(1234L);
        for (int i = 0; i < 100; i++) {
            assertEquals(WeightedRandom.pick(items, s -> 1.0, r1),
                    WeightedRandom.pick(items, s -> 1.0, r2),
                    "identical seeds must yield identical picks");
        }
    }

    @Test
    void heavyWeightDominates() {
        List<String> items = List.of("rare", "common");
        RandomSource rng = RandomSource.create(42L);
        int common = 0;
        for (int i = 0; i < 10_000; i++) {
            if ("common".equals(WeightedRandom.pick(items, s -> s.equals("common") ? 9.0 : 1.0, rng))) {
                common++;
            }
        }
        // Expected ~90%; allow a generous band.
        assertTrue(common > 8500 && common < 9500, "common picked " + common + "/10000");
    }

    @Test
    void allZeroWeightsYieldNull() {
        assertNull(WeightedRandom.pick(List.of("a", "b"), s -> 0.0, RandomSource.create(1L)));
        assertNull(WeightedRandom.pick(List.<String>of(), s -> 1.0, RandomSource.create(1L)));
    }
}
