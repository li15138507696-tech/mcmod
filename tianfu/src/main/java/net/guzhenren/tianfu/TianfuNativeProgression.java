package net.guzhenren.tianfu;

import java.util.Set;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.tianfu.compat.Guzhenren98Progression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Bonuses are applied at native accepted writes, never from an invented natural baseline. */
public final class TianfuNativeProgression {
    private TianfuNativeProgression() {}

    /** 宿慧的修炼加成上限（1.4.18）：0.05×转数，封顶 +25%。 */
    public static final double SUHUI_CAP = 0.25;

    /**
     * 宿慧提供的修炼加成（纯函数，便于单测）。
     *
     * <p>本体把 {@code zhuanshu} 卡在 5，所以正常上限是 +25%；但其它模组或命令抬高转数后
     * 旧实现会线性无界，故 1.4.18 起硬封顶。
     */
    public static double suhuiBonus(double zhuanshu) {
        return Math.min(SUHUI_CAP, 0.05 * Math.max(1.0, zhuanshu));
    }

    public static double cultivation(Entity entity, PlayerVariables variables, double nativeAfter) {
        if (!(entity instanceof ServerPlayer player)) return nativeAfter;
        Set<String> ids = TianfuApply.effective(player);
        double bonus = TianfuTalents.xiulian(ids);
        if (ids.contains("suhui")) {
            bonus += suhuiBonus(variables.zhuanshu);
        }
        if (ids.contains("kuxiu_buchuo")) bonus += 0.20;
        if (ids.contains("suiyue_chendian")) bonus += 0.10;
        return Guzhenren98Progression.cultivation(variables.gushi_xiulian_dangqian,
                nativeAfter, variables.gushi_xiulian_jindu, bonus);
    }

    public static void align(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        PlayerVariables variables = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
        double result = Guzhenren98Progression.align(variables.gushi_xiulian_dangqian, variables.gushi_xiulian_jindu);
        if (Double.doubleToLongBits(result) != Double.doubleToLongBits(variables.gushi_xiulian_dangqian)) {
            variables.gushi_xiulian_dangqian = result;
            variables.markSyncDirty();
        }
    }

    /**
     * 突破加成的**总上限**（加性百分点）。
     *
     * <p>1.4.18 起「突破」是独立互斥族，且总加成硬封顶 0.50 —— 否则多条高阶天赋的
     * 加性叠加会把本体 `P ≤ 0` 的三转以后突破一路买到必成（旧实现 22 点即可 100%）。
     */
    public static final double BREAKTHROUGH_CAP = 0.50;

    public static double breakthroughRoll(Entity entity, double roll) {
        if (!(entity instanceof ServerPlayer player)) return roll;
        Set<String> ids = TianfuApply.effective(player);
        double reduction = TianfuTalents.breakthrough(ids) + (ids.contains("houji_bofa") ? 0.20 : 0.0);
        // 负向（负面天赋）不设下限，只封顶正向收益。
        reduction = Math.clamp(reduction, -1.0, BREAKTHROUGH_CAP);
        if (reduction == 0.0) return roll;
        PlayerVariables v = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
        double nativeChance = Guzhenren98Progression.nativeBreakthroughChance(
                v.zhuanshu, v.renqi, v.zuida_zhenyuan, v.qiyun, v.mingge == 5.0);
        return Guzhenren98Progression.breakthroughRoll(roll, nativeChance, reduction);
    }

    public static double yuanRecovery(Entity entity, PlayerVariables variables, double nativeAfter) {
        if (!(entity instanceof ServerPlayer player) || !Double.isFinite(variables.zhenyuan)
                || !Double.isFinite(nativeAfter) || nativeAfter <= variables.zhenyuan) return nativeAfter;
        Set<String> ids = TianfuApply.effective(player);
        double bonus = TianfuTalents.regenZy(ids);
        if (TianfuData.isConfirmed(player)) {
            if (variables.tizhi == 31.0) bonus += 0.15;
            if (variables.tizhi == 40.0 && TianfuEffects.isNight(player)) bonus += 0.80;
        }
        return Guzhenren98Progression.recovery(variables.zhenyuan, nativeAfter, variables.zuida_zhenyuan, bonus);
    }

    public static double thoughtRecovery(Entity entity, PlayerVariables variables, double nativeAfter) {
        if (!(entity instanceof ServerPlayer player) || !Double.isFinite(variables.niantou)
                || !Double.isFinite(nativeAfter) || nativeAfter <= variables.niantou) return nativeAfter;
        double bonus = TianfuTalents.regenNt(TianfuApply.effective(player));
        if (TianfuData.isConfirmed(player) && variables.tizhi == 48.0) bonus += 0.30;
        return Guzhenren98Progression.recovery(variables.niantou, nativeAfter, variables.niantou_rongliang, bonus);
    }

    public static int itemCooldownTicks(ServerPlayer player, int nativeTicks) {
        if (nativeTicks <= 0) return nativeTicks;
        return Guzhenren98Progression.itemCooldownTicks(nativeTicks, TianfuTalents.guCd(TianfuApply.effective(player)));
    }
}
