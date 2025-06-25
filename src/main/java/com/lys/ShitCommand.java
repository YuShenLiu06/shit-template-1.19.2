package com.lys;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
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

        Vec3d playerPos = player.getPos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d behindPos = playerPos.subtract(lookVec.multiply(0.5)).add(0, -0.5, 0);

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
                    ItemEntity item = new ItemEntity(
                            world,
                            behindPos.x, behindPos.y, behindPos.z,
                            new ItemStack(Items.ROTTEN_FLESH)
                    );

                    // 增加喷射动能
                    Vec3d velocity = new Vec3d(
                            (player.getRandom().nextDouble() - 0.5) * 0.3, // 增加水平速度
                            0.3 + player.getRandom().nextDouble() * 0.2,    // 增加垂直速度
                            (player.getRandom().nextDouble() - 0.5) * 0.3  // 增加水平速度
                    );

                    // 添加一些旋转效果
                    item.setYaw(player.getRandom().nextFloat() * 360.0F);
                    item.setPitch(player.getRandom().nextFloat() * 180.0F - 90.0F);
                    item.setVelocity(velocity);

                    world.spawnEntity(item);

                    // 每喷出几个腐肉播放一次音效
                    if (index % 3 == 0) {
                        world.playSound(
                                null,
                                behindPos.x, behindPos.y, behindPos.z,
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