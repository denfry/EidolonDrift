package com.denfry.eidolondrift.registry;

import java.util.function.Supplier;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.mind.MindState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data Attachment registry (per-player / per-chunk / per-BE state).
 *
 * <p>Empty for M0. {@code MIND_STATE} (L1/M1) and {@code WORLD_MEMORY} (L2/M1) attach
 * here, each serialized via its own {@code CODEC} so it survives relog (see GDD §19).
 */
public final class ModDataAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EidolonDrift.MODID);

    /** Per-player invisible fear state (GDD §4). Persists + copies on death. */
    public static final Supplier<AttachmentType<MindState>> MIND_STATE =
            ATTACHMENTS.register("mind_state", () -> AttachmentType.builder(MindState::new)
                    .serialize(MindState.CODEC)
                    .copyOnDeath()
                    .build());

    /** Per-player record of real habits (GDD §6). Persists + copies on death. */
    public static final Supplier<AttachmentType<PlayerWorldMemory>> WORLD_MEMORY =
            ATTACHMENTS.register("world_memory", () -> AttachmentType.builder(PlayerWorldMemory::new)
                    .serialize(PlayerWorldMemory.CODEC)
                    .copyOnDeath()
                    .build());

    private ModDataAttachments() {}
}
