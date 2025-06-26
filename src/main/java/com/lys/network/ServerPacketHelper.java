package com.lys.network;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerPacketHelper {

    // 1.19.2中食用动画的常量值
    private static final int EATING_ANIMATION_ID = 9;

    // 发送白糖食用动画
    public static void sendSugarEatAnimation(ServerPlayerEntity eater) {
        EntityAnimationS2CPacket packet = new EntityAnimationS2CPacket(eater, EATING_ANIMATION_ID);
        sendToAllTracking(eater, packet);
    }

    // 发送白糖食用声音
    public static void sendSugarEatSound(ServerPlayerEntity eater, float volume, float pitch) {
        Vec3d pos = eater.getPos();
        PlaySoundS2CPacket packet = new PlaySoundS2CPacket(
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS,
                pos.x, pos.y, pos.z,
                volume,
                pitch,
                eater.getRandom().nextLong()
        );
        sendToAllTracking(eater, packet);
    }

    // 发送完整食用过程
    public static void sendFullEatingProcess(ServerPlayerEntity eater, int durationTicks) {
        // 初始声音
        sendSugarEatSound(eater, 0.8f, 1.0f + (eater.getRandom().nextFloat() - 0.5f) * 0.4f);

        // 创建调度器模拟持续动画
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final int interval = 4; // 每4tick发送一次动画更新

        for (int i = 1; i <= durationTicks / interval; i++) {
            int delay = i * interval * 50; // tick转毫秒
            scheduler.schedule(() -> {
                if (eater.isUsingItem()) {
                    // 发送中间动画帧
                    sendSugarEatAnimation(eater);

                    // 发送咀嚼音效
                    sendSugarEatSound(eater, 0.6f, 1.0f + (eater.getRandom().nextFloat() - 0.5f) * 0.4f);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        // 结束时关闭调度器
        scheduler.schedule(scheduler::shutdown, durationTicks * 50L, TimeUnit.MILLISECONDS);
    }

    // 发送/shit命令效果
    public static void sendShitCommandEffect(ServerPlayerEntity player) {
        Vec3d pos = player.getPos();

        // 播放声音
        PlaySoundS2CPacket soundPacket = new PlaySoundS2CPacket(
                SoundEvents.ENTITY_LLAMA_SPIT,
                SoundCategory.PLAYERS,
                pos.x, pos.y, pos.z,
                1.0f,
                0.8f + player.getRandom().nextFloat() * 0.4f,
                player.getRandom().nextLong()
        );
        sendToAllTracking(player, soundPacket);
    }

    // 辅助方法：发送给所有正在追踪此实体的玩家
    private static void sendToAllTracking(Entity entity, Object packet) {
        World world = entity.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            Collection<ServerPlayerEntity> players = serverWorld.getPlayers();

            for (ServerPlayerEntity player : players) {
                // 只发送给能看到实体的玩家
                if (player.canSee(entity)) {
                    player.networkHandler.sendPacket((net.minecraft.network.Packet<?>) packet);
                }
            }
        }
    }
}