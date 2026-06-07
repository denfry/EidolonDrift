package com.denfry.eidolondrift;

import com.denfry.eidolondrift.command.EidolonCommand;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.network.EidolonNetworking;
import com.denfry.eidolondrift.registry.ModBlocks;
import com.denfry.eidolondrift.registry.ModDataAttachments;
import com.denfry.eidolondrift.registry.ModEntities;
import com.denfry.eidolondrift.registry.ModItems;
import com.denfry.eidolondrift.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Eidolon Drift — psychological-horror NeoForge mod for Minecraft 1.21.1.
 *
 * <p>The world — not a monster — is the antagonist. A server-side {@code MindState},
 * a {@code WorldMemory} of real habits, and an {@code AnomalyDirector} produce rare,
 * personalised, mostly client-side anomalies. See {@code docs/PLAN.md} and the GDD.
 *
 * <p>This is the Layer 0 (Bootstrap) entry point: it wires the deferred registries to
 * the mod event bus and schedules network payload registration. No content yet.
 */
@Mod(EidolonDrift.MODID)
public class EidolonDrift {

    /** Mod id / namespace for every {@link ResourceLocation}, sound, and lang key. */
    public static final String MODID = "eidolon_drift";

    public static final Logger LOGGER = LogUtils.getLogger();

    public EidolonDrift(IEventBus modBus, ModContainer container) {
        // Config must register before content so spec values are available at setup.
        ModConfig.register(container);

        // Deferred registries — empty for M0, populated in later layers.
        ModDataAttachments.ATTACHMENTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        // Network payloads register on the mod bus via RegisterPayloadHandlersEvent.
        modBus.addListener(EidolonNetworking::registerPayloads);
        modBus.addListener(this::commonSetup);

        // Commands register on the game event bus, not the mod bus.
        NeoForge.EVENT_BUS.addListener(EidolonCommand::onRegisterCommands);

        LOGGER.info("Eidolon Drift bootstrapped. The house is listening.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Reserved for cross-registry wiring once systems land (Director, Echo, etc.).
    }

    /**
     * Canonical helper for building {@code eidolon_drift:}-namespaced resource locations.
     * Every {@link ResourceLocation} in the mod MUST go through here (project convention).
     */
    public static ResourceLocation RL(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
