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

/** Native 9.8 resets end with shiqiao=false at exactly two PUTFIELD sites. */
@Mixin(value = JingJieBaoCunProcedure.class, remap = false)
public abstract class JingJieBaoCunProcedureMixin {
    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), require = 1)
    private static void tianfu$capture(Event event, LevelAccessor world, Entity entity, CallbackInfo ci) {
        TianfuReincarnation.capture(entity);
    }

    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "FIELD", target = "Lnet/guzhenren/network/GuzhenrenModVariables$PlayerVariables;shiqiao:Z",
                    opcode = 181, shift = At.Shift.AFTER), require = 2, allow = 2)
    private static void tianfu$resetCompleted(Event event, LevelAccessor world, Entity entity, CallbackInfo ci) {
        TianfuReincarnation.complete(entity);
    }

    @Inject(method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"), require = 1)
    private static void tianfu$release(Event event, LevelAccessor world, Entity entity, CallbackInfo ci) {
        TianfuReincarnation.release(entity);
    }
}
