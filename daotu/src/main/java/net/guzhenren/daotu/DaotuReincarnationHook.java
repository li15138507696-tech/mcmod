package net.guzhenren.daotu;

import net.guzhenren.init.GuzhenrenModGameRules;
import net.guzhenren.init.GuzhenrenModMobEffects;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/**
 * 转世入口共享判定：供 3 参（9.8/10.x）与 6 参（11.22+）两个 Mixin 壳共用。
 * 放在非 mixin 包下，避免 Sponge Mixin 禁止直接加载 mixin 包内辅助类。
 */
public final class DaotuReincarnationHook {
    private DaotuReincarnationHook() {
    }

    @SuppressWarnings("deprecation")
    public static void onReincarnation(LevelAccessor world, Entity entity) {
        try {
            if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
                return;
            }
            if (player.hasEffect(GuzhenrenModMobEffects.FUHUO)) {
                return; // 复活效果：本体不转世
            }
            boolean xiuwei = world.getLevelData().getGameRules()
                    .getBoolean(GuzhenrenModGameRules.XIUWEI);
            PlayerVariables v = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
            if (xiuwei && v.shouyuan > 0.0) {
                return; // 普通死亡
            }
            DaotuOpening.onReincarnation(player);
        } catch (Throwable t) {
            DaotuMod.LOGGER.warn("daotu 转世钩子失败（已忽略）", t);
        }
    }
}
