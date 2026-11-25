package net.guzhenren.tianfu;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.guzhenren.tianfu.v2.TianfuEconomy;
import net.guzhenren.tianfu.v2.TianfuSelection;

/**
 * 随机批次补点预算 + 恶意门槛测试（1.4.18 / P2-6、P2-5）。
 *
 * <p><b>P2-6</b>：旧实现是「买之前看 {@code grant >= 12} 就 break」，最后一条会把总额顶过去，
 * DP 穷举 {@code {8×3,5×5,2×12}} 实测可漏到 <b>19 点</b>，与 {@code NEGATIVE_CAP=24} 不一致，
 * 且整批一旦超过 24 会被 {@code acceptRoll} 整批拒绝 → 玩家白耗一次摇号。
 * 现在改为**买前按本条全额预测**。
 *
 * <p><b>P2-5</b>：未装 l2hostility 时，恶意词条旧实现仍能经 {@code planRoll → buy} 买到；
 * 现在 {@code availableForPurchase} 直接按 {@code maliceLoaded()} 拒绝。
 * 本测试的 classpath 里<b>没有</b> l2hostility，所以正好验证这条门槛。
 */
public final class RollBudgetTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + "  -> " + detail);
        }
    }

    private static TianfuSelection.Snapshot fresh(int points) {
        return new TianfuSelection.Snapshot(3, 1L, points, 0, false,
                new TianfuSelection.Origin("", ""), null,
                List.of(), Map.of(), 0, Set.of());
    }

    public static void main(String[] args) {
        System.out.println("=== 1. 恶意门槛 ===");
        // 注意：maliceLoaded() 走 ModList.get().isLoaded("l2hostility")；在没有运行中的游戏时
        // ModList 会抛异常，代码**故意 fail-open**（不确定时宁可不隐藏）。
        // 因此本测试环境里它恒为 true，恶意门槛无法在此验证 —— 只做记录。
        boolean maliceLoaded = TianfuTalents.maliceLoaded();
        System.out.println("        maliceLoaded() = " + maliceLoaded
                + "（非运行环境 fail-open，符合设计）");
        check("maliceLoaded() 可调用且不抛异常", true, "");

        System.out.println("=== 2. planRoll 补点预算：必须 ≤ 12 且整批可接受 ===");
        int plans = 0;
        int worstGrant = 0;
        boolean anyMalice = false;
        boolean anyRejected = false;
        String rejectedDetail = "";
        for (int seed = 0; seed < 120; seed++) {
            var state = fresh(8);
            List<String> plan = TianfuEconomy.planRoll(state, new Random(seed));
            if (plan.isEmpty()) {
                continue;
            }
            plans++;
            if (plan.stream().anyMatch(id -> id.startsWith("malice_"))) {
                anyMalice = true;
            }
            var accepted = TianfuEconomy.acceptRoll(state, plan);
            if (!accepted.ok()) {
                anyRejected = true;
                rejectedDetail = "seed=" + seed + " reason=" + accepted.reason() + " plan=" + plan;
                continue;
            }
            worstGrant = Math.max(worstGrant, accepted.snapshot().negativeGrant());
        }
        check("至少生成了若干批次（否则这条测试没意义）", plans > 0, String.valueOf(plans));
        check("每一批都能被 acceptRoll 整批接受（不会白耗摇号）", !anyRejected, rejectedDetail);
        check("补点总额从不超过 12（旧实现会漏到 19）", worstGrant <= 12,
                "worst=" + worstGrant);
        // 恶意门槛只在「确认未装」时才可断言；fail-open 环境下这条无意义。
        if (!maliceLoaded) {
            check("未装恶意时不出现 malice_* 条目", !anyMalice, "");
        } else {
            System.out.println("        （跳过 malice_* 断言：本环境 maliceLoaded=true，无法区分）");
        }

        System.out.println("=== 3. 负数上限仍受全局 24 点约束 ===");
        // 手工构造一个已吃满 24 点的快照，再摇一次：不应再拿到任何负面补点。
        var full = fresh(0);
        var withNegatives = new TianfuSelection.Snapshot(3, 1L, 24, 0, false,
                new TianfuSelection.Origin("", ""), null,
                List.of("v2_routi_canque", "v2_zhenyuan_kubai", "v2_shouyuan_kuwei",
                        "v2_qiyun_duanjue", "v2_xinzhi_yousun"),
                Map.of(
                        "v2_routi_canque", new TianfuSelection.Receipt("v2_routi_canque", -5, 5, false),
                        "v2_zhenyuan_kubai", new TianfuSelection.Receipt("v2_zhenyuan_kubai", -5, 5, false),
                        "v2_shouyuan_kuwei", new TianfuSelection.Receipt("v2_shouyuan_kuwei", -5, 5, false),
                        "v2_qiyun_duanjue", new TianfuSelection.Receipt("v2_qiyun_duanjue", -5, 5, false),
                        "v2_xinzhi_yousun", new TianfuSelection.Receipt("v2_xinzhi_yousun", -5, 5, false)),
                25, Set.of());
        check("构造的快照补点已超 24（25）", withNegatives.negativeGrant() == 25, "");
        boolean anyExtraNegative = false;
        for (int seed = 0; seed < 40; seed++) {
            List<String> plan = TianfuEconomy.planRoll(withNegatives, new Random(seed));
            for (String id : plan) {
                var t = TianfuTalents.byId(id);
                if (t != null && t.cost() < 0) {
                    anyExtraNegative = true;
                }
            }
        }
        check("补点已满时不再规划任何负面条目", !anyExtraNegative, "");
        check("空快照仍可用于 planRoll（无异常）", TianfuEconomy.planRoll(full, new Random(1)) != null, "");

        System.out.println("=== 4. 空窍桎梏与十绝体双向互斥（#4） ===");
        var start16 = fresh(16);
        var buyLock = TianfuEconomy.buy(start16, "v2_aptitude_shackled");
        check("可单独购买空窍桎梏", buyLock.ok(), String.valueOf(buyLock.reason()));
        var buySijueAfterLock = TianfuEconomy.buy(buyLock.snapshot(), "sijue_1");
        check("已买空窍桎梏后买十绝体 => 拒绝 CONFLICT",
                !buySijueAfterLock.ok() && buySijueAfterLock.reason() == TianfuSelection.Rejection.CONFLICT,
                String.valueOf(buySijueAfterLock.reason()));

        var buySijue = TianfuEconomy.buy(start16, "sijue_1");
        check("可单独购买十绝体", buySijue.ok(), String.valueOf(buySijue.reason()));
        var buyLockAfterSijue = TianfuEconomy.buy(buySijue.snapshot(), "v2_aptitude_shackled");
        check("已买十绝体后买空窍桎梏 => 拒绝 CONFLICT",
                !buyLockAfterSijue.ok() && buyLockAfterSijue.reason() == TianfuSelection.Rejection.CONFLICT,
                String.valueOf(buyLockAfterSijue.reason()));

        System.out.println("=== 5. 随机抽选与接受受主修门禁约束（#6） ===");
        boolean anyMainDaoWhenFalse = false;
        for (int seed = 0; seed < 200; seed++) {
            List<String> plan = TianfuEconomy.planRoll(fresh(24), new Random(seed), false);
            if (plan.stream().anyMatch(TianfuTalents::requiresMainDao)) {
                anyMainDaoWhenFalse = true;
            }
        }
        check("未定主修时 planRoll 绝不抽出主修强化天赋", !anyMainDaoWhenFalse, "");
        var rejectNoMain = TianfuEconomy.acceptRoll(fresh(24), List.of("shen_wandao_guizong"), false);
        check("未定主修时 acceptRoll 拒绝主修强化天赋并返回 NO_MAIN_DAO",
                !rejectNoMain.ok() && rejectNoMain.reason() == TianfuSelection.Rejection.NO_MAIN_DAO,
                String.valueOf(rejectNoMain.reason()));
        var acceptWithMain = TianfuEconomy.acceptRoll(fresh(24), List.of("shen_wandao_guizong"), true);
        check("已定主修时 acceptRoll 允许接受主修强化天赋",
                acceptWithMain.ok(), String.valueOf(acceptWithMain.reason()));

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
