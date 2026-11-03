package net.guzhenren.tianfu.mixin;

import net.guzhenren.tianfu.TianfuNativeProgression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ServerItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ItemCooldowns.class, remap = false)
public abstract class NativeItemCooldownMixin {
    @ModifyVariable(method = "addCooldown(Lnet/minecraft/world/item/Item;I)V", at = @At("HEAD"), argsOnly = true,
            ordinal = 0, require = 1)
    private int tianfu$nativeGuCooldown(int nativeTicks, Item item, int originalTicks) {
        if (!((Object)this instanceof ServerItemCooldowns)
                || !"guzhenren".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) return nativeTicks;
        return TianfuNativeProgression.itemCooldownTicks(((ServerItemCooldownsAccessor)(Object)this).tianfu$player(), nativeTicks);
    }
}
