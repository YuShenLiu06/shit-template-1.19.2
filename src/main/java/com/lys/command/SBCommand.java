package com.lys.command;

import com.lys.scoreboard.PlayTimeStorage;
import com.lys.scoreboard.ScoreboardDataStorage;
import com.lys.scoreboard.ScoreboardManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.UUID;

public class SBCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 主命令改为 "lb" (榜单)
        dispatcher.register(CommandManager.literal("lb")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("global")
                        .then(CommandManager.literal("on")
                                .executes(context -> setGlobalEnabled(context.getSource(), true)))
                        .then(CommandManager.literal("off")
                                .executes(context -> setGlobalEnabled(context.getSource(), false))))
                .then(CommandManager.literal("player")
                        .then(CommandManager.argument("target", EntityArgumentType.players())
                                .then(CommandManager.literal("on")
                                        .executes(context -> setPlayerVisibility(
                                                context.getSource(),
                                                EntityArgumentType.getPlayers(context, "target"),
                                                true)))
                                .then(CommandManager.literal("off")
                                        .executes(context -> setPlayerVisibility(
                                                context.getSource(),
                                                EntityArgumentType.getPlayers(context, "target"),
                                                false)))))
                .then(CommandManager.literal("next")
                        .executes(context -> rotateToNextBoard(context.getSource())))
                .then(CommandManager.literal("refresh")
                        .executes(context -> refreshScoreboards(context.getSource())))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> showPlayerInfo(
                                        context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")))
                                .then(CommandManager.literal("reset")
                                        .executes(context -> resetPlayerData(
                                                context.getSource(),
                                                EntityArgumentType.getPlayer(context, "player")))))));

        // 玩家个人命令改为 "mylb"
        dispatcher.register(CommandManager.literal("mylb")
                .executes(context -> togglePlayerVisibility(context.getSource(), context.getSource().getPlayer())));
    }

    private static int setGlobalEnabled(ServerCommandSource source, boolean enabled) {
        ScoreboardManager.setGlobalEnabled(enabled);
        source.sendFeedback(formatText("全局榜单已" + (enabled ? "开启" : "关闭"),
                enabled ? Formatting.GREEN : Formatting.RED), true);
        return 1;
    }

    private static int setPlayerVisibility(ServerCommandSource source, Collection<ServerPlayerEntity> players, boolean visible) {
        for (ServerPlayerEntity player : players) {
            ScoreboardManager.setPlayerVisibility(player, visible);
        }

        String message = "已" + (visible ? "开启" : "关闭") + players.size() + "位玩家的榜单显示";
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

        source.sendFeedback(formatText("个人榜单已" + (newVisibility ? "开启" : "关闭"),
                newVisibility ? Formatting.GREEN : Formatting.RED), false);
        return 1;
    }

    // 刷新所有榜单
    private static int refreshScoreboards(ServerCommandSource source) {
        ScoreboardManager.refreshAllScoreboards();
        source.sendFeedback(formatText("已刷新所有榜单", Formatting.GREEN), true);
        return 1;
    }

    // 显示玩家调试信息
    private static int showPlayerInfo(ServerCommandSource source, ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        // 获取在线时间（格式化）
        String playTime = com.lys.Shit.getFormattedPlayTime(uuid);

        // 获取各个榜单值
        int mined = getPlayerScoreValue(player, "mined");
        int playtime = getPlayerScoreValue(player, "playtime"); // 这是总分钟数
        int deaths = getPlayerScoreValue(player, "deaths");

        source.sendFeedback(formatText(player.getName().getString() + "的榜单信息:", Formatting.YELLOW), false);
        source.sendFeedback(formatText("总在线时间: " + playTime, Formatting.AQUA), false);
        source.sendFeedback(formatText("挖掘方块: " + mined, Formatting.GOLD), false);
        source.sendFeedback(formatText("死亡次数: " + deaths, Formatting.RED), false);

        return 1;
    }

    // 重置玩家数据
    private static int resetPlayerData(ServerCommandSource source, ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        // 重置在线时间
        PlayTimeStorage.resetPlayTime(uuid);

        // 重置榜单数据
        ScoreboardDataStorage.resetPlayerScore(uuid, "mined");
        ScoreboardDataStorage.resetPlayerScore(uuid, "deaths");

        // 重置榜单分数
        resetPlayerScore(player, "mined");
        resetPlayerScore(player, "playtime");
        resetPlayerScore(player, "deaths");

        source.sendFeedback(formatText("已重置玩家 " + player.getName().getString() + " 的榜单数据", Formatting.GREEN), true);
        return 1;
    }

    // 重置玩家分数
    private static void resetPlayerScore(ServerPlayerEntity player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) {
            scoreboard.getPlayerScore(player.getName().getString(), objective).setScore(0);
        }
    }

    // 获取玩家分数值
    private static int getPlayerScoreValue(ServerPlayerEntity player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) {
            return scoreboard.getPlayerScore(player.getName().getString(), objective).getScore();
        }
        return -1;
    }

    // 切换到下一个榜单
    private static int rotateToNextBoard(ServerCommandSource source) {
        ScoreboardManager.rotateToNextBoard();
        String currentBoard = ScoreboardManager.getCurrentBoardName();
        source.sendFeedback(formatText("已切换到榜单: " + currentBoard, Formatting.GREEN), true);
        return 1;
    }

    private static MutableText formatText(String message, Formatting color) {
        return Text.literal("[榜单] ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(message).formatted(color));
    }
}