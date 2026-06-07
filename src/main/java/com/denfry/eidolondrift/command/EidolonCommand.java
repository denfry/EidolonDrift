package com.denfry.eidolondrift.command;

import java.util.Locale;
import java.util.stream.Collectors;

import com.denfry.eidolondrift.compat.Integration;
import com.denfry.eidolondrift.compat.ModIntegrations;
import com.denfry.eidolondrift.config.ModConfig;
import com.denfry.eidolondrift.director.AnomalyDirector;
import com.denfry.eidolondrift.director.AnomalyRegistry;
import com.denfry.eidolondrift.memory.HomeMemory;
import com.denfry.eidolondrift.memory.PlayerWorldMemory;
import com.denfry.eidolondrift.mind.MindState;
import com.denfry.eidolondrift.mind.MindStateManager;
import com.denfry.eidolondrift.network.CancelClientAnomalyPayload;
import com.denfry.eidolondrift.network.EidolonNetworking;
import com.denfry.eidolondrift.observer.ObserverPhase;
import com.denfry.eidolondrift.observer.ObserverSpawnManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /eidolon} admin / debug command tree (PLAN §7 — debug tooling first). All
 * subcommands target a player (explicit arg, or the executor). Player-facing horror never
 * exposes raw MindState numbers; this is an op-only inspection tool (permission level 2).
 */
public final class EidolonCommand {

    private static final int PERMISSION_LEVEL = 2;

    /** Settable MindState fields (distortion is derived and intentionally excluded). */
    private static final String[] FIELDS = {
            "dread", "suspicion", "attachment", "isolation", "routine", "memoryPressure",
            "homeCorruption", "caveResonance", "sleepDebt", "echoDensity"
    };

    private static final SuggestionProvider<CommandSourceStack> FIELD_SUGGESTIONS =
            (ctx, b) -> SharedSuggestionProvider.suggest(FIELDS, b);
    private static final SuggestionProvider<CommandSourceStack> ANOMALY_SUGGESTIONS =
            (ctx, b) -> SharedSuggestionProvider.suggest(
                    AnomalyRegistry.all().stream().map(a -> a.id().toString()), b);
    private static final SuggestionProvider<CommandSourceStack> PHASE_SUGGESTIONS =
            (ctx, b) -> SharedSuggestionProvider.suggest(
                    java.util.Arrays.stream(ObserverPhase.values())
                            .map(p -> p.name().toLowerCase(Locale.ROOT)), b);

