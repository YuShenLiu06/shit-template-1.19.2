package com.lys.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    // 修复点1：1.19.2使用ItemStack参数
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void makeSugarEdible(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (stack.getItem() == Items.SUGAR) {
            System.out.println("[ShitMod] [DEBUG] 将白糖标记为可食用物品");
            cir.setReturnValue(UseAction.EAT);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void setSugarUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() == Items.SUGAR) {
            System.out.println("[ShitMod] [DEBUG] 设置白糖食用时间为16 ticks");
            cir.setReturnValue(16);
        }
    }

    // 修复点2：1.19.2使用不同的finishUsing签名
    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void onSugarEaten(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("[ShitMod] [DEBUG] 物品食用完成事件触发 - 物品: " + stack.getItem().getTranslationKey());

        if (stack.getItem() == Items.SUGAR && user instanceof PlayerEntity player) {
            System.out.println("[ShitMod] [DEBUG] 检测到白糖食用事件 - 玩家: " + player.getName().getString());
            System.out.println("[ShitMod] [DEBUG] 客户端模式: " + world.isClient);
            System.out.println("[ShitMod] [DEBUG] 玩家当前饱食度: " + player.getHungerManager().getFoodLevel());

            if (!world.isClient) {
                System.out.println("[ShitMod] [DEBUG] 服务器端处理白糖食用效果");

                // 增加饱食度
                int currentFoodLevel = player.getHungerManager().getFoodLevel();
                player.getHungerManager().add(4, 0.2f);
                System.out.println("[ShitMod] [DEBUG] 增加饱食度: " + currentFoodLevel + " -> " + player.getHungerManager().getFoodLevel());

                // 添加状态效果
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS,
                        15 * 20,
                        1
                ));
                System.out.println("[ShitMod] [DEBUG] 添加虚弱效果（15秒，等级II）");

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.LEVITATION,
                        5 * 20,
                        0
                ));
                System.out.println("[ShitMod] [DEBUG] 添加漂浮效果（5秒，等级I）");

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA,
                        15 * 20,
                        2
                ));
                System.out.println("[ShitMod] [DEBUG] 添加反胃效果（15秒，等级III）");
            }

            // 减少物品数量（创造模式除外）
            if (!player.getAbilities().creativeMode) {
                int beforeCount = stack.getCount();
                stack.decrement(1);
                System.out.println("[ShitMod] [DEBUG] 减少白糖数量: " + beforeCount + " -> " + stack.getCount());
            } else {
                System.out.println("[ShitMod] [DEBUG] 创造模式，不减少物品数量");
            }

            // 返回剩余的物品堆栈
            ItemStack resultStack = stack.isEmpty() ? ItemStack.EMPTY : stack;
            cir.setReturnValue(resultStack);
            System.out.println("[ShitMod] [DEBUG] 返回物品堆栈: " + resultStack);
        }
    }

    // 修复点3：添加缺失的交互处理器
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onSugarUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.getItem() == Items.SUGAR) {
            System.out.println("[ShitMod] [DEBUG] 玩家开始食用白糖");
            user.setCurrentHand(hand);
            cir.setReturnValue(TypedActionResult.consume(stack));
        }
    }
}