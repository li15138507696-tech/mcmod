// src/main/java/net/guzhenren/tianfu/TianfuApply.java
package net.guzhenren.tianfu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 服务端权威的增量购买：难度门禁 + 点数足额校验 → 原子扣点落库 → 立即生效。
 *
 * <p>点数规则：单一自由点钱包（开局按难度发放），可购全部条目，无数量与槽位限制；
 * 仅要求先选开局难度。
 */
public final class TianfuApply {
    private TianfuApply() {
    }

    /**
     * 过滤掉不认识 / 重复的 id，保持客户端顺序。
     *
     * <p>注意：这里用 {@link TianfuTalents#byIdIncludingUnavailable} 而不是
     * {@link TianfuPool#byId} —— 后者只含「当前环境可购买」的条目。若玩家买了
     * 附属联动天赋后又把附属卸载，其 id 会从池子里消失，但 purchased 里仍在、
     * 点数也已经扣了。此时若在这里把它滤掉，就会变成「点数白扣 + 无法撤销」。
     * 所以已购判定必须认得这些条目，只是它们的效果解析不到而已（不崩、不生效）。
     */
    public static Set<String> sanitize(List<String> raw) {
        Set<String> set = new LinkedHashSet<>();
        for (String id : raw) {
            if (TianfuPool.byId(id) != null || TianfuTalents.byIdIncludingUnavailable(id) != null) {
                set.add(id);
            }
        }
        return set;
    }

    /**
     * 「当前真正生效」的天赋集合：未最终确认时返回空集。
     *
     * <p>购买与生效解耦后，凡是要按已购条目算数值的地方（属性修正 / 战斗乘区 / 周期效果）
     * 都必须走这里，否则确认前就能白嫖加成。已确认时等价于 {@link #sanitize}。
     */
    public static Set<String> effective(Player player) {
        if (!TianfuData.isConfirmed(player)) {
            return Set.of();
        }
        return sanitize(TianfuData.purchased(player));
    }

    /** 单条目购买（服务端权威）：只落库，效果等最终确认时统一结算。 */
    /** V2 契约：服务端门禁后调用TianfuEconomy.buy，原子拦截补点超24并保存成交快照，失败返回false。未填充时拒绝执行，不写入玩家状态。 */
    public static boolean buy(ServerPlayer player, String id) {
        if (player == null || id == null || id.isEmpty()) {
            return false;
        }
        if (!TianfuData.isApplied(player) || TianfuData.isConfirmed(player)) {
            return false;
        }
        net.guzhenren.tianfu.v2.TianfuSelection.Snapshot snapshot =
                net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        net.guzhenren.tianfu.v2.TianfuSelection.Result result =
                net.guzhenren.tianfu.v2.TianfuEconomy.buy(snapshot, id);
        if (!result.ok()) {
            return false;
        }
        return net.guzhenren.tianfu.v2.TianfuV2Store.commit(player,
                new net.guzhenren.tianfu.v2.TianfuSelection.Change(snapshot.revision(), result.snapshot()));    }

    /**
     * 撤销已购天赋（服务端权威）。
     *
     * <p>撤销范围：凡体（BODY）+ 词条（ENTRY）。**十绝体/机缘不允许撤销**
     * （原生流程含随机值/Scoreboard 改写，回滚会污染存档）。
     *
     * <p>效果回滚：
     * <ul>
     *   <li>从 {@code purchased} 原子移除 + 退回自由点（正成本条目退回 cost 点；负成本条目扣回 -cost 点）</li>
     *   <li>BODY：tizhi 清零 + Daohen 成对减 1000/100 + Effects.revokeOnce 反向字段回滚 + 回收飞行</li>
     *   <li>ENTRY：Effects.revokeOnce 反向字段回滚</li>
     *   <li>周期效果/战斗乘区/常驻药水：从 purchased 移除后 onTick 自动不再触发，幂等重建自然消失</li>
     * </ul>
     */
    /** V2 契约：按Receipt原价撤销且保持现有体质机缘撤销限制，失败返回false且无副作用。未填充时拒绝执行，不写入玩家状态。 */
    public static boolean revoke(ServerPlayer player, String id) {
        if (player == null || id == null || id.isEmpty()) {
            return false;
        }
        // 已确认/已结算后不可撤销：效果已落到本体字段，逆向回滚会与原生回调打架。
        if (TianfuData.isConfirmed(player) || TianfuData.isSettled(player)) {
            return false;
        }
        // 十绝体/机缘不允许撤销（见上方 javadoc）：原生流程含随机值与 scoreboard 改写。
        net.guzhenren.tianfu.TianfuPool.Spec spec = net.guzhenren.tianfu.TianfuPool.byId(id);
        if (spec != null && (spec.kind() == net.guzhenren.tianfu.TianfuPool.Kind.SIJUE
                || spec.kind() == net.guzhenren.tianfu.TianfuPool.Kind.JIYUAN)) {
            return false;
        }
        net.guzhenren.tianfu.v2.TianfuSelection.Snapshot snapshot =
                net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        net.guzhenren.tianfu.v2.TianfuSelection.Result result =
                net.guzhenren.tianfu.v2.TianfuEconomy.revoke(snapshot, id);
        if (!result.ok()) {
            return false;
        }
        if (!net.guzhenren.tianfu.v2.TianfuV2Store.commit(player,
                new net.guzhenren.tianfu.v2.TianfuSelection.Change(snapshot.revision(), result.snapshot()))) {
            return false;
        }
        // 未确认前效果不结算，因此无需逆向写本体字段；确认后本方法已被上面的门禁拦住。
        return true;
    }

    /** V2 契约：经VersionAdapter预检后执行可恢复幂等结算，legacy不重复已有上限且老档无出身不补效果。未填充时拒绝执行，不写入玩家状态。 */
    public static void applyAllConfirmed(ServerPlayer player, boolean legacy) {
        if (player == null) {
            throw new NullPointerException("player");
        }
        net.guzhenren.tianfu.v2.TianfuSelection.Snapshot snapshot =
                net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        if (TianfuData.isSettled(player)) {
            // 幂等：已结算不重跑，避免道痕/乘法/寿元被二次扣减。
            return;
        }
        if (!snapshot.confirmed()) {
            if (!legacy) {
                return;
            }
            TianfuData.markConfirmed(player);
            snapshot = net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        }
        net.guzhenren.tianfu.v2.VersionAdapter adapter =
                new net.guzhenren.tianfu.v2.NeoForge1211Adapter();
        net.guzhenren.tianfu.v2.TianfuSelection.Rejection preflight =
                adapter.preflight(player, snapshot);
        if (preflight != net.guzhenren.tianfu.v2.TianfuSelection.Rejection.NONE) {
            // 预检不过不写 settled，保留可重试状态。
            throw new IllegalStateException("V2 预检未通过: " + preflight);
        }
        adapter.settle(player, snapshot);
        TianfuData.markSettled(player);
    }
}
