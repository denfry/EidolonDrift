package com.denfry.eidolondrift.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /eidolon} admin command tree. Debug tooling is built early (per PLAN §7) so
 * every later milestone is testable. M0 ships {@code status} + {@code reload} stubs;
 * subcommands ({@code anomaly}, {@code memory inspect}, {@code debug client}, ...) are
 * added as their systems land.
 */
public final class EidolonCommand {

    private static final int PERMISSION_LEVEL = 2; // ops / single-player cheats

    private EidolonCommand() {}

    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eidolon")
                .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                .then(Commands.literal("status").executes(EidolonCommand::status))
                .then(Commands.literal("reload").executes(EidolonCommand::reload)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.header"), false);
        // Stage is hard-wired to 0 until MindState (L1/M1) exists.
        src.sendSuccess(() -> Component.translatable("command.eidolon_drift.status.stage", 0), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        // Config auto-reloads via NeoForge's file watcher; this is an explicit ack for now.
        ctx.getSource().sendSuccess(
                () -> Component.translatable("command.eidolon_drift.reload.success"), true);
        return 1;
    }
}
