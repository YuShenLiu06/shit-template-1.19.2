package com.lys;

import com.lys.command.ScoreboardCommand;
import com.lys.command.ShitCommand;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Shit implements ModInitializer {
	private static final Map<UUID, Long> loginTimeMap = new ConcurrentHashMap<>();
	private static final Map<UUID, Vec3d> lastPositions = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ShitCommand.register(dispatcher);
			ScoreboardCommand.register(dispatcher);
		});

		// 注册计分板系统
		ServerLifecycleEvents.SERVER_STARTING.register(ScoreboardManager::initialize);

		// 玩家登录事件
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			UUID uuid = player.getUuid();
			loginTimeMap.put(uuid, System.currentTimeMillis());
			lastPositions.put(uuid, player.getPos());
			ScoreboardManager.setPlayerVisibility(player, true);
		});

		// 玩家退出事件
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.player;
			UUID uuid = player.getUuid();
			updatePlayTime(uuid, player);
			updateDistance(uuid, player.getPos(), player);
			loginTimeMap.remove(uuid);
			lastPositions.remove(uuid);
		});

		// 每5秒更新一次数据
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (server.getTicks() % 100 == 0) { // 每5秒（100tick）
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					UUID uuid = player.getUuid();
					updatePlayTime(uuid, player);
					updateDistance(uuid, player.getPos(), player);
				}
			}
		});

		// 方块挖掘事件
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
			if (!world.isClient) {
				onBlockBreak(player);
			}
		});

		// 使用替代方法处理方块放置事件
		// 由于1.19.2 Fabric的PlayerBlockPlaceEvents存在问题，使用其他方法
		// 我们将在玩家刻事件中检测方块放置
	}

	// 方块挖掘事件处理
	public static void onBlockBreak(PlayerEntity player) {
		Scoreboard scoreboard = player.getScoreboard();
		ScoreboardObjective objective = scoreboard.getObjective("mined");
		if (objective != null) {
			scoreboard.getPlayerScore(player.getName().getString(), objective).incrementScore();
		}
	}

	// 在玩家刻事件中处理方块放置（替代方案）
	public static void onPlayerTick(ServerPlayerEntity player) {
		// 这里可以检测方块放置
		// 实际实现需要更复杂的逻辑来检测变化
	}

	private void updatePlayTime(UUID uuid, ServerPlayerEntity player) {
		Long loginTime = loginTimeMap.get(uuid);
		if (loginTime != null) {
			int seconds = (int) ((System.currentTimeMillis() - loginTime) / 1000);
			Scoreboard scoreboard = player.getScoreboard();
			ScoreboardObjective objective = scoreboard.getObjective("playtime");
			if (objective != null) {
				scoreboard.getPlayerScore(player.getName().getString(), objective)
						.incrementScore(seconds);
			}
			loginTimeMap.put(uuid, System.currentTimeMillis());
		}
	}

	private void updateDistance(UUID uuid, Vec3d currentPos, ServerPlayerEntity player) {
		Vec3d lastPos = lastPositions.get(uuid);
		if (lastPos != null) {
			double distance = currentPos.distanceTo(lastPos);
			Scoreboard scoreboard = player.getScoreboard();
			ScoreboardObjective objective = scoreboard.getObjective("distance");
			if (objective != null) {
				scoreboard.getPlayerScore(player.getName().getString(), objective)
						.incrementScore((int) (distance * 100)); // 转换为厘米
			}
		}
		lastPositions.put(uuid, currentPos);
	}
}