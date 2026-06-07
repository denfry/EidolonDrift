package com.denfry.eidolondrift.observer;

import java.util.Optional;
import java.util.UUID;

import com.denfry.eidolondrift.mind.MindStateManager;
import com.denfry.eidolondrift.util.PlayerLookUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Observer (GDD §8): not a boss — no loot, no HP bar, no melee. A category error in the
 * world that reacts to being <i>looked at</i>, retreats from light, and is rendered for exactly
 * <b>one</b> player (invariant §2, enforced by {@link #broadcastToPlayer}).
 *
 * <p>It carries no goal AI ({@code setNoAi(true)}); all motion is deliberate teleport relocation,
 * so there is no navigation to exploit (M2 done-criterion). It is {@code noSave} — a transient
 * presence respawned from {@link MindState} by {@link ObserverSpawnManager}, never persisted.
 */
public final class ObserverEntity extends Monster {

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(ObserverEntity.class, EntityDataSerializers.BYTE);

    /** Stare tolerance fed to {@link PlayerLookUtil} (GDD §8 pseudocode uses 0.12). */
    private static final double LOOK_TOLERANCE = 0.12;
    /** Drop the Observer once the target gets this far away (just past tracking range). */
    private static final double DESPAWN_RANGE = 140.0;
    /** SEEN vanishes when the player closes within this distance. */
    private static final double SEEN_FLEE_DIST = 8.0;
    /** PERIPHERAL slips back to the edge of vision when the player closes within this distance. */
    private static final double PERIPHERAL_FLEE_DIST = 16.0;

    private UUID targetPlayerUUID;
    private int viewCheckCooldown = 0;
    private int retreatCooldown = 0;

    public ObserverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
        this.setNoAi(true);                 // no goals → no pathfinding to exploit
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    /** Monsters need default attributes; the Observer is effectively unkillable (see {@link #hurt}). */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty — the Observer never paths or fights.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, (byte) ObserverPhase.PERIPHERAL.ordinal());
    }

    // ── identity / phase ─────────────────────────────────────────────────────────

    public void setTargetPlayer(UUID uuid) {
        this.targetPlayerUUID = uuid;
    }

    public Optional<UUID> getTargetPlayer() {
        return Optional.ofNullable(targetPlayerUUID);
    }

    public ObserverPhase getPhase() {
        return ObserverPhase.byOrdinal(this.entityData.get(DATA_PHASE));
    }

    public void setPhase(ObserverPhase phase) {
        this.entityData.set(DATA_PHASE, (byte) phase.cappedToImplemented().ordinal());
    }

    /**
     * Per-player unicast for a server-real entity (invariant §2): only the target client ever
     * tracks — and therefore renders or minimaps — this Observer. Others receive nothing.
     */
    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return targetPlayerUUID != null && targetPlayerUUID.equals(player.getUUID());
    }

    // ── lifecycle ────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        ServerPlayer target = resolveTarget();
        if (target == null || target.level() != this.level()
                || this.distanceToSqr(target) > DESPAWN_RANGE * DESPAWN_RANGE) {
            discard();
            return;
        }

        faceToward(target);

        if (--viewCheckCooldown <= 0) {
            viewCheckCooldown = 5;                          // ≤1 raycast / 5 ticks (GDD §8)
            if (PlayerLookUtil.isLookingAt(target, this, LOOK_TOLERANCE)) {
                onObserved(target);
            }
        }

        if (retreatCooldown > 0) retreatCooldown--;
        else if (getPhase().retreatsFromLight()
                && this.level().getMaxLocalRawBrightness(this.blockPosition()) > 11) {
            retreatFromLight(target);
        }

        switch (getPhase()) {
            case PERIPHERAL -> tickPeripheral(target);
            case SEEN -> tickSeen(target);
            default -> { /* IMPLIED has no entity; NEAR+ land in later milestones */ }
        }
    }

    /** The Observer never persists and never despawns on its own terms — only {@link #tick} discards it. */
    @Override
    public void checkDespawn() {
        // no-op
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ── reactions (GDD §8) ───────────────────────────────────────────────────────

    private void onObserved(ServerPlayer p) {
        MindStateManager.addDread(p, 8.0f);
        switch (getPhase()) {
            case PERIPHERAL -> relocateToEdgeOfVision(p);
            case SEEN -> teleportAwayFrom(p, 80.0);
            default -> { }
        }
    }

    /**
     * "Attacking" it is ineffective (GDD §8): no damage is ever taken. A hit from the target
     * spikes dread+suspicion and despawns the figure (PERIPHERAL/SEEN). Environmental damage is
     * ignored so it can't be accidentally killed off by the world.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide
                && source.getEntity() instanceof ServerPlayer p
                && p.getUUID().equals(targetPlayerUUID)) {
            MindStateManager.addDread(p, 20.0f);
            MindStateManager.addSuspicion(p, 15.0f);
            discard();
        }
        return false;                                       // never takes real damage → no death, no loot
    }

    private void tickPeripheral(ServerPlayer p) {
        if (this.distanceToSqr(p) < PERIPHERAL_FLEE_DIST * PERIPHERAL_FLEE_DIST) {
            relocateToEdgeOfVision(p);
        }
    }

    private void tickSeen(ServerPlayer p) {
        if (this.distanceToSqr(p) < SEEN_FLEE_DIST * SEEN_FLEE_DIST) {
            teleportAwayFrom(p, 80.0);
        }
    }

    private void retreatFromLight(ServerPlayer p) {
        retreatCooldown = 100;                              // 5 s before it can retreat again
        teleportAwayFrom(p, 45.0);
    }

    // ── placement ────────────────────────────────────────────────────────────────

    /** Slip to the player's peripheral arc (~50 blocks, well off the centre of their view). */
    private void relocateToEdgeOfVision(ServerPlayer p) {
        float side = p.getRandom().nextBoolean() ? 1f : -1f;
        float angle = p.getYRot() + side * (60f + p.getRandom().nextFloat() * 50f);
        teleportTo(p, angle, 42.0 + p.getRandom().nextDouble() * 26.0);
    }

    /** Vanish to a random bearing roughly {@code dist} blocks from the player. */
    private void teleportAwayFrom(ServerPlayer p, double dist) {
        float angle = p.getRandom().nextFloat() * 360f;
        teleportTo(p, angle, dist);
    }

    /** Place on the surface at {@code distance} along {@code yawDeg} from the player, if loaded. */
    private void teleportTo(ServerPlayer p, float yawDeg, double distance) {
        double rad = Math.toRadians(yawDeg);
        double x = p.getX() - Math.sin(rad) * distance;
        double z = p.getZ() + Math.cos(rad) * distance;
        groundPos(p.serverLevel(), x, z).ifPresent(pos -> {
            this.moveTo(pos.x, pos.y, pos.z, yawDeg, 0f);
            this.setYHeadRot(yawDeg);
            this.setYBodyRot(yawDeg);
        });
    }

    private static Optional<Vec3> groundPos(ServerLevel level, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level.isLoaded(new net.minecraft.core.BlockPos(bx, level.getMinBuildHeight() + 1, bz))) {
            return Optional.empty();
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return Optional.of(new Vec3(bx + 0.5, y, bz + 0.5));
    }

    private void faceToward(ServerPlayer p) {
        double dx = p.getX() - this.getX();
        double dz = p.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(p.getEyeY() - this.getEyeY(), horiz)));
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    private ServerPlayer resolveTarget() {
        if (targetPlayerUUID == null || !(this.level() instanceof ServerLevel level)) return null;
        Player p = level.getPlayerByUUID(targetPlayerUUID);
        return (p instanceof ServerPlayer sp && sp.isAlive()) ? sp : null;
    }
}
