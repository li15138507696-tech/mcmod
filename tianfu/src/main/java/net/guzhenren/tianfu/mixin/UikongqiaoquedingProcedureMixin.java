package net.guzhenren.tianfu.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.guzhenren.procedures.UikongqiaoquedingProcedure;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuOpening;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

/**
 * 开窍收尾的 TAIL 拦截：{@link TianfuOpening#onCloseOpenQiao}——
 * 标记本世开窍已结算 + 提示 K 键 + 未选难度时弹出附属天赋界面。
 *
 * <p>本适配严格绑定已核验的 9.8 {@code UikongqiaoquedingProcedure}，不声称兼容 10.x。
 *
 * <p>本体 9.8 在该过程开头无条件关闭玩家容器。附属显式调用原生结算时
 * （希望蛊开窍事务或选定资质），只有事务作用域内跳过这次关闭，避免原生结算失败回滚后
 * 界面已经不可恢复；本体独立触发此过程时仍执行原生关闭。
 */
@Mixin(value = UikongqiaoquedingProcedure.class, remap = false)
public class UikongqiaoquedingProcedureMixin {
    @Redirect(method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;closeContainer()V"),
            require = 1, remap = false)
    private static void tianfu$keepContainerDuringTransactionalSettlement(Player player) {
        if (!TianfuOpening.isNativeSettlement(player)) {
            player.closeContainer();
        }
    }

    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), require = 1, remap = false)
    private static void tianfu$onCloseOpenQiao(LevelAccessor world, Entity entity, CallbackInfo ci) {
        try {
            TianfuOpening.onCloseOpenQiao(entity);
        } catch (Throwable t) {
            TianfuMod.LOGGER.error("[天赋] 开窍收尾钩子异常，已静默放行", t);
        }
    }
}
