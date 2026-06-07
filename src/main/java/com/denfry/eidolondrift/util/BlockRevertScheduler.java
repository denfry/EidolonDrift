package com.denfry.eidolondrift.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.denfry.eidolondrift.EidolonDrift;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Restores a block (and, optionally, its block-entity data) to a captured state after a
 * delay. This is what makes the config-gated home mutations <b>revertible</b> — a door that
 * drifts open swings back, a relit furnace goes cold again, a re-lettered sign reverts — so
 * the house only ever <i>seems</i> to change (invariant §1) and never destroys property
 * (invariant §8). Used solely behind {@code world_mutation_allowed}.
 */
public final class BlockRevertScheduler {

    private record Task(ResourceKey<Level> dimension, BlockPos pos, BlockState original,
                        @Nullable CompoundTag blockEntityData, long fireTick) {}

    private static final List<Task> QUEUE = new ArrayList<>();

    private BlockRevertScheduler() {}

    /** Restore {@code pos} to {@code original} (and {@code beData} if non-null) after {@code delayTicks}. */
    public static void schedule(ServerLevel level, BlockPos pos, BlockState original,
                                @Nullable CompoundTag beData, int delayTicks) {
        long fire = level.getGameTime() + Math.max(1, delayTicks);
        QUEUE.add(new Task(level.dimension(), pos.immutable(), original, beData, fire));
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<Task> it = QUEUE.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            ServerLevel level = server.getLevel(t.dimension());
            if (level == null) { it.remove(); continue; }
            if (level.getGameTime() < t.fireTick()) continue;
            it.remove();
            if (!level.isLoaded(t.pos())) continue; // chunk gone — leave it; nothing to revert visibly
            restore(level, t);
        }
    }

    private static void restore(ServerLevel level, Task t) {
        try {
            level.setBlock(t.pos(), t.original(), Block.UPDATE_ALL);
            if (t.blockEntityData() != null) {
                BlockEntity be = level.getBlockEntity(t.pos());
                if (be != null) {
                    be.loadWithComponents(t.blockEntityData(), level.registryAccess());
                    be.setChanged();
                    level.sendBlockUpdated(t.pos(), t.original(), t.original(), Block.UPDATE_ALL);
                }
            }
        } catch (Exception e) {
            EidolonDrift.LOGGER.error("Failed to revert block at {}", t.pos(), e);
        }
    }
}
