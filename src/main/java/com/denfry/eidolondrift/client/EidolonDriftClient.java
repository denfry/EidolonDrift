package com.denfry.eidolondrift.client;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.client.render.ObserverRenderer;
import com.denfry.eidolondrift.registry.ModEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Physical-client entry point (loaded only on {@link Dist#CLIENT}). For M2 it wires the Observer's
 * renderer; the HUD/render-deception layer (M4) attaches its handlers here too.
 */
@EventBusSubscriber(modid = EidolonDrift.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EidolonDriftClient {

    private EidolonDriftClient() {}

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.OBSERVER.get(), ObserverRenderer::new);
    }
}
