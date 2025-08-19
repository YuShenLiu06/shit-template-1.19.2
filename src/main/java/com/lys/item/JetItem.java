package com.lys.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import com.lys.network.ModMessages;
import com.lys.network.JetActionPacket;

public class JetItem extends Item {
    public JetItem() {
        super(new FabricItemSettings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user instanceof ServerPlayerEntity) {
            // 服务器端处理喷射逻辑
            ServerPlayerEntity player = (ServerPlayerEntity) user;
            ModMessages.sendToServer(new JetActionPacket(player.getUuid(), true));

            // 触发喷射效果
            triggerJetEffect(player);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    private void triggerJetEffect(ServerPlayerEntity player) {
        // 实现喷射物理效果
        player.addVelocity(0, 1.5, 0); // 向上喷射
        player.velocityModified = true;

        // 消耗耐久度或物品
        player.getItemCooldownManager().set(this, 20); // 1秒冷却
    }
}
