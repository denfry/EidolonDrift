package com.denfry.eidolondrift.client.render;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.observer.ObserverEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Observer (GDD §8): a still, dark, human-shaped silhouette. Built on the vanilla
 * player model layer so it reads as "almost a person" without bespoke geometry; the unsettling
 * texture is a near-black figure. The phase-driven scale/alpha work (taller NEAR, fading
 * PERIPHERAL) lands with the M4 client layer — for M2 it is a plain, unnervingly motionless body.
 *
 * <p>Client-only. Only the target player ever tracks the entity ({@link ObserverEntity#broadcastToPlayer}),
 * so this renderer never runs on anyone else's client.
 */
public final class ObserverRenderer extends MobRenderer<ObserverEntity, HumanoidModel<ObserverEntity>> {

    private static final ResourceLocation TEXTURE = EidolonDrift.RL("textures/entity/observer.png");

    public ObserverRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ObserverEntity entity) {
        return TEXTURE;
    }
}
