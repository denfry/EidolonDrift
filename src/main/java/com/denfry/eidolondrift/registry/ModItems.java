package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry. Empty for M0 — the 10 counterplay items land in Layer 9 (M6).
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EidolonDrift.MODID);

    private ModItems() {}
}
