package com.denfry.eidolondrift.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.denfry.eidolondrift.EidolonDrift;
import com.denfry.eidolondrift.anomaly.client.ClientAnomalyTypes;
import com.denfry.eidolondrift.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Client-only store + driver for active {@link ClientAnomalyInstance}s (GDD §10). Ages them
 * every client tick, drops them on expiry or when the player walks > {@link #CANCEL_DISTANCE}
 * blocks from the illusion's origin (PLAN §6), enforces the per-player cap, and paints the
 * feasible M3 visuals: a shadow (dark particles) and a brief room desaturation overlay.
 *
 * <p>All flash/strobe-free: visuals ease in and out (see {@link ClientAnomalyInstance#intensity()}).
 * The desaturation overlay honours {@code disable_screen_distortion} (invariant §6).
 */
@EventBusSubscriber(modid = EidolonDrift.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientAnomalyManager {

    /** Cancel an illusion once the player is this far from its origin (PLAN §6). */
    public static final double CANCEL_DISTANCE = 30.0;

    private static final List<ClientAnomalyInstance> ACTIVE = new ArrayList<>();

    private ClientAnomalyManager() {}

    // ── lifecycle (called from the network handler, client thread) ───────────────

    public static void add(ClientAnomalyInstance instance) {
        int cap = Math.max(0, ModConfig.MAX_CLIENT_ANOMALIES_PER_PLAYER.get());
        if (cap == 0) return;
        while (ACTIVE.size() >= cap) ACTIVE.remove(0); // drop the oldest to make room
        ACTIVE.add(instance);
    }

    public static void clear(ResourceLocation type) {
        ACTIVE.removeIf(i -> i.type.equals(type));
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    // ── tick: age out, distance-cancel, emit shadow particles ────────────────────

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<ClientAnomalyInstance> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ClientAnomalyInstance inst = it.next();
            inst.tick();
            if (inst.expired() || player.blockPosition().distSqr(inst.origin) > CANCEL_DISTANCE * CANCEL_DISTANCE) {
                it.remove();
                continue;
            }
            if (inst.type.equals(ClientAnomalyTypes.SHADOW_IN_CORNER)) {
                emitShadow(level, inst);
            }
        }
    }

    private static void emitShadow(ClientLevel level, ClientAnomalyInstance inst) {
        // A slow, dark wisp clinging to the corner — denser mid-life, sparse at the edges.
        if ((inst.age() & 1) != 0) return; // every other tick
        float intensity = inst.intensity();
        if (intensity < 0.05f) return;
        BlockPos o = inst.origin;
        var rng = level.random;
        int puffs = 1 + (int) (intensity * 2);
        for (int i = 0; i < puffs; i++) {
            double x = o.getX() + 0.5 + (rng.nextDouble() - 0.5) * 0.6;
            double y = o.getY() + 0.2 + rng.nextDouble() * 1.4;
            double z = o.getZ() + 0.5 + (rng.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.005, 0.0);
        }
    }

    // ── render: room desaturation overlay ────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderGui(final RenderGuiEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        if (ModConfig.DISABLE_SCREEN_DISTORTION.get()) return; // accessibility (invariant §6)

        float strongest = 0f;
        for (ClientAnomalyInstance inst : ACTIVE) {
            if (inst.type.equals(ClientAnomalyTypes.ROOM_DESATURATE)) {
                strongest = Math.max(strongest, inst.intensity());
            }
        }
        if (strongest < 0.02f) return;

        int alpha = (int) (strongest * 0x60); // peaks at ~38% — a wash, never a flash
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0x9A9A9A; // neutral grey wash → reads as colour-drain
        GuiGraphics g = event.getGuiGraphics();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), color);
    }
}
