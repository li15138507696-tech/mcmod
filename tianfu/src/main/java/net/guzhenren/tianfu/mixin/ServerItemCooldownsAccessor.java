package net.guzhenren.tianfu.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ServerItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerItemCooldowns.class, remap = false)
public interface ServerItemCooldownsAccessor {
    @Accessor("player")
    ServerPlayer tianfu$player();
}
