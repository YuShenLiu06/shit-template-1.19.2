package com.lys;

import com.lys.command.SBCommand;
import com.lys.command.ShitCommand;
import com.lys.scoreboard.PlayTimeStorage;
import com.lys.scoreboard.ScoreboardManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Shit implements ModInitializer {
	// 存储玩家登录时间（毫秒）
	private static final Map<UUID, Long> loginTimeMap = new ConcurrentHashMap<>();
	// 存储玩家当前会话的秒数
	private static final Map<UUID, Long> currentSessionSecondsMap = new ConcurrentHashMap<>();
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ShitCommand.register(dispatcher);
			SBCommand.register(dispatcher);
		});

		// 服务器启动事件
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			Shit.server = server;
			ScoreboardManager.initialize(server);
			PlayTimeStorage.load(server); // 加载在线时间数据
		});

		// 服务器停止事件
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			PlayTimeStorage.save(server); // 保存在线时间数据
		});

		// 玩家登录事件
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			UUID uuid = player.getUuid();
			loginTimeMap.put(uuid, System.currentTimeMillis());
			currentSessionSecondsMap.put(uuid, 0L); // 重置当前会话时间
			ScoreboardManager.setPlayerVisibility(player, true);
		});

		// 玩家退出事件
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			UUID uuid = player.getUuid();
			updatePlayTime(uuid, true); // 保存并重置
			loginTimeMap.remove(uuid);
			currentSessionSecondsMap.remove(uuid);
		});

		// 每分钟更新一次总时间
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (server.getTicks() % 1200 == 0) { // 每分钟（1200tick）
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					UUID uuid = player.getUuid();
					updatePlayTime(uuid, false);
					// 更新计分板显示
					updateScoreboardDisplay(uuid, player);
				}
			}
		});

		// 方块挖掘事件
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
			if (!world.isClient) {
				onBlockBreak(player);
			}
		});
	}

	// 方块挖掘事件处理
	public static void onBlockBreak(PlayerEntity player) {
		updateScoreboardValue(player, "mined");
	}

	// 更新榜单值（通用方法）
	private static void updateScoreboardValue(PlayerEntity player, String objectiveName) {
		Scoreboard scoreboard = player.getScoreboard();
		ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
		if (objective != null) {
			scoreboard.getPlayerScore(player.getName().getString(), objective).incrementScore();
		}
	}

	// 更新计分板显示
	private static void updateScoreboardDisplay(UUID uuid, ServerPlayerEntity player) {
		// 计算总时间（秒）
		long totalSeconds = PlayTimeStorage.getPlayTimeSeconds(uuid) +
				currentSessionSecondsMap.getOrDefault(uuid, 0L);

		// 转换为分钟用于计分板显示
		int totalMinutes = (int) (totalSeconds / 60);

		Scoreboard scoreboard = player.getScoreboard();
		ScoreboardObjective objective = scoreboard.getObjective("playtime");
		if (objective != null) {
			scoreboard.getPlayerScore(player.getName().getString(), objective).setScore(totalMinutes);
		}
	}

	// 更新在线时间（重构版）
	private void updatePlayTime(UUID uuid, boolean saveTotal) {
		Long loginTime = loginTimeMap.get(uuid);
		if (loginTime != null) {
			// 计算当前会话的秒数
			long elapsedSeconds = (System.currentTimeMillis() - loginTime) / 1000;

			// 更新当前会话时间
			currentSessionSecondsMap.put(uuid, elapsedSeconds);

			// 每分钟保存一次或退出时保存
			if (saveTotal || (elapsedSeconds > 0 && elapsedSeconds % 60 == 0)) {
				// 添加到总时间
				PlayTimeStorage.addPlayTimeSeconds(uuid, elapsedSeconds);
				// 重置登录时间
				loginTimeMap.put(uuid, System.currentTimeMillis());
				// 重置当前会话时间
				currentSessionSecondsMap.put(uuid, 0L);
			}
		}
	}

	// 获取玩家在线时间（格式化）
	public static String getFormattedPlayTime(UUID uuid) {
		// 总时间 = 存储的总时间 + 当前会话时间
		long totalSeconds = PlayTimeStorage.getPlayTimeSeconds(uuid) +
				currentSessionSecondsMap.getOrDefault(uuid, 0L);

		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		return hours + "h" + minutes + "min";
	}

	// 获取服务器实例
	public static MinecraftServer getServer() {
		return server;
	}
}