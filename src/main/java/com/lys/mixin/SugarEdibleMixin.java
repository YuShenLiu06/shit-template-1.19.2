package com.lys.mixin;

import com.lys.network.ServerPacketHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class SugarEdibleMixin {

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void makeSugarEdible(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (stack.getItem() == Items.SUGAR) {
            cir.setReturnValue(UseAction.EAT);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void setSugarUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() == Items.SUGAR) {
            cir.setReturnValue(16);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onSugarUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.getItem() == Items.SUGAR) {
            // 设置玩家为食用状态
            user.setCurrentHand(hand);

            // 服务端发送动画和声音包
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                // 发送完整的食用过程
                ServerPacketHelper.sendFullEatingProcess(serverPlayer, 16);
            }

            cir.setReturnValue(TypedActionResult.consume(stack));
        }
    }

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void onSugarEaten(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() == Items.SUGAR && user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // 增加饱食度
                player.getHungerManager().add(4, 0.2f);

                // 添加状态效果
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS,
                        15 * 20,
                        1
                ));

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.LEVITATION,
                        5 * 20,
                        0
                ));

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA,
                        15 * 20,
                        2
                ));
            }

            // 减少物品数量
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }

            cir.setReturnValue(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }
}