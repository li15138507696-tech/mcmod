package net.guzhenren.tianfu.mixin;

import net.guzhenren.procedures.JingJieBaoCunProcedure;
import net.guzhenren.tianfu.TianfuReincarnation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 蛊真人本体 11.22+ 转世入口适配（6 参签名，含 x/y/z 坐标）。
 * 由 {@link TianfuMixinPlugin} 在运行期按真实字节码签名自动启用或跳过。
 */
@Mixin(value = JingJieBaoCunProcedure.class, remap = false)
public abstract class JingJieBaoCunV2ProcedureMixin {
    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), require = 1)
    private static void tianfu$capture(Event event, LevelAccessor world, double x, double y, double z,
                                       Entity entity, CallbackInfo ci) {
        TianfuReincarnation.capture(entity);
    }

    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "FIELD", target = "Lnet/guzhenren/network/GuzhenrenModVariables$PlayerVariables;shiqiao:Z",
                    opcode = 181, shift = At.Shift.AFTER), require = 2, allow = 2)
    private static void tianfu$resetCompleted(Event event, LevelAccessor world, double x, double y, double z,
                                              Entity entity, CallbackInfo ci) {
        TianfuReincarnation.complete(entity);
    }

    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"), require = 1)
    private static void tianfu$release(Event event, LevelAccessor world, double x, double y, double z,
                                       Entity entity, CallbackInfo ci) {
        TianfuReincarnation.release(entity);
    }
}
