package com.denfry.eidolondrift.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.denfry.eidolondrift.config.ModConfig;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Plays horror sounds to a <b>single</b> player (unicast, invariant §2) at a world
 * position, optionally as a timed series (e.g. three footsteps a second apart). Other
 * players never receive these packets, so one player's dread is inaudible to the rest.
 *
 * <p>Server-side: it sends {@link ClientboundSoundPacket}s directly down the player's
 * connection. Respects {@code reduce_audio_horror} (−60% volume).
 */
public final class SoundScheduler {

    private record Step(UUID player, Holder<SoundEvent> sound, double x, double y, double z,
                        float volume, float pitch, long fireTick) {}

    private static final List<Step> QUEUE = new ArrayList<>();

    private SoundScheduler() {}

    /** Play once, immediately, for one player at {@code pos}. */
    public static void playOnce(ServerPlayer player, Holder<SoundEvent> sound, Vec3 pos,
                                float volume, float pitch) {
        send(player, sound, pos.x, pos.y, pos.z, volume, pitch);
    }

    /**
     * Queue {@code count} plays of {@code sound} at {@code pos}, {@code intervalTicks}
     * apart, for one player. The first lands immediately.
     */
    public static void playSeries(ServerPlayer player, Holder<SoundEvent> sound, Vec3 pos,
                                  int count, int intervalTicks, float volume, float pitch) {
        long base = player.level().getGameTime();
        for (int i = 0; i < count; i++) {
            QUEUE.add(new Step(player.getUUID(), sound, pos.x, pos.y, pos.z,
                    volume, pitch, base + (long) i * intervalTicks));
        }
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Step> it = QUEUE.iterator();
        while (it.hasNext()) {
            Step s = it.next();
            if (now < s.fireTick()) continue;
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(s.player());
            if (player != null && player.isAlive()) {
                send(player, s.sound(), s.x(), s.y(), s.z(), s.volume(), s.pitch());
            }
        }
    }

    private static void send(ServerPlayer player, Holder<SoundEvent> sound,
                             double x, double y, double z, float volume, float pitch) {
        if (ModConfig.REDUCE_AUDIO_HORROR.get()) volume *= 0.4f;
        long seed = player.level().getRandom().nextLong();
        player.connection.send(new ClientboundSoundPacket(
                sound, SoundSource.AMBIENT, x, y, z, volume, pitch, seed));
    }
}
