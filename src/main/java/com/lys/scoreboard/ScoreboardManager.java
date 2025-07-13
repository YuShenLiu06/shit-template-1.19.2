package com.lys.scoreboard;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScoreboardManager {
    private static final Map<UUID, Boolean> playerVisibility = new ConcurrentHashMap<>();
    private static boolean globalEnabled = true;
    // 移除放置榜单，只保留挖掘、在线时长和死亡榜单
    private static final List<String> boardTypes = Arrays.asList(
            "mined", "playtime", "deaths"
    );
    private static int currentBoardIndex = 0;
    private static ScheduledExecutorService rotationScheduler;
    private static final int ROTATION_INTERVAL = 30; // 秒
    private static MinecraftServer server;

    public static void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        createObjectives(server);
        startRotationTask(server);
        refreshAllScoreboards(); // 启动时刷新所有计分板
    }

    public static void shutdown() {
        if (rotationScheduler != null && !rotationScheduler.isShutdown()) {
            rotationScheduler.shutdownNow();
        }
    }

    private static void createObjectives(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        // 创建计分板目标（如果不存在）
        if (scoreboard.getObjective("mined") == null) {
            scoreboard.addObjective("mined", ScoreboardCriterion.DUMMY,
                    formatText("挖掘榜单", Formatting.GOLD), ScoreboardCriterion.RenderType.INTEGER);
        }

        if (scoreboard.getObjective("playtime") == null) {
            // 修改标题为"在线时长(min)"
            scoreboard.addObjective("playtime", ScoreboardCriterion.DUMMY,
                    formatText("在线时长(min)", Formatting.BLUE), ScoreboardCriterion.RenderType.INTEGER);
        }

        // 使用DEATH_COUNT替代DEATHS
        if (scoreboard.getObjective("deaths") == null) {
            scoreboard.addObjective("deaths", ScoreboardCriterion.DEATH_COUNT,
                    formatText("死亡榜单", Formatting.RED), ScoreboardCriterion.RenderType.INTEGER);
        }
    }

    private static void startRotationTask(MinecraftServer server) {
        if (rotationScheduler != null && !rotationScheduler.isShutdown()) {
            rotationScheduler.shutdown();
        }

        rotationScheduler = Executors.newSingleThreadScheduledExecutor();
        rotationScheduler.scheduleAtFixedRate(() -> {
            if (!globalEnabled) return;

            server.execute(() -> {
                rotateToNextBoard();
            });
        }, ROTATION_INTERVAL, ROTATION_INTERVAL, TimeUnit.SECONDS);
    }

    // 切换到下一个榜单
    public static void rotateToNextBoard() {
        currentBoardIndex = (currentBoardIndex + 1) % boardTypes.size();
        String nextBoard = boardTypes.get(currentBoardIndex);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isScoreboardVisible(player)) {
                updateScoreboardDisplay(player, nextBoard);
            }
        }
    }

    // 获取当前榜单名称
    public static String getCurrentBoardName() {
        return boardTypes.get(currentBoardIndex);
    }

    public static void updateScoreboardDisplay(ServerPlayerEntity player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjective(objectiveName);

        if (objective != null) {
            // 使用1作为侧边栏显示槽位
            scoreboard.setObjectiveSlot(1, objective);
        }
    }

    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;

        if (!enabled) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // 清除侧边栏显示
                player.getScoreboard().setObjectiveSlot(1, null);
            }
        } else {
            // 启用时刷新所有计分板
            refreshAllScoreboards();
        }
    }

    public static void setPlayerVisibility(ServerPlayerEntity player, boolean visible) {
        playerVisibility.put(player.getUuid(), visible);
        ScoreboardConfig.getConfig().updatePlayerVisibility(player.getUuid(), visible);

        if (globalEnabled) {
            if (visible) {
                updateScoreboardDisplay(player, boardTypes.get(currentBoardIndex));
            } else {
                // 清除该玩家的侧边栏显示
                player.getScoreboard().setObjectiveSlot(1, null);
            }
        }

        // 发送状态提示
        player.sendMessage(
                Text.literal("[榜单] 个人榜单显示已" + (visible ? "开启" : "关闭"))
                        .formatted(visible ? Formatting.GREEN : Formatting.RED),
                false
        );
    }

    public static boolean isScoreboardVisible(PlayerEntity player) {
        // 全局关闭时强制隐藏
        if (!globalEnabled) return false;

        // 全局开启时使用个人设置
        return playerVisibility.getOrDefault(player.getUuid(), true);
    }

    // 强制刷新所有玩家的计分板 (添加空检查)
    public static void refreshAllScoreboards() {
        if (server == null) return;

        // 关键修复：检查玩家管理器是否初始化
        if (server.getPlayerManager() == null) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (isScoreboardVisible(player)) {
                updateScoreboardDisplay(player, boardTypes.get(currentBoardIndex));
            }
        }
    }

    private static MutableText formatText(String message, Formatting color) {
        return Text.literal("[榜单] ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(message).formatted(color));
    }
}