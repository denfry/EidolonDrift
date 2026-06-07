package com.denfry.eidolondrift.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Line-of-sight maths for the Observer (GDD §8): is the player looking <i>at</i> it? Mirrors
 * vanilla {@code Mob#isLookingAtMe} — the angular tolerance is divided by distance, so a faraway
 * Observer only "catches" a near-direct stare while a close one reacts to a glance.
 *
 * <p>The pure {@link #isLookingToward} core takes plain vectors so it is unit-testable without a
 * live entity; the {@link #isLookingAt} overload adds the engine's occlusion check.
 */
public final class PlayerLookUtil {

    private PlayerLookUtil() {}

    /**
     * True if {@code viewVector} (normalised look direction from {@code viewerEye}) points within
     * {@code tolerance}/distance radians of {@code targetCenter}. Distance-scaled like vanilla.
     */
    public static boolean isLookingToward(Vec3 viewerEye, Vec3 viewVector,
                                          Vec3 targetCenter, double tolerance) {
        Vec3 diff = targetCenter.subtract(viewerEye);
        double len = diff.length();
        if (len < 1.0e-4) return true;                 // standing inside it: trivially "seen"
        double dot = viewVector.dot(diff.scale(1.0 / len));
        return dot > 1.0 - tolerance / len;
    }

    /**
     * Server-side "is the player staring at this Observer" check, including occlusion
     * ({@link ServerPlayer#hasLineOfSight}). Aim at the upper torso so a half-hidden figure
     * still registers. The caller rate-limits this (≤1 / 5 ticks per GDD §8).
     */
    public static boolean isLookingAt(ServerPlayer viewer, Entity target, double tolerance) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 view = viewer.getViewVector(1.0F).normalize();
        Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.7, 0.0);
        return isLookingToward(eye, view, center, tolerance) && viewer.hasLineOfSight(target);
    }
}
