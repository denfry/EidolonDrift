package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.observer.ObserverEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity registry. The Observer (L5/M2) registers here; Player Echo (L8/M5) lands later.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EidolonDrift.MODID);

    /**
     * The Observer (GDD §8). A {@code noSave}, no-summon presence: transient, respawned from
     * {@link com.denfry.eidolondrift.mind.MindState} rather than persisted. Tracking range is wide
     * (10 chunks) so it can be glimpsed at 60–90 blocks; visibility is still per-player unicast
     * via {@link ObserverEntity#broadcastToPlayer}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ObserverEntity>> OBSERVER =
            ENTITIES.register("observer", () -> EntityType.Builder
                    .of(ObserverEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("observer"));

    private ModEntities() {}

    /** Default attributes for the Observer (required for every {@code Mob}). Mod event bus. */
    public static void onCreateAttributes(final EntityAttributeCreationEvent event) {
        event.put(OBSERVER.get(), ObserverEntity.createAttributes().build());
    }
}