    private EidolonCommand() {}

    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eidolon")
                .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                .then(Commands.literal("status")
                        .executes(c -> status(c, self(c)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("memory")
                        .executes(c -> memory(c, self(c)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> memory(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("stage")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 9))
                                .executes(c -> stage(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> stage(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("field", StringArgumentType.word()).suggests(FIELD_SUGGESTIONS)
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 100f))
                                        .executes(c -> set(c, self(c)))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> set(c, EntityArgument.getPlayer(c, "player")))))))
                .then(Commands.literal("setdrift")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 100f))
                                .executes(c -> setDrift(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> setDrift(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("anomaly")
                        .then(Commands.literal("list").executes(EidolonCommand::anomalyList))
                        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(ANOMALY_SUGGESTIONS)
                                .executes(c -> anomaly(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> anomaly(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("observer")
                        .then(Commands.literal("spawn")
                                .executes(c -> observerSpawn(c, self(c), null))
                                .then(Commands.argument("phase", StringArgumentType.word()).suggests(PHASE_SUGGESTIONS)
                                        .executes(c -> observerSpawn(c, self(c), StringArgumentType.getString(c, "phase")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> observerSpawn(c, EntityArgument.getPlayer(c, "player"),
                                                        StringArgumentType.getString(c, "phase"))))))
                        .then(Commands.literal("clear")
                                .executes(c -> observerClear(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> observerClear(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("info")
                                .executes(c -> observerInfo(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> observerInfo(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("home")
                        .then(Commands.literal("info")
                                .executes(c -> homeInfo(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> homeInfo(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("clear")
                                .executes(c -> homeClear(c, self(c)))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> homeClear(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("integrations").executes(EidolonCommand::integrations))
                .then(Commands.literal("reload").executes(EidolonCommand::reload)));
    }

    // ── subcommands ─────────────────────────────────────────────────────────────

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        MindState ms = MindStateManager.get(p);
        PlayerWorldMemory mem = MindStateManager.memory(p);
        mem.fearProfile.recalculate(mem, ms);

        var src = c.getSource();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.header"), false);
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.stage", ms.progressionStage), false);
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.mind",
                f(ms.dread), f(ms.suspicion), f(ms.isolation), f(ms.distortion)), false);
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.place",
                f(ms.routine), f(ms.homeCorruption), f(ms.caveResonance), f(ms.sleepDebt)), false);
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.fear",
                mem.fearProfile.primaryFear, f(mem.fearProfile.habitStrength * 100f),
                AnomalyDirector.memory().sessionCount(p), AnomalyDirector.SESSION_BUDGET), false);
        return 1;
    }

    private static int memory(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        PlayerWorldMemory mem = MindStateManager.memory(p);
        var src = c.getSource();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.memory.header"), false);
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.memory.line",
                mem.knownChunks.size(), mem.timesSlept, mem.lastSleptDay, f(mem.homeCorruptionLevel)), false);
        return 1;
    }

    private static int stage(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        int value = IntegerArgumentType.getInteger(c, "value");
        MindState ms = MindStateManager.get(p);
        ms.progressionStage = value;
        ms.recomputeDistortion();
        ms.clampAll();
        MindStateManager.set(p, ms);
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.stage.success", value), true);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        String field = StringArgumentType.getString(c, "field");
        float value = FloatArgumentType.getFloat(c, "value");
        if (!applyField(p, field, value)) {
            c.getSource().sendFailure(Component.translatable("command.eidolon_drift.set.unknown", field));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.set.success", field, f(value)), true);
        return 1;
    }

    private static int setDrift(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        float value = FloatArgumentType.getFloat(c, "value");
        applyField(p, "dread", value);
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.set.success", "dread", f(value)), true);
        return 1;
    }

    private static int anomaly(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        ResourceLocation id = ResourceLocationArgument.getId(c, "id");
        if (AnomalyDirector.forceFire(p, id)) {
            c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.anomaly.fired", id.toString()), true);
            return 1;
        }
        c.getSource().sendFailure(Component.translatable("command.eidolon_drift.anomaly.unknown", id.toString()));
        return 0;
    }

    private static int anomalyList(CommandContext<CommandSourceStack> c) {
        String ids = AnomalyRegistry.all().stream().map(a -> a.id().getPath()).collect(Collectors.joining(", "));
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.anomaly.list",
                AnomalyRegistry.size(), ids), false);
        return 1;
    }

    private static int observerSpawn(CommandContext<CommandSourceStack> c, ServerPlayer p, String phaseArg) {
        if (p == null) return noTarget(c);
        ObserverPhase phase = ObserverPhase.PERIPHERAL;
        if (phaseArg != null) {
            try {
                phase = ObserverPhase.valueOf(phaseArg.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                c.getSource().sendFailure(Component.translatable("command.eidolon_drift.observer.bad_phase", phaseArg));
                return 0;
            }
        }
        ObserverPhase live = ObserverSpawnManager.debugSpawn(p, phase);
        final ObserverPhase shown = live;
        c.getSource().sendSuccess(() -> Component.translatable(
                "command.eidolon_drift.observer.spawned", shown.name().toLowerCase(Locale.ROOT)), true);
        return 1;
    }

    private static int observerClear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        boolean had = ObserverSpawnManager.debugClear(p);
        c.getSource().sendSuccess(() -> Component.translatable(
                had ? "command.eidolon_drift.observer.cleared" : "command.eidolon_drift.observer.none"), true);
        return 1;
    }

    private static int observerInfo(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        String info = ObserverSpawnManager.debugInfo(p);
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.observer.info", info), false);
        return 1;
    }

    private static int homeInfo(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        PlayerWorldMemory mem = MindStateManager.memory(p);
        MindState ms = MindStateManager.get(p);
        var home = mem.home();
        var src = c.getSource();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.home.header"), false);
        if (!home.hasHome()) {
            src.sendSuccess(() -> Component.translatable("command.eidolon_drift.home.none"), false);
            return 1;
        }
        var anchor = home.anchor.get();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.home.line",
                anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ(),
                HomeMemory.HALF_EXTENT_XZ * 2 + 1, f(ms.homeCorruption)), false);
        return 1;
    }

    private static int homeClear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (p == null) return noTarget(c);
        EidolonNetworking.sendToPlayer(p, CancelClientAnomalyPayload.all());
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.home.cleared"), true);
        return 1;
    }

    private static int integrations(CommandContext<CommandSourceStack> c) {
        var src = c.getSource();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.integrations.header"), false);
        boolean master = ModConfig.INTEGRATIONS_ENABLED.get();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.integrations.master",
                state(master)), false);
        for (Integration in : Integration.values()) {
            boolean present = ModIntegrations.isPresent(in);
            boolean active = ModIntegrations.isActive(in);
            String key = !present
                    ? "command.eidolon_drift.integrations.absent"
                    : active
                            ? "command.eidolon_drift.integrations.active"
                            : "command.eidolon_drift.integrations.disabled";
            src.sendSuccess(() -> Component.translatable(key,
                    in.key, in.modId, in.category.name().toLowerCase(Locale.ROOT)), false);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.translatable("command.eidolon_drift.reload.success"), true);
        return 1;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Mutate one MindState field; returns false for an unknown field name. */
    private static boolean applyField(ServerPlayer p, String field, float value) {
        MindState ms = MindStateManager.get(p);
        switch (field) {
            case "dread" -> ms.dread = value;
            case "suspicion" -> ms.suspicion = value;
            case "attachment" -> ms.attachment = value;
            case "isolation" -> ms.isolation = value;
            case "routine" -> ms.routine = value;
            case "memoryPressure" -> ms.memoryPressure = value;
            case "homeCorruption" -> ms.homeCorruption = value;
            case "caveResonance" -> ms.caveResonance = value;
            case "sleepDebt" -> ms.sleepDebt = value;
            case "echoDensity" -> ms.echoDensity = value;
            default -> { return false; }
        }
        ms.recomputeDistortion();
        ms.clampAll();
        MindStateManager.set(p, ms);
        return true;
    }

    /** Resolve the executing player, or {@code null} when run from console/RCON. */
    private static ServerPlayer self(CommandContext<CommandSourceStack> c) {
        return c.getSource().getPlayer();
    }

    private static int noTarget(CommandContext<CommandSourceStack> c) {
        c.getSource().sendFailure(Component.translatable("command.eidolon_drift.no_target"));
        return 0;
    }

    private static String f(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String state(boolean on) {
        return on ? "on" : "off";
    }
}
