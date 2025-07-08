package com.lys.command;

import com.lys.scoreboard.ScoreboardManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

public class ScoreboardCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("scoreboard")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("global")
                        .then(CommandManager.literal("on").executes(context -> setGlobalEnabled(context.getSource(), true))
                                .then(CommandManager.literal("off").executes(context -> setGlobalEnabled(context.getSource(), false))
                                )
                                .then(CommandManager.literal("player")
                                        .then(CommandManager.argument("target", EntityArgumentType.players())
                                                .then(CommandManager.literal("on").executes(context -> setPlayerVisibility(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayers(context, "target"),
                                                        true
                                                )))
                                                .then(CommandManager.literal("off").executes(context -> setPlayerVisibility(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayers(context, "target"),
                                                        false
                                                )))
                                        )
                                ))));

        dispatcher.register(CommandManager.literal("myscoreboard")
                .executes(context -> togglePlayerVisibility(context.getSource(), context.getSource().getPlayer()))
        );
    }

    private static int setGlobalEnabled(ServerCommandSource source, boolean enabled) {
        ScoreboardManager.setGlobalEnabled(enabled);
        source.sendFeedback(formatText("全局计分板已" + (enabled ? "开启" : "关闭"),
                enabled ? Formatting.GREEN : Formatting.RED), true);
        return 1;
    }

    private static int setPlayerVisibility(ServerCommandSource source, Collection<ServerPlayerEntity> players, boolean visible) {
        for (ServerPlayerEntity player : players) {
            ScoreboardManager.setPlayerVisibility(player, visible);
        }

        // 修正的字符串拼接
        String message = "已" + (visible ? "开启" : "关闭") + players.size() + "位玩家的计分板显示";
        source.sendFeedback(formatText(message,
                visible ? Formatting.GREEN : Formatting.RED), true);
        return players.size();
    }

    private static int togglePlayerVisibility(ServerCommandSource source, ServerPlayerEntity player) {
        if (player == null) {
            source.sendError(formatText("只有玩家可以执行此命令", Formatting.RED));
            return 0;
        }

        boolean newVisibility = !ScoreboardManager.isScoreboardVisible(player);
        ScoreboardManager.setPlayerVisibility(player, newVisibility);

        source.sendFeedback(formatText("个人计分板已" + (newVisibility ? "开启" : "关闭"),
                newVisibility ? Formatting.GREEN : Formatting.RED), false);
        return 1;
    }

    private static MutableText formatText(String message, Formatting color) {
        return Text.literal("[榜单] ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(message).formatted(color));
    }
}