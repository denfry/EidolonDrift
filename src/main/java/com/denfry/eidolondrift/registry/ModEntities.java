package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity registry. Empty for M0 — Observer (L5/M2) and Player Echo (L8/M5) land later.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EidolonDrift.MODID);

    private ModEntities() {}
}
