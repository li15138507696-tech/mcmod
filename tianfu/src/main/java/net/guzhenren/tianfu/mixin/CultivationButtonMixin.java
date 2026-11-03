package net.guzhenren.tianfu.mixin;

import net.guzhenren.tianfu.TianfuNativeProgression;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.guzhenren.procedures.UIxiuliananjianProcedure", remap = false)
public abstract class CultivationButtonMixin {
    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), require = 1)
    private static void tianfu$alignBeforeNativeGate(LevelAccessor world, double x, double y, double z,
            Entity entity, CallbackInfo ci) {
        TianfuNativeProgression.align(entity);
    }

    @Redirect(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"), require = 4, expect = 4, allow = 4)
    private static double tianfu$nativeBreakthroughRisk(LevelAccessor world, double x, double y, double z, Entity entity) {
        return TianfuNativeProgression.breakthroughRoll(entity, Math.random());
    }
}
