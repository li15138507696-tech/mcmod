package net.guzhenren.tianfu.mixin;

import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.tianfu.TianfuNativeProgression;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.guzhenren.procedures.XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure", remap = false)
public abstract class CultivationProgressMixin {
    @Redirect(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "FIELD", target = "Lnet/guzhenren/network/GuzhenrenModVariables$PlayerVariables;gushi_xiulian_dangqian:D",
                    opcode = Opcodes.PUTFIELD), require = 3, expect = 3, allow = 3)
    private static void tianfu$acceptedPulse(PlayerVariables variables, double nativeAfter,
            LevelAccessor world, double x, double y, double z, Entity entity) {
        variables.gushi_xiulian_dangqian = TianfuNativeProgression.cultivation(entity, variables, nativeAfter);
    }

    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"), require = 1)
    private static void tianfu$alignEveryReturn(LevelAccessor world, double x, double y, double z,
            Entity entity, CallbackInfo ci) {
        TianfuNativeProgression.align(entity);
    }
}
