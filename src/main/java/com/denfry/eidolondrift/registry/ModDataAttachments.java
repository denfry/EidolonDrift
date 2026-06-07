package com.denfry.eidolondrift.registry;

import com.denfry.eidolondrift.EidolonDrift;
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

    private ModDataAttachments() {}
}
