package net.guzhenren.daotu.mixin;

import net.guzhenren.daotu.DaotuReincarnationHook;
import net.guzhenren.procedures.JingJieBaoCunProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 本体转世入口适配（9.8 / 10.4 / 10.20 3 参签名）。
 * 由 {@link DaotuMixinPlugin} 在运行期按真实字节码签名自动启用或跳过。
 */
@Mixin(value = JingJieBaoCunProcedure.class, remap = false)
public abstract class JingJieBaoCunProcedureMixin {

    @Inject(
            method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;"
                    + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), require = 0, remap = false)
    private static void daotu$onReincarnation(Event event, LevelAccessor world, Entity entity,
                                              CallbackInfo ci) {
        DaotuReincarnationHook.onReincarnation(world, entity);
    }
}
