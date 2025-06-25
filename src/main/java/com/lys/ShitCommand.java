package com.lys;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShitCommand {
    private static final Set<UUID> forbiddenPlayers = new HashSet<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("shit")
                .executes(context -> executeShit(context.getSource(), context.getSource().getPlayer()))
                .then(CommandManager.literal("forbid")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> forbidPlayer(
                                        context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")
                                ))
                        )
                )
                .then(CommandManager.literal("allow")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> allowPlayer(
                                        context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")
                                ))
                        )
                )
        );
    }

    private static int executeShit(ServerCommandSource source, ServerPlayerEntity player) {
        if (player == null) {
            source.sendError(formatText("只有玩家可以执行此命令", Formatting.RED));
            return 0;
        }

        if (forbiddenPlayers.contains(player.getUuid())) {
            source.sendError(formatText("你没有使用此命令的权限", Formatting.RED));
            return 0;
        }

        if (player.getHungerManager().getFoodLevel() < 5) {
            player.sendMessage(formatText("你不能再拉啦，都要饿死了", Formatting.YELLOW), false);
            return 0;
        }

        int cost = 3 + player.getRandom().nextInt(3);
        player.getHungerManager().setFoodLevel(player.getHungerManager().getFoodLevel() - cost);

        World world = player.getWorld();
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_SPLASH,
                SoundCategory.PLAYERS,
                1.0f,
                0.8f + player.getRandom().nextFloat() * 0.4f
        );

        player.sendMessage(formatText("噗~", Formatting.GOLD), false);

        // 获取玩家精确的臀部位置
        Vec3d playerPos = getButtPosition(player);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        int count = 8 + player.getRandom().nextInt(5);
        int baseDelay = 200; // 基础延迟200ms
        int delayVariance = 100; // 延迟变化范围±100ms

        for (int i = 0; i < count; i++) {
            final int index = i;
            int delay = baseDelay * i + player.getRandom().nextInt(delayVariance) - delayVariance/2;

            scheduler.schedule(() -> {
                if (!player.isAlive()) return;

                world.getServer().execute(() -> {
                    // 创建腐肉实体
                    ItemEntity item = new ItemEntity(
                            world,
                            playerPos.x, playerPos.y, playerPos.z,
                            new ItemStack(Items.ROTTEN_FLESH)
                    );

                    // 设置拾取延迟（防止立即拾取）
                    item.setPickupDelay(40); // 2秒（20 ticks/秒）

                    // 计算精确的喷射方向（与玩家视角相反）
                    Vec3d velocity = calculateShitVelocity(player);

                    // 添加随机旋转效果
                    item.setYaw(player.getRandom().nextFloat() * 360.0F);
                    item.setPitch(player.getRandom().nextFloat() * 180.0F - 90.0F);

                    // 应用速度
                    item.setVelocity(velocity);

                    // 生成实体
                    world.spawnEntity(item);

                    // 每喷出几个腐肉播放一次音效
                    if (index % 3 == 0) {
                        world.playSound(
                                null,
                                playerPos.x, playerPos.y, playerPos.z,
                                SoundEvents.ENTITY_SLIME_SQUISH,
                                SoundCategory.PLAYERS,
                                0.6f,
                                0.5f + player.getRandom().nextFloat() * 0.5f
                        );
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        }

        // 修复整数乘法警告
        long totalDelay = (long) count * baseDelay + 500;
        scheduler.schedule(scheduler::shutdown, totalDelay, TimeUnit.MILLISECONDS);

        return 1;
    }

    // 计算玩家臀部位置（精确）
    private static Vec3d getButtPosition(ServerPlayerEntity player) {
        // 获取玩家位置
        Vec3d playerPos = player.getPos();

        // 获取玩家视线方向（单位向量）
        Vec3d lookVec = player.getRotationVec(1.0f);

        // 计算臀部位置：
        // 1. 降低Y轴（玩家高度1.8，臀部大约在脚部上方0.3处）
        // 2. 向后偏移（与视线方向相反）
        double buttHeight = player.getY() - 0.3; // 臀部高度
        double backwardOffset = 0.2; // 向后偏移量

        // 计算水平方向的反方向（忽略Y轴）
        Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z).normalize();
        Vec3d buttOffset = horizontalLook.multiply(-backwardOffset);

        return new Vec3d(
                playerPos.x + buttOffset.x,
                buttHeight,
                playerPos.z + buttOffset.z
        );
    }

    // 计算腐肉喷射速度（与玩家视角相反）
    private static Vec3d calculateShitVelocity(ServerPlayerEntity player) {
        // 获取玩家视线方向（单位向量）
        Vec3d lookVec = player.getRotationVec(1.0f);

        // 基础速度参数
        float basePower = 0.4f;
        float verticalBoost = 0.25f;
        float randomSpread = 0.15f;

        // 计算反向速度（X和Z方向取反，Y方向稍微向上）
        double velX = -lookVec.x * basePower;
        double velY = verticalBoost; // 主要向上喷射
        double velZ = -lookVec.z * basePower;

        // 添加随机扩散
        velX += (player.getRandom().nextDouble() - 0.5) * randomSpread;
        velY += player.getRandom().nextDouble() * 0.1; // 稍微增加Y方向的随机性
        velZ += (player.getRandom().nextDouble() - 0.5) * randomSpread;

        return new Vec3d(velX, velY, velZ);
    }

    private static int forbidPlayer(ServerCommandSource source, ServerPlayerEntity player) {
        forbiddenPlayers.add(player.getUuid());
        source.sendFeedback(
                formatText("已禁止玩家 " + player.getName().getString() + " 使用/shit命令", Formatting.GOLD),
                true
        );
        return 1;
    }

    private static int allowPlayer(ServerCommandSource source, ServerPlayerEntity player) {
        forbiddenPlayers.remove(player.getUuid());
        source.sendFeedback(
                formatText("已允许玩家 " + player.getName().getString() + " 使用/shit命令", Formatting.GREEN),
                true
        );
        return 1;
    }

    // 格式化文本工具方法
    private static MutableText formatText(String message, Formatting color) {
        return Text.literal("[Shit] ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(message).formatted(color));
    }
}