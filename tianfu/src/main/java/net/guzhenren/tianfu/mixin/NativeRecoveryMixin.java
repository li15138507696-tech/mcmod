package net.guzhenren.tianfu.mixin;

import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.tianfu.TianfuNativeProgression;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.guzhenren.procedures.KE1Procedure", remap = false)
public abstract class NativeRecoveryMixin {
    @Redirect(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "FIELD", target = "Lnet/guzhenren/network/GuzhenrenModVariables$PlayerVariables;zhenyuan:D",
                    opcode = Opcodes.PUTFIELD), require = 7, expect = 7, allow = 7)
    private static void tianfu$nativeYuanGain(PlayerVariables variables, double nativeAfter,
            LevelAccessor world, double x, double y, double z, Entity entity) {
        variables.zhenyuan = TianfuNativeProgression.yuanRecovery(entity, variables, nativeAfter);
    }

    @Redirect(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "FIELD", target = "Lnet/guzhenren/network/GuzhenrenModVariables$PlayerVariables;niantou:D",
                    opcode = Opcodes.PUTFIELD), require = 4, expect = 4, allow = 4)
    private static void tianfu$nativeThoughtGain(PlayerVariables variables, double nativeAfter,
            LevelAccessor world, double x, double y, double z, Entity entity) {
        variables.niantou = TianfuNativeProgression.thoughtRecovery(entity, variables, nativeAfter);
    }
}
