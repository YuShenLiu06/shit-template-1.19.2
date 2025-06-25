package com.lys;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
            source.sendError(Text.literal("只有玩家可以执行此命令"));
            return 0;
        }

        if (forbiddenPlayers.contains(player.getUuid())) {
            source.sendError(Text.literal("你没有使用此命令的权限"));
            return 0;
        }

        if (player.getHungerManager().getFoodLevel() < 5) {
            player.sendMessage(Text.literal("你不能再拉啦，都要饿死了"), false);
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

        player.sendMessage(Text.literal("噗~"), false);

        Vec3d playerPos = player.getPos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d behindPos = playerPos.subtract(lookVec.multiply(0.5)).add(0, -0.5, 0);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        int count = 8 + player.getRandom().nextInt(5);

        for (int i = 0; i < count; i++) {
            final int delay = i * 100;
            scheduler.schedule(() -> {
                if (!player.isAlive()) return;

                world.getServer().execute(() -> {
                    ItemEntity item = new ItemEntity(
                            world,
                            behindPos.x, behindPos.y, behindPos.z,
                            new ItemStack(Items.ROTTEN_FLESH)
                    );

                    Vec3d velocity = new Vec3d(
                            (player.getRandom().nextDouble() - 0.5) * 0.1,
                            0.2 + player.getRandom().nextDouble() * 0.1,
                            (player.getRandom().nextDouble() - 0.5) * 0.1
                    );
                    item.setVelocity(velocity);

                    world.spawnEntity(item);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }

        scheduler.schedule(scheduler::shutdown, count * 100 + 100, TimeUnit.MILLISECONDS);

        return 1;
    }

    private static int forbidPlayer(ServerCommandSource source, ServerPlayerEntity player) {
        forbiddenPlayers.add(player.getUuid());
        source.sendFeedback(Text.literal("已禁止玩家 " + player.getName().getString() + " 使用/shit命令"), true);
        return 1;
    }

    private static int allowPlayer(ServerCommandSource source, ServerPlayerEntity player) {
        forbiddenPlayers.remove(player.getUuid());
        source.sendFeedback(Text.literal("已允许玩家 " + player.getName().getString() + " 使用/shit命令"), true);
        return 1;
    }
}