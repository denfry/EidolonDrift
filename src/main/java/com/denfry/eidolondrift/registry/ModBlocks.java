package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry. Empty for M0 — the 9 blocks land in Layer 9 (M6).
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EidolonDrift.MODID);

    private ModBlocks() {}
}
