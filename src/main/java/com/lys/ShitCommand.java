package com.lys;

import com.lys.network.ServerPacketHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ShitCommand {
    private static final Set<UUID> forbiddenPlayers = new HashSet<>();
    private static final Map<UUID, ScheduledFuture<?>> particleTasks = new ConcurrentHashMap<>();

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

        // 计算总扣除饱食度
        final int totalCost = 3 + player.getRandom().nextInt(3);
        final AtomicInteger remainingCost = new AtomicInteger(totalCost);
        final AtomicBoolean negativeMode = new AtomicBoolean(player.getHungerManager().getFoodLevel() <= 0);

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

        // 发送命令效果（声音）
        ServerPacketHelper.sendShitCommandEffect(player);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        int count = 8 + player.getRandom().nextInt(5);
        int baseDelay = 200;
        int delayVariance = 100;

        for (int i = 0; i < count; i++) {
            final int index = i;
            int delay = baseDelay * i + player.getRandom().nextInt(delayVariance) - delayVariance/2;

            // 负面模式下增加延迟
            if (negativeMode.get()) {
                delay *= 2; // 双倍延迟
            }

            scheduler.schedule(() -> {
                if (!player.isAlive()) return;

                world.getServer().execute(() -> {
                    // 重新计算臀部位置（跟随玩家移动）
                    Vec3d buttPos = getButtPosition(player);

                    // 创建腐肉实体
                    ItemEntity item = new ItemEntity(
                            world,
                            buttPos.x, buttPos.y, buttPos.z,
                            new ItemStack(Items.ROTTEN_FLESH)
                    );

                    // 设置拾取延迟
                    item.setPickupDelay(40);

                    // 计算喷射速度
                    Vec3d velocity = calculateShitVelocity(player);

                    // 负面模式下速度减半
                    if (negativeMode.get()) {
                        velocity = velocity.multiply(0.5);
                    }

                    // 应用速度和旋转
                    item.setYaw(player.getRandom().nextFloat() * 360.0F);
                    item.setPitch(player.getRandom().nextFloat() * 180.0F - 90.0F);
                    item.setVelocity(velocity);

                    // 生成实体
                    world.spawnEntity(item);

                    // 每喷出几个腐肉播放一次音效
                    if (index % 3 == 0) {
                        world.playSound(
                                null,
                                buttPos.x, buttPos.y, buttPos.z,
                                SoundEvents.ENTITY_SLIME_SQUISH,
                                SoundCategory.PLAYERS,
                                0.6f,
                                0.5f + player.getRandom().nextFloat() * 0.5f
                        );
                    }

                    // 逐渐扣除饱食度（每次1点）
                    if (remainingCost.get() > 0 && player.getHungerManager().getFoodLevel() > 0) {
                        // 仅扣除饱食度，不增加
                        player.getHungerManager().setFoodLevel(
                                Math.max(0, player.getHungerManager().getFoodLevel() - 1)
                        );
                        remainingCost.decrementAndGet();

                        // 检查是否进入负面状态
                        if (player.getHungerManager().getFoodLevel() <= 0 && !negativeMode.get()) {
                            negativeMode.set(true);
                            triggerNegativeEffects(player);
                        }
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
        }

        // 安排关闭调度器
        long totalDelay = (long) count * baseDelay + 500;
        if (negativeMode.get()) totalDelay *= 2; // 负面模式延长关闭时间
        scheduler.schedule(scheduler::shutdown, totalDelay, TimeUnit.MILLISECONDS);

        return 1;
    }

    // 触发负面效果
    private static void triggerNegativeEffects(ServerPlayerEntity player) {
        World world = player.getWorld();

        // 给予玩家负面效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 15 * 20, 2)); // 15秒虚弱3级
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 15 * 20, 2)); // 15秒失明3级
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 15 * 20, 0));    // 15秒中毒1级
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5 * 20, 9));   // 5秒缓慢10级

        player.sendMessage(formatText("你感觉身体被掏空...", Formatting.DARK_RED), false);
    }

    // 计算玩家臀部位置（跟随玩家移动）
    private static Vec3d getButtPosition(ServerPlayerEntity player) {
        // 获取玩家位置和方向
        Vec3d playerPos = player.getPos();
        Vec3d lookVec = player.getRotationVec(1.0f);

        // 计算水平方向的反方向（忽略Y轴）
        Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z);
        double length = Math.sqrt(horizontalLook.x * horizontalLook.x + horizontalLook.z * horizontalLook.z);
        if (length > 0) {
            horizontalLook = new Vec3d(
                    horizontalLook.x / length,
                    0,
                    horizontalLook.z / length
            );
        }

        // 臀部位置计算：
        // 1. 玩家位置Y轴减去0.3（臀部高度）
        // 2. 向后偏移0.2格（臀部后方）
        // 3. 考虑玩家宽度（0.6）避免在玩家体内生成
        double offsetX = -horizontalLook.x * 0.2;
        double offsetZ = -horizontalLook.z * 0.2;

        // 确保不会在玩家体内生成
        if (Math.abs(offsetX) < 0.3) offsetX = Math.copySign(0.3, offsetX);
        if (Math.abs(offsetZ) < 0.3) offsetZ = Math.copySign(0.3, offsetZ);

        return new Vec3d(
                playerPos.x + offsetX,
                playerPos.y - 0.3,
                playerPos.z + offsetZ
        );
    }

    // 计算腐肉喷射速度
    private static Vec3d calculateShitVelocity(ServerPlayerEntity player) {
        Vec3d lookVec = player.getRotationVec(1.0f);

        // 基础速度参数
        float basePower = 0.4f;
        float verticalBoost = 0.25f;
        float randomSpread = 0.15f;

        // 计算反向速度（X和Z方向取反）
        double velX = -lookVec.x * basePower;
        double velY = verticalBoost;
        double velZ = -lookVec.z * basePower;

        // 添加随机扩散
        velX += (player.getRandom().nextDouble() - 0.5) * randomSpread;
        velY += player.getRandom().nextDouble() * 0.1;
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