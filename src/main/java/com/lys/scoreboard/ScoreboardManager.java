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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScoreboardManager {
    private static final Map<UUID, Boolean> playerVisibility = new ConcurrentHashMap<>();
    private static boolean globalEnabled = true;
    private static final List<String> boardTypes = Arrays.asList(
            "mined", "placed", "playtime", "deaths", "distance"
    );
    private static int currentBoardIndex = 0;
    private static ScheduledExecutorService rotationScheduler;
    private static final int ROTATION_INTERVAL = 30; // 秒
    private static ScoreboardConfig config;
    private static MinecraftServer server;

    public static void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        config = ScoreboardConfig.load();
        globalEnabled = config.globalEnabled;
        createObjectives(server);
        startRotationTask(server);
    }

    private static void createObjectives(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        // 创建计分板目标（如果不存在）
        if (scoreboard.getObjective("mined") == null) {
            scoreboard.addObjective("mined", ScoreboardCriterion.DUMMY,
                    formatText("挖掘榜单", Formatting.GOLD), ScoreboardCriterion.RenderType.INTEGER);
        }

        if (scoreboard.getObjective("placed") == null) {
            scoreboard.addObjective("placed", ScoreboardCriterion.DUMMY,
                    formatText("放置榜单", Formatting.GREEN), ScoreboardCriterion.RenderType.INTEGER);
        }

        if (scoreboard.getObjective("playtime") == null) {
            scoreboard.addObjective("playtime", ScoreboardCriterion.DUMMY,
                    formatText("在线时长", Formatting.BLUE), ScoreboardCriterion.RenderType.INTEGER);
        }

        // 使用DEATH_COUNT替代DEATHS
        if (scoreboard.getObjective("deaths") == null) {
            scoreboard.addObjective("deaths", ScoreboardCriterion.DEATH_COUNT,
                    formatText("死亡榜单", Formatting.RED), ScoreboardCriterion.RenderType.INTEGER);
        }

        if (scoreboard.getObjective("distance") == null) {
            scoreboard.addObjective("distance", ScoreboardCriterion.DUMMY,
                    formatText("移动距离", Formatting.YELLOW), ScoreboardCriterion.RenderType.INTEGER);
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
                currentBoardIndex = (currentBoardIndex + 1) % boardTypes.size();
                String nextBoard = boardTypes.get(currentBoardIndex);

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (isScoreboardVisible(player)) {
                        updateScoreboardDisplay(player, nextBoard);
                    }
                }
            });
        }, ROTATION_INTERVAL, ROTATION_INTERVAL, TimeUnit.SECONDS);
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
        config.globalEnabled = enabled;
        config.save();

        if (!enabled) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // 清除侧边栏显示
                player.getScoreboard().setObjectiveSlot(1, null);
            }
        }
    }

    public static void setPlayerVisibility(ServerPlayerEntity player, boolean visible) {
        playerVisibility.put(player.getUuid(), visible);
        config.updatePlayerVisibility(player.getUuid(), visible);

        if (globalEnabled) {
            if (visible) {
                updateScoreboardDisplay(player, boardTypes.get(currentBoardIndex));
            } else {
                // 清除该玩家的侧边栏显示
                player.getScoreboard().setObjectiveSlot(1, null);
            }
        }
    }

    public static boolean isScoreboardVisible(PlayerEntity player) {
        return globalEnabled && playerVisibility.getOrDefault(player.getUuid(), true);
    }

    private static MutableText formatText(String message, Formatting color) {
        return Text.literal("[榜单] ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(message).formatted(color));
    }
}